#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$ROOT/WatchMenuShell.2"
WATCH_DIR="$ROOT/watch-music-1.2_19/watch-music"
WATCH_LINK="$ANDROID_DIR/app/src/main/java/com/example/WatchLink.kt"
GRADLE_PROPERTIES="$ANDROID_DIR/gradle.properties"
OUT="$ROOT/output"
BUILD_ROOT="$ROOT/.build"
APPS_REV="674da67eaab0739f5504ea46f590645c526c5f2e"
SDK_REV="cccaf794a372f98ec223ffa6f25db0960cb1c1c6"
APPS_URL="https://github.com/fmctools/apps.git"
SDK_URL="https://github.com/fmctools/cmf-watch-sdk.git"

fail() { printf '\nERROR: %s\n' "$*" >&2; exit 1; }
step() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }

[[ -d "$ANDROID_DIR/app" ]] || fail "Android project is missing: $ANDROID_DIR"
[[ -f "$WATCH_DIR/Cargo.toml" ]] || fail "Watch Music source is missing: $WATCH_DIR"
[[ -f "$WATCH_LINK" ]] || fail "WatchLink.kt is missing: $WATCH_LINK"

# Stop immediately if an older ZIP/source tree is being built. These exact declarations
# were absent from the first archive and caused the unresolved-reference errors in the log.
grep -Fq 'import java.util.concurrent.ConcurrentLinkedQueue' "$WATCH_LINK" || fail "Stale Android source: ConcurrentLinkedQueue import is missing. Extract the v3 ZIP into its own new folder."
grep -Fq 'private const val UNTAGGED_APP_ACK: Long = Long.MIN_VALUE' "$WATCH_LINK" || fail "Stale Android source: UNTAGGED_APP_ACK declaration is missing. Extract the v3 ZIP into its own new folder."
grep -Fq 'private val pendingAppAckGenerations = ConcurrentLinkedQueue<Long>()' "$WATCH_LINK" || fail "Stale Android source: pendingAppAckGenerations declaration is missing. Extract the v3 ZIP into its own new folder."
grep -Fq 'org.gradle.jvmargs=-Xmx6g' "$GRADLE_PROPERTIES" || fail "Gradle heap config is not the v3 6 GB setting. Extract the v3 ZIP into its own new folder."
grep -Fq -- '-XX:+UseSerialGC' "$GRADLE_PROPERTIES" || fail "SerialGC workaround is missing from Gradle JVM settings."
grep -Fq -- '-XX:ActiveProcessorCount=4' "$GRADLE_PROPERTIES" || fail "JVM processor cap is missing from Gradle JVM settings."
grep -Fq -- '-XX:CICompilerCount=2' "$GRADLE_PROPERTIES" || fail "JIT compiler cap is missing from Gradle JVM settings."
grep -Fq 'org.gradle.workers.max=1' "$GRADLE_PROPERTIES" || fail "Gradle worker limit is not the v3 single-worker setting. Extract the v3 ZIP into its own new folder."
printf 'Android handshake declarations: present\n'
printf 'Gradle JVM: 6 GB heap / SerialGC / 4 visible CPUs / 2 JIT compilers / 1 Gradle worker\n'
printf 'Android source SHA256: '
sha256sum "$WATCH_LINK" | awk '{print $1}'
[[ -x "$ANDROID_DIR/gradlew" ]] || chmod +x "$ANDROID_DIR/gradlew"

step "Checking Linux build dependencies"
missing=()
for tool in git make uv rustc cargo rustup llvm-objcopy resvg java sha256sum; do
  command -v "$tool" >/dev/null 2>&1 || missing+=("$tool")
done
if ((${#missing[@]})); then
  printf 'Missing required tools: %s\n' "${missing[*]}" >&2
  cat >&2 <<'HELP'
Install the missing tools before building:
  - Git, GNU make, Java 17+ (Java 21 recommended)
  - Rust via rustup, plus: rustup target add thumbv7em-none-eabi
  - llvm-objcopy (the Rust llvm-tools component is recommended)
  - uv (Python project runner) and resvg (SVG icon renderer)
On Arch/CachyOS, install the system packages with pacman where available,
install Rust with rustup, then install resvg with: cargo install resvg
HELP
  exit 1
fi

JAVA_VERSION="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p')"
[[ -n "$JAVA_VERSION" && "$JAVA_VERSION" -ge 17 ]] || fail "Java 17 or newer is required (detected: ${JAVA_VERSION:-unknown})."
rustup target list --installed | grep -Fxq 'thumbv7em-none-eabi' || fail "Rust target missing. Run: rustup target add thumbv7em-none-eabi"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" ]]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/Android/sdk" "/opt/android-sdk" "/usr/lib/android-sdk"; do
    if [[ -d "$candidate/platforms" ]]; then SDK_ROOT="$candidate"; break; fi
  done
fi
[[ -n "$SDK_ROOT" && -d "$SDK_ROOT/platforms" ]] || fail "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT to your SDK directory."
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
printf 'Java: %s\nAndroid SDK: %s\n' "$JAVA_VERSION" "$ANDROID_SDK_ROOT"
printf 'Rust target: thumbv7em-none-eabi\n'
# Validate the collector and CPU flags against the selected runtime before downloading/building anything.
java -XX:+UseSerialGC -XX:ActiveProcessorCount=4 -XX:CICompilerCount=2 -version >/dev/null 2>&1 || fail "The selected Java runtime does not accept the v3 SerialGC/CPU flags."

mkdir -p "$OUT" "$BUILD_ROOT"

# A content-specific build workspace makes repeated runs safe: it never reuses or overwrites a different
# uploaded source tree, and it never changes the original source folders in this ZIP.
SOURCE_HASH="$(find "$WATCH_DIR" -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | cut -c1-12)"
WORKSPACE="$BUILD_ROOT/apps-workspace-${APPS_REV:0:12}-$SOURCE_HASH"

step "Preparing the pinned CMF reference build workspace"
if [[ ! -d "$WORKSPACE/.git" ]]; then
  [[ ! -e "$WORKSPACE" ]] || fail "Build workspace path exists but is not a Git checkout: $WORKSPACE"
  git clone --filter=blob:none "$APPS_URL" "$WORKSPACE"
  git -C "$WORKSPACE" checkout --detach "$APPS_REV"
else
  CURRENT="$(git -C "$WORKSPACE" rev-parse HEAD)"
  [[ "$CURRENT" == "$APPS_REV" ]] || fail "Existing build workspace is at $CURRENT, expected $APPS_REV. It was left untouched: $WORKSPACE"
fi

TARGET_APP="$WORKSPACE/apps/watch-music"
if [[ ! -e "$TARGET_APP" ]]; then
  cp -a "$WATCH_DIR" "$TARGET_APP"
else
  diff -qr "$WATCH_DIR" "$TARGET_APP" >/dev/null || fail "The generated Watch Music copy differs from the ZIP source. To preserve your edits, this script will not overwrite it: $TARGET_APP"
fi

PINNED_SDK="$WORKSPACE/.sdk/wp2-apps"
if [[ ! -d "$PINNED_SDK/.git" ]]; then
  [[ ! -e "$PINNED_SDK" ]] || fail "SDK path exists but is not a Git checkout: $PINNED_SDK"
  mkdir -p "$(dirname "$PINNED_SDK")"
  git clone --filter=blob:none "$SDK_URL" "$PINNED_SDK"
  git -C "$PINNED_SDK" checkout --detach "$SDK_REV"
else
  CURRENT="$(git -C "$PINNED_SDK" rev-parse HEAD)"
  [[ "$CURRENT" == "$SDK_REV" ]] || fail "Existing SDK checkout is at $CURRENT, expected $SDK_REV. It was left untouched: $PINNED_SDK"
fi

step "Testing and building Watch Music (Rust / .wp2app)"
make -C "$WORKSPACE" test APP=watch-music
make -C "$WORKSPACE" build APP=watch-music
WATCH_PACKAGE="$WORKSPACE/target/watch-music.wp2app"
[[ -s "$WATCH_PACKAGE" ]] || fail "The Watch Music build did not produce a non-empty .wp2app package."
WATCH_VERSION="$(sed -n 's/^version = "\([^"]*\)"/\1/p' "$WATCH_DIR/store.toml" | head -n1)"
[[ -n "$WATCH_VERSION" ]] || fail "Could not read Watch Music version from store.toml."
cp -f "$WATCH_PACKAGE" "$OUT/watch-music-$WATCH_VERSION.wp2app"
[[ -s "$OUT/watch-music-$WATCH_VERSION.wp2app" ]] || fail "Copied Watch Music package is empty."

step "Testing and building Watch Menu 2 (Android APK)"
(
  cd "$ANDROID_DIR"
  ./gradlew --no-daemon --max-workers=1 --stacktrace :app:testDebugUnitTest :app:assembleDebug
)
ANDROID_VERSION="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' "$ANDROID_DIR/app/build.gradle.kts" | head -n1)"
[[ -n "$ANDROID_VERSION" ]] || fail "Could not read Android versionName from app/build.gradle.kts."
APK="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$APK" ]] || fail "Gradle completed without a non-empty debug APK at the expected path: $APK"
cp -f "$APK" "$OUT/WatchMenu-$ANDROID_VERSION-debug.apk"
[[ -s "$OUT/WatchMenu-$ANDROID_VERSION-debug.apk" ]] || fail "Copied Android APK is empty."

step "Build completed"
printf 'Android APK: %s\n' "$OUT/WatchMenu-$ANDROID_VERSION-debug.apk"
printf 'Watch app:   %s\n' "$OUT/watch-music-$WATCH_VERSION.wp2app"
printf '\nNo device was installed, paired, flashed, or modified by this script.\n'

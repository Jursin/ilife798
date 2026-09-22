#!/usr/bin/env bash
# 无签名 IPA 打包：xcodebuild 归档（不使用任何签名身份）→ Payload 组装 → zip 为 .ipa。
# 用法：./iosApp/scripts/build-unsigned-ipa.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

APP_NAME="ilife798"

ARCHIVE_PATH="$ROOT_DIR/iosApp/build/iosApp.xcarchive"
DIST_DIR="$ROOT_DIR/iosApp/build/dist"

xcodebuild \
  -project "$ROOT_DIR/iosApp/iosApp.xcodeproj" \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE_PATH" \
  archive \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=""

APP_PATH="$ARCHIVE_PATH/Products/Applications/iosApp.app"
test -d "$APP_PATH" || { echo "错误：归档中未找到 $APP_PATH" >&2; exit 1; }

STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT
mkdir -p "$STAGING/Payload"
cp -R "$APP_PATH" "$STAGING/Payload/"

mkdir -p "$DIST_DIR"
IPA_PATH="$DIST_DIR/$APP_NAME-ios-unsigned.ipa"
rm -f "$IPA_PATH"
(cd "$STAGING" && ditto -c -k --keepParent Payload "$IPA_PATH")

# 产物校验：Payload 结构必须包含 Info.plist 与主二进制。
unzip -l "$IPA_PATH" | grep -q "Payload/iosApp.app/Info.plist"
unzip -l "$IPA_PATH" | grep -q "Payload/iosApp.app/iosApp"
unzip -t "$IPA_PATH" > /dev/null

echo "已生成: $IPA_PATH"

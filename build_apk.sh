#!/bin/bash
# 本地一键构建 debug 安装包（需先安装 Android SDK 且网络可达）
set -e

SDK="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
if [ -z "$SDK" ]; then
  echo "错误：请先设置环境变量 ANDROID_SDK_ROOT 或 ANDROID_HOME 指向 Android SDK 目录"
  exit 1
fi

echo "== 安装 SDK 组件：platform-tools / platforms;android-34 / build-tools;34.0.0 =="
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "sdk.dir=$SDK" > local.properties

echo "== 构建 debug APK =="
./gradlew assembleDebug --no-daemon

echo
echo "完成！安装包位于："
echo "  app/build/outputs/apk/debug/app-debug.apk"

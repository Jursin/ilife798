# ILife798

慧生活798 第三方客户端，使用 Miuix UI 框架。

## 构建与运行

- Android Debug: `./gradlew :androidApp:assembleDebug`
- Android Release: `./gradlew :androidApp:assembleRelease`
- iOS app: 用 Xcode 打开 [/iosApp](./iosApp) 目录运行

## 运行测试

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

## 敏感数据配置

- Android 接口配置：复制 `secrets.properties.example` 为 `secrets.properties`
- Android 签名配置：复制 `local.properties.example` 为 `local.properties`
- iOS 接口配置：复制 `iosApp/Configuration/Secrets.xcconfig.example` 为 `iosApp/Configuration/Secrets.xcconfig`

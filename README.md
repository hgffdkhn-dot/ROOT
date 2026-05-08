# Root权限守护者 (RootGuard)

一个简洁、安全的 Android Root 权限管理应用，采用现代 Clean Architecture + MVVM 架构。

## 功能特性

- 🔍 **Root 状态检测** - 实时检测设备 Root 状态
- 📱 **应用管理** - 查看和管理已安装应用的 Root 权限
- 📋 **安全日志** - 记录所有 Root 权限访问历史
- ⚙️ **设置中心** - 自定义应用行为和通知偏好

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **架构**: Clean Architecture + MVVM
- **依赖注入**: Hilt
- **异步**: Kotlin Coroutines + Flow
- **数据存储**: DataStore Preferences

## 系统要求

- Android 7.0 (API 24) 及以上
- 支持 Android 10, 11, 12, 13, 14

## 快速开始

### 方法一：从源码构建

1. 克隆仓库
```bash
git clone https://github.com/YOUR_USERNAME/RootGuard.git
cd RootGuard
```

2. 使用 Android Studio
- 打开 Android Studio
- 选择 "Open an Existing Project"
- 选择项目根目录
- 等待 Gradle 同步完成
- 点击 Build → Build Bundle(s) / APK(s) → Build APK(s)

3. 或使用命令行
```bash
./gradlew assembleDebug
```

APK 输出位置: `app/build/outputs/apk/debug/app-debug.apk`

### 方法二：下载 CI 自动构建的 APK

每次代码推送到 `main` 分支，GitHub Actions 会自动构建 APK：

1. 进入项目的 **Actions** 页面
2. 选择最新的 workflow 运行
3. 在 Artifacts 中下载 `app-debug.apk`

## GitHub Actions CI/CD

项目已配置自动构建工作流：

### 构建流程

推送代码到 `main` 分支时自动触发：
1. 检出代码
2. 配置 JDK 17 环境
3. 授予 gradlew 执行权限
4. 执行 `./gradlew assembleDebug`
5. 上传 APK 作为构建产物

### 查看构建结果

1. 打开 GitHub 仓库
2. 点击 **Actions** 标签
3. 查看构建历史和日志
4. 下载构建产物 (APK)

### 下载最新 APK

直接下载链接（替换 `YOUR_USERNAME` 和 `REPO_NAME`）:
```
https://github.com/YOUR_USERNAME/RootGuard/actions/workflows/android.yml
```

在 Actions 页面点击最新运行，下载 `app-debug.apk` artifact。

## 项目结构

```
RootGuard/
├── app/
│   └── src/main/
│       ├── java/com/rootguard/
│       │   ├── data/           # 数据层
│       │   │   └── repository/ # 仓库实现
│       │   ├── di/             # 依赖注入模块
│       │   ├── domain/         # 领域层
│       │   │   ├── model/      # 数据模型
│       │   │   ├── repository/ # 仓库接口
│       │   │   └── usecase/    # 用例
│       │   ├── presentation/   # 展示层
│       │   │   ├── components/ # 可复用组件
│       │   │   ├── navigation/ # 导航配置
│       │   │   ├── screens/    # 页面
│       │   │   └── viewmodel/  # ViewModel
│       │   └── ui/theme/      # UI 主题
│       ├── res/               # 资源文件
│       └── AndroidManifest.xml
├── .github/
│   └── workflows/
│       └── android.yml        # CI/CD 工作流
├── build.gradle.kts           # 根构建配置
├── settings.gradle.kts         # 项目设置
├── gradle.properties          # Gradle 属性
└── local.properties           # 本地配置
```

## 开发指南

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 添加依赖

在 `app/build.gradle.kts` 的 `dependencies` 块中添加：

```kotlin
implementation("com.example:library:version")
```

### 运行应用

1. 连接 Android 设备或启动模拟器
2. 在 Android Studio 中点击 Run 按钮
3. 或使用命令行:
   ```bash
   ./gradlew installDebug
   ```

### 构建发布版本

```bash
./gradlew assembleRelease
```

## 许可证

本项目仅供学习和研究使用。

## 贡献

欢迎提交 Issue 和 Pull Request！

# 悬浮球

一个很小的 Android 悬浮球。打开开关后，屏幕上会出现一个圆球：可以拖动，松手贴到左右边缘；点一下展开快捷面板。

- 包名：`com.zts5202.floatingball`
- 显示名：悬浮球
- versionName：`0.1.0`
- versionCode：`1`
- minSdk：26

## 为什么没有直接依赖 JetOverlay

评估过 [JetOverlay 1.1.0](https://github.com/YazanAesmael/JetOverlay)（JitPack：`com.github.YazanAesmael:JetOverlay:1.1.0`，仓库仍在维护，不是弃用）。它的 README 能管权限和悬浮窗，但 1.1.0 的实现满足不了这个 MVP：

- 拖拽写死在库的 `OverlayService` 里，松手只判断垃圾桶，没有贴边回调。
- 窗口大小在 `addView` 时定下，调用方改不了 `LayoutParams`，面板展开后窗口不会跟着变大，也收不到窗外点击。
- `onStartCommand` 固定返回 `START_STICKY`。进程被系统杀掉后会拉起一个没有悬浮球的前台服务，通知会一直挂着。
- README 里的 `checkPermission()` / `show(id, config)` 和 1.1.0 源码不一致。源码是 `show(context, config, payload)`，没有权限辅助方法。

所以这里按同样的系统能力自己实现：前台服务 + `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` + `ComposeView`（补上 Lifecycle、ViewModelStore、SavedState）。没有无障碍服务，没有录屏，也没有铺满全屏的点击层。

## 授予悬浮窗权限

悬浮窗不是普通的运行时权限，要到系统设置里打开。

1. 安装并打开「悬浮球」。
2. 打开「启用悬浮球」。如果还没授权，会进入系统的「显示在其他应用上层」。
3. 找到「悬浮球」，允许显示在其他应用上层。
4. 返回应用。页面会在 `ON_RESUME` 时重新检查；已经允许就会启动悬浮球。
5. Android 13 及以上，第一次开启时还会询问通知权限。拒绝通知不影响圆球本身，只是状态栏里可能看不到「悬浮球运行中」。

也可以不拨开关，直接点「去授权」，路径相同。

## 怎么用

1. 授权并打开开关。屏幕边缘出现圆球，通知标题是「悬浮球运行中」。
2. 按住圆球拖动，松手后吸附到更近的左右边缘。
3. 点一下圆球，展开面板（主页、快捷、关闭悬浮球）。再点圆球、点面板空白，或点圆球外面，面板收起。点外面时，这次触摸仍会落到下面的应用。
4. 面板里的「关闭悬浮球」，或主页里关掉开关，都会拿掉悬浮窗并停止前台服务。
5. 从最近任务划掉或进程被杀之后，不会自己再启动。下次打开应用时，如果开关还是开的并且权限还在，会恢复悬浮球。

## 在电脑上构建

需要 JDK 17、Android SDK Platform 37 和 Build-Tools 37。

```bash
export ANDROID_HOME="$HOME/Android/Sdk"   # 改成你的 SDK 路径
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Debug APK 在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

构建时会丢掉 armeabi-v7a、x86、x86_64 的原生库，只保留 arm64-v8a。装到 64 位 ARM 手机上即可。

安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

覆盖贴边计算、开关是否允许显示，以及主页在「未授权 / 已开启」时的文案和点击。

## 下载 CI 打出来的包

推到 `main` 后，GitHub Actions 工作流 [Debug APK](.github/workflows/debug-apk.yml) 会跑测试、打 debug APK，并上传：

- Actions artifact：`floating-ball-debug-arm64`
- Prerelease：`v0.1.0-debug`（说明里有这次构建的 SHA-256）

直链（发布成功后可用）：

```text
https://github.com/zts5202/floating-ball/releases/download/v0.1.0-debug/floating-ball-0.1.0-debug-arm64.apk
```

versionName `0.1.0`，versionCode `1`。SHA-256 以该次 Release 说明和 artifact 里的 `apk-sha256.txt` 为准，不要用手算旧包。

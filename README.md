# 163MusicPro (163音乐Pro)

适用于小天才手表的网易云音乐播放器 v2.0。

## 功能特性

- 🎵 **音乐搜索** - 搜索网易云音乐曲库
- ▶️ **音乐播放** - 支持播放控制（上一首/下一首/暂停/播放）
- 🔄 **自动播放** - 播放完毕或切歌时自动开始播放下一首
- 🔊 **音量调节** - 便捷的音量增减按钮
- ❤️ **收藏功能** - 本地收藏喜欢的歌曲，支持删除重装后自动恢复
- 📱 **扫码登录** - 使用网易云音乐App扫码登录
- 💬 **短信登录** - 手机号验证码登录
- 🔑 **Cookie登录** - 手动设置Cookie实现VIP音乐播放
- 🔒 **空Cookie保护** - 登录返回空Cookie时不会覆盖已有登录状态
- 🛡️ **后台保活** - 前台服务+唤醒锁确保后台和黑屏时不被杀死
- 🎲 **播放模式** - 列表循环 / 单曲循环 / 随机播放

## v2.0 更新说明

- 🆕 新增自定义应用图标
- 🔧 修复短信登录"环境不安全"错误
- 📐 修复二维码显示不完整问题
- ▶️ 修复切歌后无法自动播放问题
- ❤️ 收藏列表自动从外部存储恢复（删除重装后自动加载）
- 🛡️ 新增前台服务保活机制，防止后台被杀
- 🔒 登录返回空Cookie时不再覆盖已有Cookie
- 📦 包名更新为 `com.qinghe.music163pro`
- 🚀 新增自动发布工作流（合并到main自动发布Release）

## 技术要求

- Android 7.0+ (API 24)
- 目标: Android 8.0 (API 26)
- 屏幕: 360x320 (小天才手表)

## 构建

### 调试版本
```bash
./gradlew assembleDebug
```
APK输出路径: `app/build/outputs/apk/debug/app-debug.apk`

### 签名发布版本
```bash
# 需要先配置签名环境变量
export KEYSTORE_BASE64="<base64编码的keystore>"
export KEYSTORE_PASSWORD="<keystore密码>"
export KEY_ALIAS="<key别名>"
export KEY_PASSWORD="<key密码>"

# 解码keystore
echo "$KEYSTORE_BASE64" | base64 -d > release.keystore

# 构建签名APK
./gradlew assembleRelease
```
APK输出路径: `app/build/outputs/apk/release/app-release.apk`

## 自动发布 (CI/CD)

当代码合并到 `main` 分支时，GitHub Actions 会自动构建并发布签名APK到 GitHub Releases。

### 配置签名密钥（仓库管理员操作）

在仓库的 **Settings → Secrets and variables → Actions** 中添加以下 Secrets：

| Secret 名称 | 说明 | 示例值 |
|---|---|---|
| `KEYSTORE_BASE64` | keystore文件的base64编码 | `MIIKvg...` |
| `KEYSTORE_PASSWORD` | keystore密码 | `music163pro2024` |
| `KEY_ALIAS` | 密钥别名 | `music163pro` |
| `KEY_PASSWORD` | 密钥密码 | `music163pro2024` |

#### 生成签名密钥

如果需要生成新的签名密钥：

```bash
# 1. 生成keystore文件
keytool -genkeypair -v \
  -keystore music163pro-release.keystore \
  -alias music163pro \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass <你的密码> \
  -keypass <你的密码> \
  -dname "CN=163MusicPro, OU=QingHe, O=QingHe, L=Beijing, ST=Beijing, C=CN"

# 2. 转换为base64
base64 -w 0 music163pro-release.keystore
# 将输出的base64字符串设置为 KEYSTORE_BASE64 secret
```

#### 配置步骤

1. 打开仓库页面 → **Settings**
2. 左侧菜单选择 **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. 依次添加上面 4 个 Secret
5. 完成后，每次合并到 main 分支都会自动发布签名APK

> ⚠️ **注意**: 签名密钥通过 GitHub Secrets 存储，不会出现在仓库代码中，其他人无法直接查看。

## 使用说明

1. 从 [Releases](../../releases) 页面下载最新APK
2. 将APK传输到手表并安装
3. 打开应用，点击右上角 **⋮** 进入更多界面
4. 在更多界面中搜索歌曲，点击即可播放
5. 如需播放VIP音乐，点击 **⚙** 进入设置：
   - **扫码登录**: 使用网易云音乐App扫描二维码
   - **短信登录**: 输入手机号获取验证码登录
   - **手动Cookie**: 直接粘贴Cookie
6. 点击 **♡** 收藏喜欢的歌曲
7. 收藏数据保存在 `/sdcard/163Music/favorites.json`，删除重装后自动恢复

## 项目信息

| 项目 | 值 |
|---|---|
| 包名 | `com.qinghe.music163pro` |
| 版本 | 2.0 (versionCode 2) |
| 最低SDK | Android 7.0 (API 24) |
| 目标SDK | Android 8.0 (API 26) |
| 编译SDK | Android 14 (API 34) |

# JFZip

相册多选图片后分享到 JFZip，按 `yy.MM.dd-序号` 重命名并打包为 `yy.MM.dd-姓名.zip`。

压缩包只写入应用缓存目录，下次启动或重新生成时清理。

## 构建

```bash
./gradlew assembleRelease
```

## 发布

推送 `v` 开头的标签，GitHub Actions 会构建 APK 并创建 Release：

```bash
git tag v1.0.0
git push origin v1.0.0
```

也可以手动触发 `build` workflow，APK 会作为 artifact 上传。

## 签名

在仓库 Settings > Secrets and variables > Actions 中配置：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

生成密钥并转为 base64：

```bash
keytool -genkeypair -v -keystore release.jks -alias jfzip \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks
```

未配置密钥时，release 构建使用 debug 签名。如果之前安装过 debug 签名版本，安装正式签名版本前需要先卸载。

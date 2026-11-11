# JFZip Web

静态 PWA。图片在浏览器内重命名并打包为 ZIP，不上传服务器。

## 开发

```bash
npm install
npm run dev
```

## 构建

```bash
npm run build
```

产物在 `dist/`。

## 部署

把 `dist/` 内容上传到任意静态目录，通过 HTTPS 访问。页面使用相对路径，可以部署在根目录，也可以部署在 `/jfzip/` 这类子目录；地址建议保留结尾斜杠。

Service Worker 需要 HTTPS（`localhost` 除外）。建议缓存策略：

- `sw.js`、`index.html`：`no-cache`
- `assets/*`：`public, max-age=31536000, immutable`
- 图标和 manifest：按普通静态文件处理

## 说明

- iOS Safari 不支持 Web Share Target，网页不能出现在相册分享面板中；需要在网页内选择图片。
- 生成完成后单独点击“分享”，不要在异步生成后直接调用 `navigator.share`。
- 大批量图片受 Safari 内存限制，建议每批控制在 50～100 张以内。
- HEIC 保留原扩展名，不做转换。

# Rail Ticket Web

铁路票务系统前端，使用 Vue 3、TypeScript、Vite、Pinia、Vue Router 和 Axios。

## 本地启动

1. 先启动 MySQL、Redis、RabbitMQ 和 Spring Boot 后端（默认端口 `8080`）。
2. 在本目录执行：

```bash
npm install
npm run dev
```

3. 浏览器访问 `http://localhost:5173`。开发服务器会把 `/api` 代理至后端。

## 构建

```bash
npm run build
```

生产文件输出到 `dist/`。前后端分开部署时，可通过 `VITE_API_BASE_URL` 指定后端地址。

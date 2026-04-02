# Coal 煤炭业务统计与报表管理系统

基于 **Spring Boot 3 + Vue 3 + SQL Server** 构建的企业级统计填报、审批流转与报表导出平台，面向煤炭集团多层级组织场景，重点解决“多单位协同填报 + 严格审批链路 + 累计口径一致 + 历史模板兼容导出”等复杂业务问题。

项目核心技术特性：

- **统一元数据驱动**：后端按模块配置动态生成列结构、查询能力与导出逻辑，支持多报表模块复用同一套通用框架
- **前后端累计口径一致**：前端实时累计预览，后端统一规则校准落库，避免人工口径偏差
- **多层级审批引擎**：基于单位层级自动推导审批深度，支持提交、审批、退回修改的完整闭环
- **高保真 Excel 导出**：按历史业务模板结构输出，兼顾格式兼容与数据一致性
- **可扩展架构**：Spring Boot 3 + Vue 3 组合，前后端职责清晰，便于持续扩展新模块和新指标

---

## 部署指引

### 1. 数据库配置 (SQL Server)

1. **新建数据库**：在 SQL Server 中创建一个名为 `coal` 的数据库。
2. **导入表结构**：执行提供的 SQL 脚本以初始化数据表。
3. **后端连接配置**：
   - 打开 `COAL/springboot/src/main/resources/application.yml`。
   - 修改 `spring.datasource` 下的 `username`、`password` 和 `url`。
   - **注意**：默认配置中使用了 `14330` 端口，请根据您的 SQL Server 实际端口进行调整。

```yaml
spring:
  datasource:
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    username: sa
    password: YOUR_PASSWORD
    url: jdbc:sqlserver://localhost:14330;databaseName=coal;encrypt=false;trustServerCertificate=true
```

### 2. 后端部署 (Spring Boot)

1. **进入目录**：
   ```bash
   cd COAL/springboot
   ```
2. **依赖安装与编译**：
   ```bash
   mvn clean install
   ```
3. **启动项目**：
   ```bash
   mvn spring-boot:run
   ```
   *后端默认运行在 `http://localhost:9090`*

### 3. 前端部署 (Vue 3)

1. **进入目录**：
   ```bash
   cd COAL/vue
   ```
2. **API 地址配置**：
   - 修改 `.env.development` 中的 `VITE_BASE_URL` 指向后端地址（默认为 `http://localhost:9090`）。
3. **安装依赖**：
   ```bash
   npm install
   ```
4. **运行开发服务器**：
   ```bash
   npm run dev
   ```
5. **生产环境构建** (可选)：
   ```bash
   npm run build
   ```

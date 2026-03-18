# Coal 煤炭业务管理系统

基于 **Spring Boot 3 + Vue 3 + SQL Server** 构建的高效、现代化的煤炭业务数据统计与报表管理系统。

---

## 🛠️ 环境要求

在部署本项目之前，请确保您的环境中已安装以下软件：

- **JDK 17+** (推荐版本 17)
- **Node.js 18+** (推荐版本 20, 包含 npm)
- **Maven 3.8+**
- **SQL Server 2017+**

---

## 📂 项目结构

```text
Coal-springboot3+vue3/
├── COAL/
│   ├── springboot/  # 后端项目 (Spring Boot 3)
│   └── vue/         # 前端项目 (Vue 3 + Vite)
└── ...              # 历史备份与 Excel 模版
```

---

## 🚀 部署指引

### 1. 数据库配置 (SQL Server)

1. **新建数据库**：在 SQL Server 中创建一个名为 `coal` 的数据库。
2. **导入表结构**：执行提供的 SQL 脚本以初始化数据表。
3. **后端连接配置**：
   - 打开 `COAL/springboot/src/main/resources/application.yml`。
   - 修改 `spring.datasource` 下的 `username`、`password` 和 `url`。
   - **注意**：默认设置中使用了 `14330` 端口，请根据您的 SQL Server 实际端口进行调整。

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

---

## 📝 系统特性

- **双列填报布局**：支持本月值与累计值实时同步计算，减少人工计算误差。
- **动态审批流**：根据单位层级（基层单位到集团）自动计算审批深度。
- **通用报表管理**：利用反射机制实现的通用报表逻辑，支持多种业务数据的扩展。
- **Excel 导出**：支持Excel 报表导出，自动进行四舍五入处理。

---

## ⚠️ 注意事项

- **基层单位限制**：除“产、销、存”等少数模块外，只有被标记为“基层单位”的账号才能新增报表数据。
- **文件上传/导出**：运行环境需具备文件读写权限，以便系统生成临时报表文件或保存附件。
- **数据一致性**：前端实时计算的累计值仅用于预览，系统在保存时会以后端 `CumulativeUtils` 的计算结果为准进行落库。

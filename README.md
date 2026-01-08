# Coal 煤炭管理系统

基于 **Spring Boot 3 + Vue 3** 的全栈管理系统。

---

## 后端配置

后端配置文件位于：`COAL/springboot/src/main/resources/application.yml`

```yaml
server:
  port: 9090  # 后端端口，可按需修改

spring:
  datasource:
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    username: sa              # 数据库用户名
    password: 123456          # 数据库密码
    url: jdbc:sqlserver://localhost:14330;databaseName=coal;encrypt=false;trustServerCertificate=true
    # localhost:14330 改为你的 SQL Server 地址和端口

fileBaseUrl: http://localhost:${server.port}  # 文件访问地址
```

---

## 数据库配置

使用 **SQL Server** 数据库，需创建 `coal` 数据库并建表：

```sql
-- 管理员表
CREATE TABLE admin (
    id INT PRIMARY KEY IDENTITY(1,1),
    username NVARCHAR(50) NOT NULL UNIQUE,
    password NVARCHAR(100) NOT NULL,
    name NVARCHAR(50),
    avatar NVARCHAR(255),
    role NVARCHAR(20),
    phone NVARCHAR(20),
    email NVARCHAR(100)
);

-- 公告表
CREATE TABLE notice (
    id INT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(200) NOT NULL,
    content NVARCHAR(MAX),
    time NVARCHAR(50)
);

-- 默认管理员（密码 123456）
INSERT INTO admin (username, password, name, role) 
VALUES ('admin', '123456', '系统管理员', 'ADMIN');
```

---

## 启动项目

```bash
# 后端
cd COAL/springboot
mvn spring-boot:run

# 前端
cd COAL/vue
npm install
npm run dev
```

> 前端 API 地址配置在 `.env.development` 文件中的 `VITE_BASE_URL`

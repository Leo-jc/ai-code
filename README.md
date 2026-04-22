# Serain AI Code - AI 代码生成器

一个基于 Spring Boot + Vue 3 的全栈 AI 代码生成器平台，用户可以通过与 AI 对话来生成网站应用、实时预览效果并一键部署。

## 项目简介

Serain AI Code 是一个智能代码生成平台，集成了多种 AI 模型（DeepSeek、阿里通义千问等），支持生成 HTML 单页应用、多文件项目以及 Vue 项目。平台提供完整的应用生命周期管理，包括创建、编辑、部署和分享功能。

## 功能特性

### 核心功能

- 🤖 **AI 智能对话**：基于 LangChain4j 和 LangGraph4j 实现的多轮对话系统
- 💻 **代码生成**：支持 HTML、多文件项目、Vue 项目三种代码生成类型
- 👁️ **实时预览**：生成的代码可实时预览效果
- 🚀 **一键部署**：支持将应用部署到云端，生成可访问链接
- 📱 **响应式设计**：前端采用响应式布局，支持多端访问

### 用户功能

- 🔐 **用户认证**：支持用户注册、登录、登出
- 📝 **应用管理**：创建、编辑、删除个人应用
- 📋 **应用列表**：分页查询我的应用和精选应用
- 🔍 **应用搜索**：支持按名称搜索应用
- ⭐ **精选应用**：查看平台推荐的优质应用

### 管理员功能

- 🔧 **应用管理**：管理所有用户创建的应用
- 👥 **用户管理**：查看和管理平台用户
- ⭐ **精选设置**：设置应用为精选状态
- 📊 **数据监控**：集成 Spring Boot Actuator 和 Prometheus 监控

## 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.4 | 核心框架 |
| Java | 21 | 编程语言 |
| MyBatis-Flex | 1.11.1 | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |
| Redis | - | 缓存与会话存储 |
| LangChain4j | 1.1.0 | AI 应用开发框架 |
| LangGraph4j | 1.6.0-rc2 | AI 工作流编排 |
| Knife4j | 4.4.0 | API 文档生成 |
| Lombok | 1.18.40 | 代码简化工具 |

### AI 集成

- **DeepSeek API**：主要对话模型
- **阿里 DashScope**：通义千问模型支持
- **流式响应**：支持 SSE 实时流式输出

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.17 | 前端框架 |
| TypeScript | 5.8.x | 类型安全支持 |
| Ant Design Vue | 4.2.6 | UI 组件库 |
| Vite | 7.0.0 | 构建工具 |
| Pinia | 3.0.3 | 状态管理 |
| Vue Router | 4.5.1 | 路由管理 |
| Axios | 1.11.0 | HTTP 客户端 |

### 项目结构

```
serain-ai-code/
├── src/main/java/com/serain/serainaicode/    # 后端源码
│   ├── ai/                                   # AI 相关服务
│   ├── config/                               # 配置类
│   ├── controller/                           # 控制器层
│   ├── core/                                 # 核心业务逻辑
│   ├── mapper/                               # 数据访问层
│   ├── model/                                # 数据模型
│   ├── service/                              # 业务逻辑层
│   └── SerainAiCodeApplication.java          # 启动类
├── src/main/resources/                       # 配置文件
│   ├── application.yml                       # 主配置
│   ├── mapper/                               # MyBatis XML
│   └── prompt/                               # AI 提示词模板
├── serain-ai-code-frontend/                  # 前端项目
│   ├── src/
│   │   ├── api/                              # API 接口
│   │   ├── components/                       # 公共组件
│   │   ├── pages/                            # 页面组件
│   │   ├── router/                           # 路由配置
│   │   └── stores/                           # 状态管理
│   └── package.json
├── sql/                                      # 数据库脚本
└── pom.xml                                   # Maven 配置
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+

### 数据库初始化

1. 创建数据库：
```sql
create database if not exists yu_ai_code_mother;
```

2. 执行初始化脚本：
```bash
mysql -u root -p yu_ai_code_mother < sql/create_table.sql
```

### 后端启动

1. 修改配置：
编辑 `src/main/resources/application.yml`，配置数据库和 AI API 密钥：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yu_ai_code_mother
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

langchain4j:
  open-ai:
    chat-model:
      api-key: your-deepseek-api-key
```

2. 启动项目：
```bash
# 方式一：使用 Maven
mvn spring-boot:run

# 方式二：使用 Maven Wrapper
./mvnw spring-boot:run
```

后端服务默认运行在 `http://localhost:8123/api`

### 前端启动

```bash
cd serain-ai-code-frontend

# 安装依赖
npm install

# 开发环境运行
npm run dev
```

前端服务默认运行在 `http://localhost:5173`

## API 文档

项目集成了 Knife4j（Swagger UI），启动后可访问：

- 开发环境：`http://localhost:8123/api/doc.html`

## 主要模块说明

### AI 代码生成模块

- **AiCodeGeneratorService**：AI 代码生成服务接口
- **AiCodeGeneratorFacade**：代码生成外观模式，整合解析和保存流程
- **CodeParser**：代码解析器，支持 HTML 和多文件解析
- **CodeFileSaver**：代码文件保存器，支持本地和云端存储

### 用户认证模块

- 基于 Spring Session + Redis 实现分布式会话
- 支持用户注册、登录、登出
- 基于注解的权限控制（@AuthCheck）

### 应用管理模块

- 应用的 CRUD 操作
- 应用部署功能
- 精选应用推荐

### 对话历史模块

- 存储用户与 AI 的对话记录
- 支持游标分页查询
- 对话上下文管理

## 配置说明

### 应用配置 (application.yml)

```yaml
# 服务端配置
server:
  port: 8123
  servlet:
    context-path: /api

# AI 模型配置
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
      max-tokens: 8192
```

### 前端环境配置

- `.env.development`：开发环境配置
- `.env.production`：生产环境配置

## 部署说明

### 后端部署

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/serain-ai-code-0.0.1-SNAPSHOT.jar
```

### 前端部署

```bash
cd serain-ai-code-frontend

# 构建生产环境
npm run build

# 构建产物位于 dist/ 目录
```

## 监控与运维

项目集成了 Spring Boot Actuator 和 Prometheus 监控：

- 健康检查：`/api/actuator/health`
- Prometheus 指标：`/api/actuator/prometheus`

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 开源协议

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

如有问题或建议，欢迎提交 Issue 或 Pull Request。

---

**注意**：本项目仅供学习和参考使用，生产环境部署请根据实际需求进行安全加固和配置优化。

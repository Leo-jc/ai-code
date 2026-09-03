# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

全栈 AI 代码生成器：Spring Boot 后端（仓库根目录）+ Vue 3 前端（`serain-ai-code-frontend/`）。用户与 AI 对话生成应用代码（HTML / 多文件 / Vue 项目），实时预览（SSE 流式），一键部署。

依赖服务：JDK 21、MySQL（库名 `yu_ai_code_mother`，建表脚本 `sql/create_table.sql`）、Redis（Session + AI 对话记忆）。AI 走 DeepSeek（LangChain4j OpenAI 兼容接口），配置在 `src/main/resources/application.yml`。

## 常用命令

后端（仓库根目录）：

```bash
./mvnw spring-boot:run                # 启动，http://localhost:8123/api
mvn clean package -DskipTests         # 打包
mvn test                              # 全部测试
mvn test -Dtest=CodeParserTest        # 单个测试类
```

注意：部分测试（如 `AiCodeGeneratorServiceTest`）会真实调用 AI API，需要有效的 api-key 和网络。

前端（`serain-ai-code-frontend/`）：

```bash
npm run dev            # 启动，http://localhost:5173（/api 代理到 8123）
npm run build          # type-check + 构建
npm run pure-build     # 跳过 type-check 的构建
npm run lint           # eslint --fix
npm run format         # prettier
npm run openapi2ts     # 从后端 OpenAPI 重新生成 src/api/*（需后端已启动）
```

`src/api/` 下的请求代码和类型由 `openapi2ts` 自动生成（配置在 `openapi2ts.config.ts`，schema 取自 `http://localhost:8123/api/v3/api-docs`），不要手改。

API 文档：`http://localhost:8123/api/doc.html`（Knife4j）。

## 核心架构：代码生成主流程

`AppController.chatToGenCode`（`/app/chat/gen/code`，SSE 流式）
→ `AppServiceImpl.chatToGenCode`（校验权限、写用户消息到对话历史；当前硬编码为 `VUE_PROJECT` 类型）
→ `AiCodeGeneratorFacade.generateAndSaveCodeStream`
→ `AiCodeGeneratorServiceFactory` 按 `appId + codeGenType` 从 Caffeine 缓存取 AI 服务实例
→ 流式响应经 `StreamHandlerExecutor` 选择处理器
→ 流结束后 `CodeParserExecutor` 解析 + `CodeFileSaverExecutor` 保存。

关键设计：

- **AI 服务实例工厂**（`ai/AiCodeGeneratorServiceFactory.java`）：每个实例绑定独立的 `MessageWindowChatMemory`（Redis 存储，最多 20 条），创建时从 MySQL 对话历史回放。`VUE_PROJECT` 使用推理流式模型 + `FileWriteTool`（`@Tool` 让 AI 直接写文件到 `tmp/code_output/vue_project_{appId}/`）；`HTML`/`MULTI_FILE` 使用默认模型。
- **两种流格式**：`HTML`/`MULTI_FILE` 输出纯文本 `Flux<String>`，由 `SimpleTextStreamHandler` 处理；`VUE_PROJECT` 输出 JSON 消息（`ai/model/message/` 下的 `AiResponseMessage`/`ToolRequestMessage`/`ToolExecutedMessage`），由 `JsonMessageStreamHandler` 解析分发，并在流完成/出错时把 AI 回复写回对话历史。
- **解析与保存**：HTML/多文件类型从 AI 回复的代码块中解析（`core/parser/`），经模板方法模式的 Saver（`core/saver/`）落盘到 `tmp/code_output/{type}_{appId}`。
- **部署**（`AppServiceImpl.deployApp`）：Vue 项目先由 `VueProjectBuilder` 执行 `npm install` + `npm run build`（Windows 下用 `npm.cmd`，注意命令拆分），把 `dist` 复制到 `tmp/code_deploy/{deployKey}`，通过 `StaticResourceController`（`/api/static/**`）对外访问；URL 由 `AppConstant.CODE_DEPLOY_HOST` 拼接。
- **对话记忆双写**：MySQL `chat_history` 表是持久层（游标分页查询），Redis 是 LangChain4j 的 ChatMemory 存储，两者通过 `loadChatHistoryToMemory` 衔接。删除应用时级联删对话历史（`AppServiceImpl.removeById`）。
- **权限**：Spring Session + Redis 分布式会话；`@AuthCheck(mustRole=...)` 注解 + `AuthInterceptor`（AOP）做角色校验。

## 重要陷阱

- `src/main/java/dev/langchain4j/**` 是**覆盖库内部类的补丁副本**（为支持工具调用的流式回调），编译时会遮蔽同名库类。改动需谨慎，升级 LangChain4j 版本可能产生冲突或使补丁失效。
- 生成代码和部署产物都在 `{user.dir}/tmp/` 下（`AppConstant.CODE_OUTPUT_ROOT_DIR` / `CODE_DEPLOY_ROOT_DIR`），构建/部署前确认目录存在。
- `MyBatis-Flex` 实体使用 APT 生成的 Table 类做查询，`controller` 中同一功能常有新旧两套接口（如 `/my/list/page` 与 `/my/list/page/vo`）共存，改接口前先确认前端实际调用的是哪一个。

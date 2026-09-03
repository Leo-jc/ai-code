# 意图识别 + 前端知识 RAG 设计文档

日期：2026-09-03
状态：已与用户逐节确认

## 背景与目标

当前 `AppServiceImpl.chatToGenCode` 将所有用户消息硬编码为 `VUE_PROJECT` 代码生成。需要新增意图识别：根据用户消息判断是「生成代码」还是「前端专业知识问答」。

- 生成代码 → 由意图识别自动选择代码格式（HTML / MULTI_FILE / VUE_PROJECT 三种全启用）
- 知识问答 → 通过 RAG 检索本地前端文档回答
- 前端零改动，复用现有 SSE 接口与事件格式

## 已确认的决策

| 决策点 | 结论 |
|---|---|
| 知识库来源 | 本地文档文件（`docs/knowledge/` 下 markdown） |
| 意图识别方式 | LLM 分类（轻量 prompt 返回 JSON） |
| 代码格式路由 | HTML / MULTI_FILE / VUE_PROJECT 三种全由意图识别选择 |
| RAG 回答形式 | 与生成同接口（SSE）流式返回，问答内容同样写入对话历史并作为后续上下文 |
| 总体方案 | 方案 A：独立 AiService 接口 + 独立 RAG Service（否决了 tool-calling 路由和规则预分类） |

## 总体架构

```
用户消息 (SSE /app/chat/gen/code)
  │
  ▼
AppServiceImpl.chatToGenCode
  │
  ├─► IntentClassifierService.classifyIntent(message)   ← 新增，非流式小模型，返回 JSON
  │       意图 = CODE_GEN | KNOWLEDGE_QA
  │       CODE_GEN 时附带 codeGenType: HTML | MULTI_FILE | VUE_PROJECT
  │
  ├─[CODE_GEN]──► 现有链路不变：Facade → ServiceFactory(按识别的 codeGenType) → StreamHandler → Parser/Saver
  │
  └─[KNOWLEDGE_QA]──► FrontendKnowledgeQaService.chat(memoryId, message)   ← 新增 AiService
                          │ 内置 ContentRetriever → 向量库检索前端文档
                          ▼
                        流式回答 → StreamHandlerExecutor(SimpleTextStreamHandler) → 写对话历史 → SSE 返回前端
```

要点：

- 意图识别不进对话记忆：分类器是无 memory 的独立 AiService，纯函数式调用，避免污染生成上下文。
- 问答有独立记忆：`FrontendKnowledgeQaService` 按 appId 挂独立 `MessageWindowChatMemory`（Redis 存储 + MySQL 历史回放），memoryId 格式 `qa_{appId}`，与代码生成记忆（`{appId}`）隔离，多轮追问可接续。
- 前端零改动：SSE 接口、事件格式（`{d:...}` + done）完全复用。
- 意图识别结果只写日志，不新增 ChatHistory 表字段（YAGNI）。

## 组件明细

### 1. `ai/IntentClassifierService.java`（新增）

```java
public interface IntentClassifierService {
    @SystemMessage(fromResource = "prompt/intent-classify-system-prompt.txt")
    IntentResult classifyIntent(@UserMessage String userMessage);
}
```

- 绑定非流式 `chatModel`（deepseek-chat），`max-tokens` 调低
- `IntentResult`：`intent`（CODE_GEN / KNOWLEDGE_QA）+ `codeGenType`（HTML / MULTI_FILE / VUE_PROJECT，非生成时为 null），用 LangChain4j 结构化输出映射 JSON
- 无 memory、无 tools，单次调用
- Prompt 文件 `prompt/intent-classify-system-prompt.txt`：三类判别规则 + few-shot 例子（"做个购物车页面"→CODE_GEN/VUE_PROJECT；"flex 布局怎么居中"→KNOWLEDGE_QA；"transform 属性有哪些"→KNOWLEDGE_QA）

### 2. `ai/FrontendKnowledgeQaService.java`（新增）

```java
public interface FrontendKnowledgeQaService {
    @SystemMessage(fromResource = "prompt/knowledge-qa-system-prompt.txt")
    TokenStream chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
```

- 绑定流式模型 + `ContentRetriever`
- memoryId 格式 `qa_{appId}`
- Prompt 约定：只基于检索到的文档回答，检索不到就明说不知道，不编造；回答带来源文件名

### 3. `rag/` 包（新增）

- `rag/config/EmbeddingModelConfig.java` — DashScope `text-embedding-v3`，需新增依赖 `langchain4j-dashscope`
- `rag/store/EmbeddingStoreConfig.java` — Redis 向量库（复用 `langchain4j-community-redis-spring-boot-starter` 现有依赖与现有 Redis 实例）
- `rag/ingest/DocumentIngestService.java` — 导入器：读 `docs/knowledge/*.md` → 按标题/段落切片（500 token，重叠 50）→ 向量化入库。手动触发：管理员接口 `POST /rag/ingest`（`@AuthCheck(ADMIN)`），不做定时任务
- `rag/retriever/FrontendContentRetriever.java` — 包装 EmbeddingStoreContentRetriever，topK=4、minScore=0.6

### 4. `core/AiCodeGeneratorFacade.java` 改动

- 新增方法 `chat(String message, Long appId)`：调意图分类 → CODE_GEN 走现有 `generateAndSaveCodeStream`，KNOWLEDGE_QA 走问答服务 → 返回统一 `Flux<String>`
- `AppServiceImpl.chatToGenCode` 改为调用 `aiCodeGeneratorFacade.chat(...)`，替换原硬编码 `VUE_PROJECT`；流仍交给 `StreamHandlerExecutor`（问答流走 `SimpleTextStreamHandler` 纯文本分支，复用 `AI_RESPONSE` 消息格式）

### 5. 依赖变更（pom.xml）

- 新增 `langchain4j-dashscope`（embedding 模型）
- 其余复用现有依赖

## 数据流

**代码生成分支：**

```
classify(~1-2s, 非流式)
  → {intent: CODE_GEN, codeGenType: VUE_PROJECT}
  → chatHistoryService.addChatMessage(USER 消息)          ← 现有逻辑，位置不变
  → Factory.getAiCodeGeneratorService(appId, 识别的类型)   ← 替换原硬编码
  → 流式生成 → StreamHandler → 保存 tmp/code_output/{type}_{appId}
```

**知识问答分支：**

```
classify(~1-2s)
  → {intent: KNOWLEDGE_QA}
  → addChatMessage(USER 消息)                              ← 同样写历史
  → FrontendKnowledgeQaService.chat("qa_" + appId, message)
      ├─ Redis 向量库检索 top-4 切片
      ├─ 切片注入 prompt 检索上下文
      └─ 流式回答（TokenStream → Flux<String>，复用 processTokenStream 转换）
  → SimpleTextStreamHandler 收集完整回答 → 写 AI 消息到历史 → SSE 吐给前端
```

**文档导入流（手动、管理员）：**

```
docs/knowledge/*.md → DocumentIngestService.ingest()
  → 切片（带 sourcePath 元数据）→ embedding → Redis 向量库
  → 重复导入按 sourcePath 先删旧切片再写入（幂等）
```

**时序：** 意图分类发生在首字节之前，首字延迟 = 分类 1-2s + 模型首 token；问答分支额外检索 ~100-300ms。用户已确认可接受。

**对话历史格式：** 问答回复为纯文本 markdown，生成回复含 `[工具调用] 写入文件...` 标记，同表存储；`JsonMessageStreamHandler` 兼容，前端 markdown-it 渲染无需改动。

## 错误处理

| 故障点 | 策略 |
|---|---|
| 意图分类调用失败/超时 | 兜底 `CODE_GEN` + `MULTI_FILE`，warn 日志，不中断 |
| 意图分类返回非法枚举值 | 同上兜底，异常路径不重试 |
| 向量库检索失败 | 降级为无检索上下文直接问 LLM |
| 检索结果为空 | Prompt 约定回答"知识库中未找到相关内容"，不编造 |
| 问答 LLM 流中断 | `doOnError` 写错误消息到历史，SSE 正常关闭 |
| embedding 调用失败（导入时） | 导入接口整体报错返回，幂等设计保证重跑无副作用 |
| DashScope api-key 未配置 | embedding bean 懒加载，缺失时在导入/问答时暴露 |

原则：问答功能永远不能搞挂代码生成主链路——分类失败兜底生成、检索失败兜底直答。

## 测试策略

**单元测试（不调外部 API）：**

- `IntentResult` 反序列化：正常 / 缺字段 / 非法枚举 → 正确映射或兜底
- `DocumentIngestService` 切片：切片数量、重叠、元数据正确；重复导入清除旧切片
- `FrontendContentRetriever`：mock EmbeddingStore → topK/minScore 生效、检索异常返回空列表

**集成测试（调真实 API，默认 `@Disabled`，同现有 `AiCodeGeneratorServiceTest` 风格）：**

- 分类准确性：10 条代表性消息（生成/问答各半）断言意图和类型
- RAG 全链路：导入测试文档 → 提问 → 流式返回含文档内容
- 记忆隔离：同一 appId 先问知识再生成 → 生成记忆未被污染

**手动验收：**

- `POST /rag/ingest` 导入 → 问"flex 怎么水平居中" → 流式回答带来源
- 问"做一个贪吃蛇" → 走代码生成，类型自动判定
- 清空向量库 → 问知识问题 → 降级回答而非报错

**Prompt 迭代基线：** 分类 few-shot 集合存放在测试类中，调 prompt 时对比准确率。

## 实施边界（不做）

- 不做定时同步文档任务
- 不新增 ChatHistory 表字段
- 不做前端 UI 改动
- 不做多级置信度混合路由

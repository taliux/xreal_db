**产品需求文档（PRD）**

**产品名称**

Xreal智能客服--FAQ 问答异构数据库管理服务

**背景与目标**

为提升问答系统的扩展性、可维护性与查询性能，本系统基于 **MySQL + Elasticsearch** 的异构数据库架构。

**数据结构设计**

**FAQ 数据模型（MySQL）**

| **字段名** | **类型** | **描述** |
| --- | --- | --- |
| id  | BIGINT | 自动生成的唯一标识符（主键） |
| question | TEXT | 问题内容 |
| answer | TEXT | 答案内容 |
| instruction | TEXT? | 可选的引导信息（如有） |
| url | VARCHAR | 可选外链 URL |
| timestamp | DATETIME | 最后修改时间（自动更新） |
| active | BOOLEAN | 是否为有效记录 |
| comment | TEXT | 业务人员自用注解 |

**标签管理模型（MySQL）**

**表：tag**

| **字段名** | **类型** | **描述** |
| --- | --- | --- |
| name | VARCHAR | 标签名称（主键） |
| description | TEXT | 标签描述（可选） |
| active | BOOLEAN | 是否启用该标签（默认启用） |

**表：faq_tag**

| **字段名** | **类型** | **描述** |
| --- | --- | --- |
| faq_id | BIGINT | 外键，指向 FAQ 表 |
| tag | VARCHAR | 外键，指向 tag 表的 name |

- 使用 faq_tag 实现多对多关系
- 强制使用系统预定义标签（通过外键约束）

**Elasticsearch 索引结构**

{

"settings": {

"index": {

"number_of_shards": 1,

"number_of_replicas": 0

}

},

"mappings": {

"properties": {

"content": {

"type": "text"

},

"metadata": {

"properties": {

"tags": {

"type": "keyword"

},

"question": {

"type": "text"

},

"answer": {

"type": "text"

},

"instruction": {

"type": "text"

},

"url": {

"type": "text"

},

"active": {

"type": "boolean"

},

"timestamp": {

"type": "date"

}

}

},

"embedding": {

"type": "dense_vector",

"dims": 1536,

"index": true,

"similarity": "cosine" /

}

}

}

}

**Elasticsearch 文档示例**

{

"id": "123",

"content": "How to use Xreal Air series? \[Applicable to Xreal Air, Xreal Air 2\]",

"embedding": \[0.12, 0.33, ..., 0.04\],

"metadata": {

"question": " How to use Xreal Air series?",

"answer": "Blah blah blah",

"tags": \["Xreal Air ", "Xreal Air 2"\],

"instruction": "Further check …",

"url": "<https://xreal.com/user_manual>",

"timestamp": "2024-01-01T00:00:00",

"active": true

}

}

- 使用 Spring AI 接入 ElasticsearchVectorStore
- 向量字段名为 embedding，数据记录存在 metadata 内部字段
- content 由question和tags按模板自动拼接而成

**接口设计（Spring Boot）**

**FAQ 接口**

| **功能** | **接口** | **方法** | **描述** |
| --- | --- | --- | --- |
| 清除所有记录 | /faqs/all | DELETE | 删除所有 FAQ 和其标签关系 |
| 添加记录 | /faqs | POST | 添加 FAQ，附带标签 |
| 修改记录 | /faqs/{id} | PUT | 更新 FAQ 内容及标签 |
| 删除记录 | /faqs/{id} | DELETE | 删除指定 FAQ |
| 获取记录 | /faqs/{id} | GET | 根据 ID 获取记录 |
| 查询所有 | /faqs | GET | 分页 + 筛选查询所有 FAQ |
| 标签搜索 | /faqs/search | GET | 根据标签列表交集查询 FAQ |
| 获取全量标签集合 | /tags/active | GET | 返回当前启用的标签集合 |

**标签管理接口**

| **功能** | **接口** | **方法** | **描述** |
| --- | --- | --- | --- |
| 添加标签 | /tags | POST | 新增标签（name唯一） |
| 修改标签 | /tags/{name} | PUT | 修改标签描述或启用状态 |
| 删除标签 | /tags/{name} | DELETE | 删除标签（不可用于已关联 FAQ） |
| 获取全部标签 | /tags | GET | 获取所有标签（含状态） |
| 获取启用标签 | /tags/active | GET | 获取所有可用标签（活跃集合） |

**查询与筛选支持**

所有查询接口（如 /faqs, /faqs/search）支持以下参数：

- page: 页码（默认 1）
- size: 每页大小（默认 10）
- active: 是否仅返回有效记录
- sort: 排序方式（如 timestamp:desc）

标签查询逻辑为：**传入标签列表，返回包含任一标签的 FAQ 记录（交集）**

**数据一致性策略**

**写操作必须双写：**

- ✅ 写 MySQL（主库）
- ✅ 写 Elasticsearch（副本）
- ❗ 如果某方失败，执行补偿逻辑

**异常处理建议**

| **情况** | **建议** |
| --- | --- |
| MySQL 成功，ES 失败 | 回滚 MySQL（事务），记录错误日志 |
| ES 成功，MySQL 失败 | 删除 ES 文档，记录错误日志 |
| 均失败 | 返回失败 |

同时推荐配置**操作日志记录（Log + Kafka）**，用于监控与恢复。

**技术选型**

- **Spring Boot** + Spring Data JPA + Spring AI
- **数据库**：MySQL（主存储） + Elasticsearch（全文检索与标签筛选）
- **事务管理**：MySQL 内事务 + 手动控制 ES 同步
- **ID生成**：UUID / 雪花算法
- **标签体系**：独立维护 + 外键控制 + 接口管理
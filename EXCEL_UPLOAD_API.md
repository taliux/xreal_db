# Excel上传API文档

## 接口概述
提供Excel文件上传功能，支持批量导入FAQ和标签数据。

## 接口详情

### URL
```
POST /api/excel/upload
```

### Content-Type
```
multipart/form-data
```

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel文件(.xlsx或.xls格式) |

### Excel文件格式要求

#### Sheet 1: xreal_tech_faq
必须包含以下列：
- **Question** (必填): 问题内容
- **Answer** (必填): 答案内容
- **Tags** (可选): 标签，多个标签用英文逗号分隔
- **Comment** (可选): 评论/备注
- **Instruction** (可选): 指令说明
- **Url** (可选): 相关链接

#### Sheet 2: tag
必须包含以下列：
- **Name** (必填): 标签名称
- **Description** (可选): 标签描述
- **Active** (可选): 标签是否激活，使用0或1表示（0=不激活，1=激活，默认为1）

### 响应格式

成功响应 (HTTP 200):
```json
{
  "totalFaqsProcessed": 10,
  "faqsImported": 8,
  "faqsUpdated": 2,
  "faqsSkipped": 0,
  "totalTagsProcessed": 5,
  "tagsImported": 3,
  "tagsUpdated": 2,
  "unrecognizedTags": ["UnknownTag1", "UnknownTag2"],
  "message": "Excel file processed successfully",
  "processingTimeMs": 1500
}
```

### 响应字段说明
- **totalFaqsProcessed**: 处理的FAQ总数
- **faqsImported**: 新导入的FAQ数量
- **faqsUpdated**: 更新的FAQ数量（基于Question字段匹配）
- **faqsSkipped**: 跳过的FAQ数量（缺少必填字段）
- **totalTagsProcessed**: 处理的标签总数
- **tagsImported**: 新导入的标签数量
- **tagsUpdated**: 更新的标签数量
- **unrecognizedTags**: 未识别的标签列表（不在tag表中的标签）
- **message**: 处理结果消息
- **processingTimeMs**: 处理耗时（毫秒）

### 错误响应
```json
{
  "error": "文件格式错误或数据验证失败",
  "message": "Excel文件中未找到'tag'工作表"
}
```

### 处理逻辑

1. **标签处理**：
   - 首先处理tag sheet，建立有效标签集合
   - 新标签会被插入数据库
   - 已存在标签的描述和激活状态会被更新（如果有变化）
   - 只有Active=1的标签会被添加到有效标签集合中
   - Active字段支持：0/1、true/false、yes/no（不区分大小写）

2. **FAQ处理**：
   - 根据Question字段判断是新增还是更新（不区分大小写，自动去除首尾空格）
   - Question匹配时会对两边数据进行trim和转小写处理
   - Tags字段中的标签会被验证
   - 只有存在于tag表中且Active=1的标签才会被关联
   - 未识别的标签或非激活标签会被记录在响应的unrecognizedTags字段中

3. **数据验证**：
   - Question和Answer为必填字段
   - 空行会被跳过
   - 标签使用英文逗号分隔
   
4. **Elasticsearch同步**：
   - 所有FAQ数据会自动同步到Elasticsearch
   - 为每个FAQ生成embedding向量
   - 同步失败不会影响数据库操作

### 使用示例

#### cURL示例
```bash
curl -X POST http://localhost:8080/api/excel/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/data.xlsx"
```

#### HTML表单示例
```html
<form action="/api/excel/upload" method="post" enctype="multipart/form-data">
    <input type="file" name="file" accept=".xlsx,.xls" required>
    <button type="submit">上传</button>
</form>
```

### 注意事项

1. 文件大小限制由Spring Boot配置决定（默认1MB，可在application.yml中配置）
2. 建议Excel文件行数不超过10000行以避免超时
3. 标签名称区分大小写
4. FAQ更新基于Question字段的匹配（不区分大小写，自动trim）
   - 例如："How to use?" 和 " HOW TO USE? " 会被视为同一个问题
5. 所有操作在事务中执行，确保数据一致性
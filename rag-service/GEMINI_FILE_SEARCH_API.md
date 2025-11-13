# Gemini File Search API Documentation

## Tổng quan
API này cung cấp 2 endpoints để sử dụng Gemini File Search feature:
1. **Lấy danh sách File Search Stores** - Liệt kê tất cả các stores đã tạo
2. **Query với File Search** - Đặt câu hỏi dựa trên nội dung các file đã upload

## Cấu hình

Trong file `application.yml`, đảm bảo đã cấu hình Gemini API Key:

```yaml
gemini:
  api-key: ${GEMINI_API_KEY:your-api-key-here}
  model: gemini-2.0-flash-exp
```

## API Endpoints

### 1. Lấy danh sách File Search Stores

**Endpoint:** `GET /api/v1/file-search/stores`

**Mô tả:** Lấy danh sách tất cả các File Search Stores đã được tạo trong Gemini API.

**Request:**
```bash
curl -X GET "http://localhost:8093/api/v1/file-search/stores"
```

**Response:**
```json
[
  {
    "name": "fileSearchStores/my-test-store-qp0mc7qxp78s",
    "displayName": "My Test Store"
  },
  {
    "name": "fileSearchStores/another-store-abc123",
    "displayName": "Another Store"
  }
]
```

**Response Fields:**
- `name`: ID của store (sử dụng để query)
- `displayName`: Tên hiển thị của store

---

### 2. Query với File Search

**Endpoint:** `POST /api/v1/file-search/query`

**Mô tả:** Đặt câu hỏi dựa trên nội dung các file trong một File Search Store cụ thể.

**Request:**
```bash
curl -X POST "http://localhost:8093/api/v1/file-search/query" \
  -H "Content-Type: application/json" \
  -d '{
    "fileStoreName": "fileSearchStores/my-test-store-qp0mc7qxp78s",
    "query": "Tài liệu này nói về gì?"
  }'
```

**Request Body:**
```json
{
  "fileStoreName": "fileSearchStores/my-test-store-qp0mc7qxp78s",
  "query": "Tài liệu này nói về gì?"
}
```

**Request Fields:**
- `fileStoreName` (required): Tên của File Search Store (lấy từ API list stores)
- `query` (required): Câu hỏi muốn đặt

**Response:**
```json
{
  "query": "Tài liệu này nói về gì?",
  "answer": "Tài liệu này nói về các khái niệm cơ bản trong toán học lớp 6, bao gồm số tự nhiên, phân số, hình học cơ bản...",
  "fileStoreName": "fileSearchStores/my-test-store-qp0mc7qxp78s",
  "timestamp": "2025-01-13T10:30:45"
}
```

**Response Fields:**
- `query`: Câu hỏi gốc
- `answer`: Câu trả lời từ Gemini AI dựa trên nội dung file
- `fileStoreName`: Store đã được query
- `timestamp`: Thời gian thực hiện query

---

## Flow sử dụng

### Bước 1: Tạo File Search Store và Upload File
Sử dụng demo code hoặc Gemini API trực tiếp để:
1. Tạo File Search Store mới
2. Upload các file PDF, TXT, DOC, v.v. vào store

### Bước 2: Lấy danh sách Stores
```bash
curl -X GET "http://localhost:8093/api/v1/file-search/stores"
```

Lưu lại `name` của store muốn sử dụng (ví dụ: `fileSearchStores/my-test-store-qp0mc7qxp78s`)

### Bước 3: Query với File Search
```bash
curl -X POST "http://localhost:8093/api/v1/file-search/query" \
  -H "Content-Type: application/json" \
  -d '{
    "fileStoreName": "fileSearchStores/my-test-store-qp0mc7qxp78s",
    "query": "Hãy tóm tắt nội dung chính của tài liệu"
  }'
```

---

## Ví dụ sử dụng

### Ví dụ 1: Hỏi về nội dung tài liệu
```bash
POST /api/v1/file-search/query
{
  "fileStoreName": "fileSearchStores/math-docs-store",
  "query": "Chương 1 nói về gì?"
}
```

### Ví dụ 2: Tìm kiếm thông tin cụ thể
```bash
POST /api/v1/file-search/query
{
  "fileStoreName": "fileSearchStores/math-docs-store",
  "query": "Công thức tính diện tích hình chữ nhật là gì?"
}
```

### Ví dụ 3: So sánh thông tin
```bash
POST /api/v1/file-search/query
{
  "fileStoreName": "fileSearchStores/math-docs-store",
  "query": "So sánh phân số và số thập phân"
}
```

---

## Lưu ý

1. **API Key**: Đảm bảo đã cấu hình đúng Gemini API Key
2. **Store Name**: Phải sử dụng đúng format `fileSearchStores/store-id`
3. **Rate Limit**: Gemini API có giới hạn số lượng request
4. **File Format**: Gemini File Search hỗ trợ nhiều định dạng: PDF, TXT, DOC, DOCX, v.v.
5. **Processing Time**: File upload cần thời gian xử lý trước khi có thể query

---

## Swagger UI

Truy cập Swagger UI để test API:
```
http://localhost:8093/swagger-ui.html
```

---

## Error Handling

API sẽ trả về lỗi trong các trường hợp:
- Store không tồn tại
- API Key không hợp lệ
- Query quá dài
- Gemini API rate limit (429)

### Xử lý lỗi 429 - Quota Exceeded

Nếu gặp lỗi 429, có các cách xử lý:

1. **Đợi và retry:** API sẽ trả về thời gian cần đợi trong response (thường ~40 giây)
2. **Thay đổi model:** Sử dụng model khác có quota riêng:
   ```yaml
   gemini:
     file-search:
       model: gemini-2.5-pro  # Thay vì gemini-2.5-flash
   ```
3. **Upgrade API Key:** Nâng cấp lên paid tier để có quota cao hơn
4. **Sử dụng nhiều API Key:** Rotate giữa các API keys khác nhau

Ví dụ error response:
```json
{
  "timestamp": "2025-01-13T10:30:45",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to query file search: HTTP Error 429: Quota exceeded...",
  "path": "/api/v1/file-search/query"
}
```

**Giải pháp nhanh:** Restart service và đợi vài phút trước khi test lại.

---

## Performance Tips

1. **Cache results:** Lưu kết quả query để tránh gọi API nhiều lần
2. **Batch queries:** Gộp nhiều câu hỏi liên quan thành một query
3. **Use smaller stores:** Chia nhỏ file stores theo chủ đề để tăng độ chính xác
4. **Monitor usage:** Theo dõi quota tại https://ai.dev/usage?tab=rate-limit



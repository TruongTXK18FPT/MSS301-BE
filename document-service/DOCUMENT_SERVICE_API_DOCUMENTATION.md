# Document Service API Documentation

## Tổng quan

Document Service là service quản lý việc upload, xử lý và truy xuất tài liệu PDF trong hệ thống. Service này hỗ trợ:
- Upload file PDF
- Xử lý tự động (chunking, OCR, embedding)
- Quản lý cấu trúc tài liệu (chapters, lessons)
- Tìm kiếm và truy xuất chunks
- Theo dõi trạng thái xử lý

---

## Base URL

Tất cả các request đều được gửi thông qua **Gateway Service**:

```
Base URL: http://localhost:8080/api/v1/document
```

**Lưu ý quan trọng:**
- Gateway chạy ở port `8080`
- Document Service chạy ở port `8091` (nhưng FE không cần biết)
- Gateway prefix: `/api/v1/document` (không có 's')
- Gateway sẽ tự động rewrite `/api/v1/document/*` thành `/api/v1/documents/*` khi route đến Document Service
- **FE chỉ cần gọi: `http://localhost:8080/api/v1/document/...`**
- **KHÔNG sử dụng `/api/v1/documents` (có 's') trong URL**

---

## Response Format

Tất cả các API đều trả về response theo format chuẩn:

```typescript
interface ApiResponse<T> {
  success: boolean;      // true nếu thành công, false nếu có lỗi
  message?: string;      // Message mô tả (optional)
  data: T;              // Dữ liệu trả về
}
```

### Success Response Example:
```json
{
  "success": true,
  "message": "PDF uploaded successfully",
  "data": {
    "id": "doc-123",
    "title": "Chapter 1",
    ...
  }
}
```

### Error Response Example:
```json
{
  "success": false,
  "message": "File size exceeds maximum limit",
  "data": null
}
```

---

## Common Schemas

### 1. DocumentResponseDto
```typescript
interface DocumentResponseDto {
  id: string;                          // Document ID
  title: string;                       // Tên tài liệu
  filename: string;                    // Tên file gốc
  status: DocumentStatus;              // Trạng thái: UPLOADED, PROCESSING, COMPLETED, FAILED, DELETED
  uploadedAt: string;                  // ISO 8601 datetime
  processedAt: string | null;          // ISO 8601 datetime (null nếu chưa xử lý xong)
  size: number;                        // Kích thước file (bytes)
  language: string;                    // Ngôn ngữ: "VIETNAMESE", "ENGLISH", etc.
  totalPages: number | null;           // Tổng số trang
  description: string | null;          // Mô tả tài liệu
  processingJob?: ProcessingJobDto;    // Thông tin job xử lý (optional)
}

enum DocumentStatus {
  UPLOADED = "UPLOADED",       // Đã upload, chưa xử lý
  PROCESSING = "PROCESSING",   // Đang xử lý
  COMPLETED = "COMPLETED",     // Đã xử lý xong
  FAILED = "FAILED",          // Xử lý thất bại
  DELETED = "DELETED"         // Đã xóa
}
```

### 2. ProcessingJobDto
```typescript
interface ProcessingJobDto {
  id: string;                          // Job ID
  status: JobStatus;                   // Trạng thái job
  currentStep: string;                 // Bước hiện tại: "EXTRACTING", "CHUNKING", "EMBEDDING"
  progress: number;                    // Tiến độ (0-100)
  startedAt: string;                   // ISO 8601 datetime
  completedAt: string | null;          // ISO 8601 datetime (null nếu chưa xong)
  errorMessage: string | null;         // Thông báo lỗi nếu có
  usedOcr: boolean;                    // Có sử dụng OCR không
  processingTimeMs: number | null;     // Thời gian xử lý (milliseconds)
}

enum JobStatus {
  PENDING = "PENDING",         // Đang chờ
  RUNNING = "RUNNING",         // Đang chạy
  COMPLETED = "COMPLETED",     // Hoàn thành
  FAILED = "FAILED",          // Thất bại
  CANCELLED = "CANCELLED"     // Đã hủy
}
```

### 3. ChunkDto
```typescript
interface ChunkDto {
  id: string;                          // Chunk ID
  chunkIndex: number;                  // Vị trí chunk (0-based)
  content: string;                     // Nội dung text
  pageNumber: number | null;           // Số trang
  chapterNumber: number | null;        // Số chapter
  chapterTitle: string | null;         // Tên chapter
  lessonNumber: number | null;         // Số lesson
  lessonTitle: string | null;          // Tên lesson
  lessonId: string | null;             // Lesson ID (dùng để link với content-service)
  tokenCount: number | null;           // Số token
  fromOcr: boolean | null;             // Có phải từ OCR không
  confidence: number | null;           // Độ tin cậy OCR (0-1)
  embedding: number[] | null;          // Vector embedding (thường không cần hiển thị)
}
```

### 4. DocumentStructureDto
```typescript
interface DocumentStructureDto {
  documentId: string;
  totalChunks: number;
  structure: ChapterDto[];
}

interface ChapterDto {
  number: number;                      // Số chapter
  title: string;                       // Tên chapter
  lessons: LessonDto[];                // Danh sách lessons
}

interface LessonDto {
  number: number;                      // Số lesson
  title: string;                       // Tên lesson
  id: string;                          // Lesson ID
  chunkCount: number;                  // Số lượng chunks trong lesson
}
```

### 5. PaginatedChunksDto
```typescript
interface PaginatedChunksDto {
  chunks: ChunkDto[];
  pagination: PaginationDto;
}

interface PaginationDto {
  page: number;              // Trang hiện tại (0-based)
  size: number;              // Số items trên 1 trang
  totalElements: number;     // Tổng số items
  totalPages: number;        // Tổng số trang
}
```

---

## API Endpoints

### 1. Upload Document

Upload file PDF lên hệ thống.

**Endpoint:**
```
POST /api/v1/document/upload
```

**Request:**
- Content-Type: `multipart/form-data`
- Body parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | File PDF cần upload (max 50MB) |
| title | string | No | Tên tài liệu (nếu không có sẽ dùng tên file) |
| description | string | No | Mô tả tài liệu |

**Example Request (JavaScript/Fetch):**
```javascript
const formData = new FormData();
formData.append('file', pdfFile);  // pdfFile là File object từ input
formData.append('title', 'Chapter 1: Introduction to Programming');
formData.append('description', 'First chapter of the course');

const response = await fetch('http://localhost:8080/api/v1/document/upload', {
  method: 'POST',
  body: formData,
  // Không cần set Content-Type header, browser sẽ tự động set với multipart/form-data
});

const result = await response.json();
```

**Example Request (Axios):**
```javascript
const formData = new FormData();
formData.append('file', pdfFile);
formData.append('title', 'Chapter 1: Introduction to Programming');

const response = await axios.post(
  'http://localhost:8080/api/v1/document/upload',
  formData,
  {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }
);
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "PDF uploaded successfully",
  "data": {
    "id": "674b1234567890abcdef1234",
    "title": "Chapter 1: Introduction to Programming",
    "filename": "chapter1.pdf",
    "status": "UPLOADED",
    "uploadedAt": "2025-10-23T10:30:00",
    "processedAt": null,
    "size": 2048576,
    "language": "VIETNAMESE",
    "totalPages": null,
    "description": "First chapter of the course"
  }
}
```

**Error Responses:**
- **400 Bad Request:** File không hợp lệ hoặc không phải PDF
- **500 Internal Server Error:** Lỗi server

**Notes:**
- File size tối đa: 50MB
- Chỉ chấp nhận file PDF
- Sau khi upload thành công, document có status là `UPLOADED`
- Cần gọi API trigger processing để bắt đầu xử lý

---

### 2. Trigger Document Processing

Bắt đầu xử lý document (chunking, OCR, embedding).

**Endpoint:**
```
POST /api/v1/document/{documentId}/process
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document cần xử lý |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/process`,
  {
    method: 'POST'
  }
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Processing started successfully",
  "data": {
    "id": "job-674b9876543210fedcba5678",
    "status": "PENDING",
    "currentStep": "INITIALIZING",
    "progress": 0,
    "startedAt": "2025-10-23T10:31:00",
    "completedAt": null,
    "errorMessage": null,
    "usedOcr": false,
    "processingTimeMs": null
  }
}
```

**Error Responses:**
- **400 Bad Request:** Document không tồn tại hoặc đã được xử lý
- **500 Internal Server Error:** Lỗi server

**Notes:**
- Processing là async, sẽ chạy ở background
- Sau khi trigger, document status chuyển thành `PROCESSING`
- Dùng API "Get Processing Status" để theo dõi tiến độ

---

### 3. Get All Documents

Lấy danh sách tất cả documents, có thể filter theo status.

**Endpoint:**
```
GET /api/v1/document
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | string | No | Filter theo status: UPLOADED, PROCESSING, COMPLETED, FAILED |

**Example Request:**
```javascript
// Lấy tất cả documents
const response = await fetch('http://localhost:8080/api/v1/document');

// Lấy chỉ documents đã xử lý xong
const response = await fetch('http://localhost:8080/api/v1/document?status=COMPLETED');
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "documents": [
      {
        "id": "674b1234567890abcdef1234",
        "title": "Chapter 1: Introduction",
        "filename": "chapter1.pdf",
        "status": "COMPLETED",
        "uploadedAt": "2025-10-23T10:30:00",
        "processedAt": "2025-10-23T10:35:00",
        "size": 2048576,
        "language": "VIETNAMESE",
        "totalPages": 25,
        "description": "First chapter"
      },
      {
        "id": "674b5678901234abcdef5678",
        "title": "Chapter 2: Variables",
        "filename": "chapter2.pdf",
        "status": "PROCESSING",
        "uploadedAt": "2025-10-23T11:00:00",
        "processedAt": null,
        "size": 3145728,
        "language": "VIETNAMESE",
        "totalPages": null,
        "description": "Second chapter"
      }
    ],
    "total": 2
  }
}
```

**Error Responses:**
- **400 Bad Request:** Status không hợp lệ
- **500 Internal Server Error:** Lỗi server

---

### 4. Get Document by ID

Lấy thông tin chi tiết của 1 document.

**Endpoint:**
```
GET /api/v1/document/{documentId}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}`
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "674b1234567890abcdef1234",
    "title": "Chapter 1: Introduction",
    "filename": "chapter1.pdf",
    "status": "COMPLETED",
    "uploadedAt": "2025-10-23T10:30:00",
    "processedAt": "2025-10-23T10:35:00",
    "size": 2048576,
    "language": "VIETNAMESE",
    "totalPages": 25,
    "description": "First chapter",
    "processingJob": {
      "id": "job-674b9876543210fedcba5678",
      "status": "COMPLETED",
      "currentStep": "COMPLETED",
      "progress": 100,
      "startedAt": "2025-10-23T10:31:00",
      "completedAt": "2025-10-23T10:35:00",
      "errorMessage": null,
      "usedOcr": true,
      "processingTimeMs": 240000
    }
  }
}
```

**Error Responses:**
- **404 Not Found:** Document không tồn tại
- **500 Internal Server Error:** Lỗi server

---

### 5. Get Processing Status

Theo dõi trạng thái xử lý của document.

**Endpoint:**
```
GET /api/v1/document/{documentId}/status
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/status`
);

const result = await response.json();
```

**Success Response - Có job (200):**
```json
{
  "success": true,
  "data": {
    "hasJob": true,
    "job": {
      "id": "job-674b9876543210fedcba5678",
      "status": "RUNNING",
      "currentStep": "EMBEDDING",
      "progress": 75,
      "startedAt": "2025-10-23T10:31:00",
      "completedAt": null,
      "errorMessage": null,
      "usedOcr": true,
      "processingTimeMs": null
    }
  }
}
```

**Success Response - Không có job (200):**
```json
{
  "success": true,
  "message": "No processing job found for this document",
  "data": {
    "hasJob": false,
    "job": null
  }
}
```

**Error Responses:**
- **500 Internal Server Error:** Lỗi server

**Notes:**
- Dùng API này để polling và hiển thị progress bar
- Recommended polling interval: 2-5 giây
- Khi status = "COMPLETED" hoặc "FAILED", ngừng polling

**Example Polling:**
```javascript
async function pollProcessingStatus(documentId) {
  const intervalId = setInterval(async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/document/${documentId}/status`
      );
      const result = await response.json();
      
      if (result.data.hasJob) {
        const job = result.data.job;
        
        // Update UI với progress
        updateProgressBar(job.progress);
        updateCurrentStep(job.currentStep);
        
        // Nếu xong hoặc failed, ngừng polling
        if (job.status === 'COMPLETED' || job.status === 'FAILED') {
          clearInterval(intervalId);
          
          if (job.status === 'COMPLETED') {
            showSuccess('Document processed successfully!');
          } else {
            showError(`Processing failed: ${job.errorMessage}`);
          }
        }
      }
    } catch (error) {
      console.error('Polling error:', error);
      clearInterval(intervalId);
    }
  }, 3000); // Poll mỗi 3 giây
  
  return intervalId;
}
```

---

### 6. Get Document Structure

Lấy cấu trúc phân cấp của document (chapters → lessons → chunks).

**Endpoint:**
```
GET /api/v1/document/{documentId}/structure
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/structure`
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "documentId": "674b1234567890abcdef1234",
    "totalChunks": 150,
    "structure": [
      {
        "number": 1,
        "title": "Introduction to Programming",
        "lessons": [
          {
            "number": 1,
            "title": "What is Programming?",
            "id": "lesson-001",
            "chunkCount": 15
          },
          {
            "number": 2,
            "title": "Programming Languages",
            "id": "lesson-002",
            "chunkCount": 20
          }
        ]
      },
      {
        "number": 2,
        "title": "Variables and Data Types",
        "lessons": [
          {
            "number": 1,
            "title": "Introduction to Variables",
            "id": "lesson-003",
            "chunkCount": 18
          }
        ]
      }
    ]
  }
}
```

**Error Responses:**
- **500 Internal Server Error:** Lỗi server

**Use Cases:**
- Hiển thị table of contents
- Navigation tree
- Course structure overview

**Example UI Rendering:**
```javascript
function renderStructure(structure) {
  return structure.structure.map(chapter => (
    <div key={chapter.number}>
      <h2>Chapter {chapter.number}: {chapter.title}</h2>
      <ul>
        {chapter.lessons.map(lesson => (
          <li key={lesson.id}>
            Lesson {lesson.number}: {lesson.title} 
            ({lesson.chunkCount} chunks)
          </li>
        ))}
      </ul>
    </div>
  ));
}
```

---

### 7. Get Document Chunks

Lấy danh sách chunks của document, có thể filter theo chapter/lesson và pagination.

**Endpoint:**
```
GET /api/v1/document/{documentId}/chunks
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| chapter | number | No | - | Filter theo chapter number |
| lesson | number | No | - | Filter theo lesson number (cần có chapter) |
| page | number | No | 0 | Page number (0-based) |
| size | number | No | 20 | Số chunks trên 1 page |

**Example Requests:**
```javascript
// Lấy tất cả chunks (page 0, size 20)
const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/chunks`
);

// Lấy chunks của chapter 1
const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/chunks?chapter=1`
);

// Lấy chunks của chapter 1, lesson 2
const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/chunks?chapter=1&lesson=2`
);

// Pagination - page 2, size 50
const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/chunks?page=2&size=50`
);
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "chunks": [
      {
        "id": "chunk-001",
        "chunkIndex": 0,
        "content": "Programming is the process of creating a set of instructions...",
        "pageNumber": 1,
        "chapterNumber": 1,
        "chapterTitle": "Introduction to Programming",
        "lessonNumber": 1,
        "lessonTitle": "What is Programming?",
        "lessonId": "lesson-001",
        "tokenCount": 150,
        "fromOcr": false,
        "confidence": null,
        "embedding": null
      },
      {
        "id": "chunk-002",
        "chunkIndex": 1,
        "content": "There are many programming languages available today...",
        "pageNumber": 1,
        "chapterNumber": 1,
        "chapterTitle": "Introduction to Programming",
        "lessonNumber": 1,
        "lessonTitle": "What is Programming?",
        "lessonId": "lesson-001",
        "tokenCount": 145,
        "fromOcr": false,
        "confidence": null,
        "embedding": null
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

**Error Responses:**
- **500 Internal Server Error:** Lỗi server

**Use Cases:**
- Hiển thị nội dung document
- Reading view với pagination
- Filter theo chapter/lesson để xem nội dung cụ thể

---

### 8. Get Chunk by ID

Lấy thông tin chi tiết của 1 chunk cụ thể.

**Endpoint:**
```
GET /api/v1/document/chunks/{chunkId}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| chunkId | string | Yes | ID của chunk |

**Example Request:**
```javascript
const chunkId = "chunk-001";

const response = await fetch(
  `http://localhost:8080/api/v1/document/chunks/${chunkId}`
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "chunk-001",
    "chunkIndex": 0,
    "content": "Programming is the process of creating a set of instructions...",
    "pageNumber": 1,
    "chapterNumber": 1,
    "chapterTitle": "Introduction to Programming",
    "lessonNumber": 1,
    "lessonTitle": "What is Programming?",
    "lessonId": "lesson-001",
    "tokenCount": 150,
    "fromOcr": false,
    "confidence": null,
    "embedding": [0.123, -0.456, 0.789, ...]
  }
}
```

**Error Responses:**
- **404 Not Found:** Chunk không tồn tại
- **500 Internal Server Error:** Lỗi server

---

### 9. Search Chunks

Tìm kiếm chunks trong document theo keyword.

**Endpoint:**
```
GET /api/v1/document/{documentId}/chunks/search
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Keyword tìm kiếm |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";
const keyword = "variable";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/chunks/search?q=${encodeURIComponent(keyword)}`
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "query": "variable",
    "results": [
      {
        "id": "chunk-015",
        "chunkIndex": 14,
        "content": "A variable is a container for storing data values...",
        "pageNumber": 5,
        "chapterNumber": 2,
        "chapterTitle": "Variables and Data Types",
        "lessonNumber": 1,
        "lessonTitle": "Introduction to Variables",
        "lessonId": "lesson-003",
        "tokenCount": 142,
        "fromOcr": false,
        "confidence": null,
        "embedding": null
      }
    ],
    "totalResults": 15
  }
}
```

**Error Responses:**
- **500 Internal Server Error:** Lỗi server

**Notes:**
- Search là case-insensitive
- Tìm kiếm simple text match (contains)
- Không dùng embedding/semantic search

---

### 10. Delete Document

Xóa document và tất cả dữ liệu liên quan.

**Endpoint:**
```
DELETE /api/v1/document/{documentId}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document cần xóa |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}`,
  {
    method: 'DELETE'
  }
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Document deleted successfully",
  "data": null
}
```

**Error Responses:**
- **404 Not Found:** Document không tồn tại
- **500 Internal Server Error:** Lỗi server

**Notes:**
- Xóa vĩnh viễn document
- Xóa tất cả chunks liên quan
- Xóa file vật lý trên server
- Xóa processing jobs
- Action không thể undo

---

### 11. Get Available Statuses

Lấy danh sách tất cả các document status có thể có.

**Endpoint:**
```
GET /api/v1/document/statuses
```

**Example Request:**
```javascript
const response = await fetch(
  'http://localhost:8080/api/v1/document/statuses'
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "data": [
    "UPLOADED",
    "PROCESSING",
    "COMPLETED",
    "FAILED",
    "DELETED"
  ]
}
```

**Use Cases:**
- Populate dropdown/select options
- Validation
- UI filters

---

### 12. Get Table of Contents Analysis

Phân tích và extract table of contents của document.

**Endpoint:**
```
GET /api/v1/document/{documentId}/toc-analysis
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| documentId | string | Yes | ID của document |

**Example Request:**
```javascript
const documentId = "674b1234567890abcdef1234";

const response = await fetch(
  `http://localhost:8080/api/v1/document/${documentId}/toc-analysis`
);

const result = await response.json();
```

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "documentId": "674b1234567890abcdef1234",
    "contentLength": 125000,
    "contentPreview": "Chapter 1: Introduction to Programming\n1.1 What is Programming?\nProgramming is the process..."
  }
}
```

**Error Responses:**
- **500 Internal Server Error:** Lỗi server

**Notes:**
- Content preview giới hạn 1000 ký tự
- Dùng để quick preview nội dung document

---

## Workflows / Use Cases

### Workflow 1: Upload và Process Document

```
1. User chọn file PDF
   ↓
2. FE upload file qua API: POST /api/v1/document/upload
   ↓
3. Nhận response với documentId và status = "UPLOADED"
   ↓
4. FE trigger processing: POST /api/v1/document/{documentId}/process
   ↓
5. Nhận response với job info, status = "PENDING"
   ↓
6. FE bắt đầu polling status mỗi 3 giây: 
   GET /api/v1/document/{documentId}/status
   ↓
7. Update progress bar với job.progress và job.currentStep
   ↓
8. Khi job.status = "COMPLETED", ngừng polling
   ↓
9. Hiển thị success message và navigate đến document view
```

**Example Code:**
```javascript
async function uploadAndProcessDocument(file, title, description) {
  try {
    // Step 1: Upload
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    formData.append('description', description);
    
    const uploadResponse = await fetch(
      'http://localhost:8080/api/v1/document/upload',
      {
        method: 'POST',
        body: formData
      }
    );
    
    const uploadResult = await uploadResponse.json();
    
    if (!uploadResult.success) {
      throw new Error(uploadResult.message);
    }
    
    const documentId = uploadResult.data.id;
    
    // Step 2: Trigger processing
    const processResponse = await fetch(
      `http://localhost:8080/api/v1/document/${documentId}/process`,
      {
        method: 'POST'
      }
    );
    
    const processResult = await processResponse.json();
    
    if (!processResult.success) {
      throw new Error(processResult.message);
    }
    
    // Step 3: Poll status
    return await pollProcessingStatus(documentId);
    
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
}

async function pollProcessingStatus(documentId) {
  return new Promise((resolve, reject) => {
    const intervalId = setInterval(async () => {
      try {
        const response = await fetch(
          `http://localhost:8080/api/v1/document/${documentId}/status`
        );
        const result = await response.json();
        
        if (result.data.hasJob) {
          const job = result.data.job;
          
          // Emit progress update event
          window.dispatchEvent(new CustomEvent('processing-progress', {
            detail: {
              documentId,
              progress: job.progress,
              currentStep: job.currentStep,
              status: job.status
            }
          }));
          
          if (job.status === 'COMPLETED') {
            clearInterval(intervalId);
            resolve({ success: true, documentId });
          } else if (job.status === 'FAILED') {
            clearInterval(intervalId);
            reject(new Error(job.errorMessage || 'Processing failed'));
          }
        }
      } catch (error) {
        clearInterval(intervalId);
        reject(error);
      }
    }, 3000);
  });
}
```

---

### Workflow 2: Hiển thị Document với Structure

```
1. FE navigate đến document detail page với documentId
   ↓
2. Fetch document info: GET /api/v1/document/{documentId}
   ↓
3. Fetch structure: GET /api/v1/document/{documentId}/structure
   ↓
4. Render table of contents từ structure
   ↓
5. User click vào 1 lesson
   ↓
6. Fetch chunks của lesson đó:
   GET /api/v1/document/{documentId}/chunks?chapter=X&lesson=Y
   ↓
7. Render nội dung chunks
```

**Example Code:**
```javascript
async function loadDocumentView(documentId) {
  try {
    // Load document info và structure song song
    const [docResponse, structureResponse] = await Promise.all([
      fetch(`http://localhost:8080/api/v1/document/${documentId}`),
      fetch(`http://localhost:8080/api/v1/document/${documentId}/structure`)
    ]);
    
    const docResult = await docResponse.json();
    const structureResult = await structureResponse.json();
    
    if (!docResult.success || !structureResult.success) {
      throw new Error('Failed to load document');
    }
    
    return {
      document: docResult.data,
      structure: structureResult.data
    };
    
  } catch (error) {
    console.error('Error loading document:', error);
    throw error;
  }
}

async function loadLessonContent(documentId, chapterNumber, lessonNumber) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/v1/document/${documentId}/chunks?chapter=${chapterNumber}&lesson=${lessonNumber}`
    );
    
    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message);
    }
    
    return result.data.chunks;
    
  } catch (error) {
    console.error('Error loading lesson content:', error);
    throw error;
  }
}
```

---

### Workflow 3: Search trong Document

```
1. User nhập keyword vào search box
   ↓
2. FE gọi search API với debouncing (300ms):
   GET /api/v1/document/{documentId}/chunks/search?q={keyword}
   ↓
3. Hiển thị kết quả tìm kiếm
   ↓
4. User click vào 1 kết quả
   ↓
5. Navigate đến chunk đó và highlight keyword
```

**Example Code:**
```javascript
import { debounce } from 'lodash';

const searchChunks = debounce(async (documentId, keyword) => {
  if (!keyword || keyword.length < 2) {
    return [];
  }
  
  try {
    const response = await fetch(
      `http://localhost:8080/api/v1/document/${documentId}/chunks/search?q=${encodeURIComponent(keyword)}`
    );
    
    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message);
    }
    
    return result.data.results;
    
  } catch (error) {
    console.error('Search error:', error);
    return [];
  }
}, 300);

// Usage trong component
function SearchBox({ documentId }) {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState([]);
  
  useEffect(() => {
    searchChunks(documentId, keyword).then(setResults);
  }, [documentId, keyword]);
  
  return (
    <div>
      <input 
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="Search in document..."
      />
      
      <div className="search-results">
        {results.map(chunk => (
          <div key={chunk.id} onClick={() => navigateToChunk(chunk)}>
            <strong>
              Chapter {chunk.chapterNumber}, Lesson {chunk.lessonNumber}
            </strong>
            <p>{highlightKeyword(chunk.content, keyword)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

## Error Handling

### Common Error Codes

| HTTP Code | Meaning | Common Causes |
|-----------|---------|---------------|
| 400 | Bad Request | Invalid input, wrong file type, validation errors |
| 404 | Not Found | Document/Chunk không tồn tại |
| 500 | Internal Server Error | Server error, processing error |

### Error Response Format

```json
{
  "success": false,
  "message": "Error description here",
  "data": null
}
```

### Best Practices

1. **Always check `success` field:**
```javascript
const result = await response.json();
if (!result.success) {
  // Handle error
  showError(result.message);
  return;
}
// Process data
processData(result.data);
```

2. **Handle network errors:**
```javascript
try {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  const result = await response.json();
  // ...
} catch (error) {
  if (error instanceof TypeError) {
    // Network error
    showError('Network error. Please check your connection.');
  } else {
    showError(error.message);
  }
}
```

3. **Set timeout cho requests:**
```javascript
const fetchWithTimeout = async (url, options = {}, timeout = 30000) => {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);
  
  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal
    });
    clearTimeout(id);
    return response;
  } catch (error) {
    clearTimeout(id);
    if (error.name === 'AbortError') {
      throw new Error('Request timeout');
    }
    throw error;
  }
};
```

---

## Performance Tips

### 1. Pagination
Luôn dùng pagination khi fetch chunks:
```javascript
// Good: Lấy 20 chunks
fetch(`${baseUrl}/chunks?page=0&size=20`);

// Bad: Lấy tất cả chunks (có thể hàng ngàn)
fetch(`${baseUrl}/chunks`);
```

### 2. Parallel Requests
Fetch nhiều resources song song khi có thể:
```javascript
// Good: Parallel
const [doc, structure] = await Promise.all([
  fetchDocument(id),
  fetchStructure(id)
]);

// Bad: Sequential
const doc = await fetchDocument(id);
const structure = await fetchStructure(id);
```

### 3. Caching
Cache document info và structure:
```javascript
const documentCache = new Map();

async function getDocument(id) {
  if (documentCache.has(id)) {
    return documentCache.get(id);
  }
  
  const doc = await fetchDocument(id);
  documentCache.set(id, doc);
  return doc;
}
```

### 4. Debounce Search
Luôn debounce search input:
```javascript
const debouncedSearch = debounce(searchFunction, 300);
```

### 5. Lazy Loading
Load chunks theo demand, không load tất cả cùng lúc:
```javascript
// Chỉ load khi user scroll đến hoặc click vào lesson
function onLessonClick(chapter, lesson) {
  loadLessonChunks(documentId, chapter, lesson);
}
```

---

## Testing Examples

### Postman Collection

Tạo collection với các request sau:

```
1. Upload Document
   POST http://localhost:8080/api/v1/document/upload
   Body: form-data
     - file: [chọn file PDF]
     - title: "Test Document"
   
2. Trigger Processing
   POST http://localhost:8080/api/v1/document/{{documentId}}/process
   
3. Get Status
   GET http://localhost:8080/api/v1/document/{{documentId}}/status
   
4. Get All Documents
   GET http://localhost:8080/api/v1/document
   
5. Get Document
   GET http://localhost:8080/api/v1/document/{{documentId}}
   
6. Get Structure
   GET http://localhost:8080/api/v1/document/{{documentId}}/structure
   
7. Get Chunks
   GET http://localhost:8080/api/v1/document/{{documentId}}/chunks?page=0&size=20
   
8. Search Chunks
   GET http://localhost:8080/api/v1/document/{{documentId}}/chunks/search?q=programming
```

### cURL Examples

```bash
# Upload document
curl -X POST http://localhost:8080/api/v1/document/upload \
  -F "file=@chapter1.pdf" \
  -F "title=Chapter 1" \
  -F "description=Introduction chapter"

# Trigger processing
curl -X POST http://localhost:8080/api/v1/document/674b1234567890abcdef1234/process

# Get status
curl http://localhost:8080/api/v1/document/674b1234567890abcdef1234/status

# Get all documents
curl http://localhost:8080/api/v1/document

# Get structure
curl http://localhost:8080/api/v1/document/674b1234567890abcdef1234/structure

# Search
curl "http://localhost:8080/api/v1/document/674b1234567890abcdef1234/chunks/search?q=variable"
```

---

## FAQs

### Q1: Upload file size limit là bao nhiêu?
**A:** Maximum 50MB per file.

### Q2: Có support file format nào khác ngoài PDF không?
**A:** Hiện tại chỉ support PDF. Nếu upload file khác sẽ bị reject với error 400.

### Q3: Processing document mất bao lâu?
**A:** Phụ thuộc vào:
- Kích thước file (số trang)
- Có cần OCR không (nếu scan PDF)
- Server load
- Thường: 2-5 phút cho document 20-50 trang

### Q4: Polling interval nên là bao nhiêu?
**A:** Recommend 2-5 giây. Không nên < 1 giây để tránh overload server.

### Q5: Chunk size là bao nhiêu?
**A:** Mỗi chunk khoảng 500-1000 tokens (~2000-4000 ký tự).

### Q6: Có thể xóa document đang processing không?
**A:** Có, có thể xóa bất kỳ lúc nào. Processing job sẽ tự động cancelled.

### Q7: Làm sao biết document cần OCR?
**A:** Check field `processingJob.usedOcr = true` trong response.

### Q8: Document status flow như thế nào?
**A:** 
```
UPLOADED → PROCESSING → COMPLETED
                ↓
              FAILED
```

### Q9: Có rate limiting không?
**A:** Hiện tại chưa có, nhưng recommend:
- Upload: Max 10 requests/minute
- Get requests: Max 100 requests/minute
- Search: Dùng debounce

### Q10: Làm sao handle upload progress?
**A:** Dùng XMLHttpRequest hoặc axios với onUploadProgress:
```javascript
axios.post(url, formData, {
  onUploadProgress: (progressEvent) => {
    const percentCompleted = Math.round(
      (progressEvent.loaded * 100) / progressEvent.total
    );
    updateProgressBar(percentCompleted);
  }
});
```

---

## Support & Contact

Nếu có vấn đề hoặc câu hỏi:
1. Check logs trong browser console
2. Check Network tab để xem request/response details
3. Liên hệ Backend team với:
   - Request URL
   - Request payload
   - Response nhận được
   - Error message

---

## Changelog

### Version 1.0.0 (2025-10-23)
- Initial release
- Support upload, processing, retrieval
- Full CRUD operations
- Structure and search features

---

**Document Generated:** November 10, 2025  
**API Version:** 1.0  
**Gateway URL:** http://localhost:8080  
**Service Port:** 8091 (internal, không cần dùng trực tiếp)



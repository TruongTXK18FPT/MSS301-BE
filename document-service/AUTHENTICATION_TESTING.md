# Document Service - Authentication Testing Guide

## 🚀 Quick Start

### 1. Start the services
```bash
# Start eureka-server (port 8761)
cd eureka-server
mvnw spring-boot:run

# Start auth-service (port 8080)
cd auth-service
mvnw spring-boot:run

# Start document-service (port 8091)
cd document-service
mvnw spring-boot:run
```

### 2. Get JWT Token from auth-service

#### Login Request
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

#### Response
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMjMiLCJpYXQiOjE2OTk5OTk5OTksImV4cCI6MTY5OTk5OTk5OX0.signature",
    "refreshToken": "...",
    "userId": 123,
    "email": "user@example.com"
  }
}
```

## 📝 Testing Endpoints

### ✅ Upload Document (Authenticated)

```bash
curl -X POST http://localhost:8091/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/document.pdf" \
  -F "title=My Document" \
  -F "description=Test document"
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "PDF uploaded successfully",
  "data": {
    "id": "doc-uuid-123",
    "filename": "document.pdf",
    "title": "My Document",
    "status": "UPLOADED",
    "uploadedAt": "2025-11-10T10:30:00Z"
  }
}
```

**Without Token (401 Unauthorized):**
```bash
curl -X POST http://localhost:8091/api/v1/documents/upload \
  -F "file=@/path/to/document.pdf"
```

### ✅ Get All Documents (Authenticated)

```bash
curl -X GET http://localhost:8091/api/v1/documents \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Documents retrieved successfully",
  "data": {
    "documents": [
      {
        "id": "doc-1",
        "filename": "document1.pdf",
        "status": "COMPLETED"
      }
    ],
    "total": 1
  }
}
```

### ✅ Get Document by ID (Authenticated)

```bash
curl -X GET http://localhost:8091/api/v1/documents/{documentId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Trigger Processing (Authenticated)

```bash
curl -X POST http://localhost:8091/api/v1/documents/{documentId}/process \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Get Processing Status (Authenticated)

```bash
curl -X GET http://localhost:8091/api/v1/documents/{documentId}/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Get Document Chunks (Authenticated)

```bash
# Get all chunks
curl -X GET http://localhost:8091/api/v1/documents/{documentId}/chunks \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Filter by chapter
curl -X GET "http://localhost:8091/api/v1/documents/{documentId}/chunks?chapter=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Pagination
curl -X GET "http://localhost:8091/api/v1/documents/{documentId}/chunks?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Get Document Structure (Authenticated)

```bash
curl -X GET http://localhost:8091/api/v1/documents/{documentId}/structure \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Search Chunks (Authenticated)

```bash
curl -X GET "http://localhost:8091/api/v1/documents/{documentId}/chunks/search?q=machine+learning" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Get TOC Analysis (Authenticated)

```bash
curl -X GET http://localhost:8091/api/v1/documents/{documentId}/toc-analysis \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Delete Document (Authenticated)

```bash
curl -X DELETE http://localhost:8091/api/v1/documents/{documentId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ✅ Get Available Statuses (Authenticated)

```bash
curl -X GET http://localhost:8091/api/v1/documents/statuses \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔓 Public Endpoints (No Authentication Required)

### Health Check
```bash
curl http://localhost:8091/health
```

### Swagger UI
```bash
# Open in browser
http://localhost:8091/swagger-ui.html
```

### API Documentation
```bash
curl http://localhost:8091/v3/api-docs
```

## ❌ Error Responses

### 401 Unauthorized (Missing or Invalid Token)
```json
{
  "timestamp": "2025-11-10T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/documents"
}
```

### 403 Forbidden (Valid Token but Insufficient Permissions)
```json
{
  "timestamp": "2025-11-10T10:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/documents/{documentId}"
}
```

## 🧪 Postman Collection

### Environment Variables
```
base_url: http://localhost:8091
auth_url: http://localhost:8080
jwt_token: {{accessToken}}
```

### Pre-request Script (Auto-refresh token)
```javascript
// Get token if not exists or expired
if (!pm.environment.get("accessToken") || pm.environment.get("tokenExpiry") < Date.now()) {
    pm.sendRequest({
        url: pm.environment.get("auth_url") + "/auth/login",
        method: 'POST',
        header: {
            'Content-Type': 'application/json',
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                email: "user@example.com",
                password: "password123"
            })
        }
    }, function (err, response) {
        const jsonData = response.json();
        pm.environment.set("accessToken", jsonData.data.accessToken);
        pm.environment.set("tokenExpiry", Date.now() + 3600000); // 1 hour
    });
}
```

## 🔍 Debugging

### Check JWT Token Claims
You can decode the JWT token at https://jwt.io to see:
```json
{
  "sub": "123",  // User ID
  "iat": 1699999999,  // Issued at
  "exp": 1699999999   // Expiry
}
```

### Server Logs
When a request is made, you should see logs like:
```
INFO  c.m.d.controller.DocumentManagementController - User 123 uploading document: example.pdf
INFO  c.m.d.controller.DocumentManagementController - User 123 fetching document: doc-id-456
INFO  c.m.d.controller.DocumentManagementController - User 123 deleting document: doc-id-789
```

### Common Issues

#### "User not authenticated" error
- Make sure JWT token is included in Authorization header
- Check token format: `Bearer <token>`
- Verify token is not expired

#### "Invalid user ID in token" error
- JWT token's `sub` claim must be a valid user ID (numeric)
- Check auth-service is issuing tokens correctly

#### CORS errors
- CORS is disabled in SecurityConfig
- If still having issues, check browser console for details

## 🎯 Integration Testing Script

```bash
#!/bin/bash

# 1. Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

echo "Token: $TOKEN"

# 2. Upload document
UPLOAD_RESPONSE=$(curl -s -X POST http://localhost:8091/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.pdf" \
  -F "title=Test Document")

DOC_ID=$(echo $UPLOAD_RESPONSE | jq -r '.data.id')
echo "Uploaded document ID: $DOC_ID"

# 3. Get document
curl -X GET "http://localhost:8091/api/v1/documents/$DOC_ID" \
  -H "Authorization: Bearer $TOKEN"

# 4. Trigger processing
curl -X POST "http://localhost:8091/api/v1/documents/$DOC_ID/process" \
  -H "Authorization: Bearer $TOKEN"

# 5. Check status
curl -X GET "http://localhost:8091/api/v1/documents/$DOC_ID/status" \
  -H "Authorization: Bearer $TOKEN"

echo "Integration test completed!"
```

## 📊 Monitoring

Check application logs for authentication-related entries:
```bash
tail -f logs/document-service.log | grep "User"
```

Example output:
```
2025-11-10 10:30:45 INFO  User 123 uploading document: textbook.pdf
2025-11-10 10:31:20 INFO  User 123 triggering processing for document: doc-uuid-123
2025-11-10 10:32:15 INFO  User 456 fetching all documents with status: COMPLETED
```


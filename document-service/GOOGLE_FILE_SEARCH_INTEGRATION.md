# Google File Search Store Integration

## Overview
This document describes the integration of Google File Search Store functionality into the document-service. When users upload PDF files, they are automatically stored in Google's File Search Store for efficient retrieval and querying.

## Features Implemented

### 1. Automatic File Search Store Creation
- When a user uploads a PDF document, a new File Search Store is automatically created in Google
- The store's display name is set to the document title (or original filename if no title provided)
- The store name is saved in the document's `googleFileSearchStoreName` field

### 2. Automatic File Upload to Google
- After local file upload, the PDF is automatically uploaded to the Google File Search Store
- Uses resumable upload protocol for reliable large file transfers
- Fails gracefully - if Google upload fails, the document is still saved locally

### 3. List All File Search Stores
- New endpoint: `GET /api/documents/google/file-search-stores`
- Returns all File Search Stores from Google
- Shows all documents that have been uploaded

### 4. Get Specific File Search Store
- New endpoint: `GET /api/documents/google/file-search-stores/{storeName}`
- Returns details of a specific File Search Store by name

### 5. Automatic Cleanup on Delete
- When a document is deleted, its associated Google File Search Store is also deleted
- Ensures no orphaned data remains in Google's system

## Configuration

Add the following to your `application.yml`:

```yaml
google:
  api:
    key: ${GOOGLE_API_KEY:your-google-api-key-here}
    base-url: https://generativelanguage.googleapis.com
```

Set the `GOOGLE_API_KEY` environment variable with your Google API key.

## API Endpoints

### List All File Search Stores
```http
GET /api/documents/google/file-search-stores
Authorization: Bearer {token}
```

**Response:**
```json
{
  "code": 200,
  "message": "Retrieved 5 File Search Stores",
  "data": [
    {
      "name": "fileSearchStores/abc123",
      "displayName": "Toán học lớp 10",
      "createTime": "2025-11-13T10:00:00Z",
      "updateTime": "2025-11-13T10:00:00Z"
    }
  ]
}
```

### Get Specific File Search Store
```http
GET /api/documents/google/file-search-stores/{storeName}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "name": "fileSearchStores/abc123",
    "displayName": "Toán học lớp 10",
    "createTime": "2025-11-13T10:00:00Z",
    "updateTime": "2025-11-13T10:00:00Z"
  }
}
```

## Architecture

### Components Created

1. **GoogleFileSearchClient** - Feign client for Google API
   - Uses OpenFeign for REST API communication
   - Handles File Search Store creation, listing, and deletion
   - Supports resumable upload protocol

2. **GoogleFileSearchService** - Service interface
   - Defines operations for File Search Store management
   - Includes upload, list, get, and delete operations

3. **GoogleFileSearchServiceImpl** - Service implementation
   - Implements resumable upload protocol
   - Handles HTTP connections for file uploads
   - Manages error handling and logging

4. **DTOs**
   - `CreateFileSearchStoreRequest` - Request to create a store
   - `FileSearchStoreResponse` - Response with store details
   - `ListFileSearchStoresResponse` - Response with list of stores
   - `InitiateUploadResponse` - Response for upload initiation

### Integration Points

1. **DocumentServiceImpl.uploadPdf()**
   - Creates File Search Store with document title
   - Uploads file to Google after local storage
   - Saves store name in document entity

2. **DocumentServiceImpl.deleteDocument()**
   - Deletes Google File Search Store before deleting local document
   - Graceful error handling if Google deletion fails

3. **DocumentManagementController**
   - New endpoints for listing and retrieving stores
   - Secured with authentication

## Usage in Query Service

For your RAG/retrieval service, you can now:

1. Get the list of all available File Search Stores (textbooks)
2. Query specific stores using Google's File Search API
3. Reference documents by their `googleFileSearchStoreName` field

Example workflow:
```java
// In your retrieval service
String storeName = document.getGoogleFileSearchStoreName();
// Use this store name to query Google's File Search API
// for RAG operations
```

## Error Handling

- If Google API key is invalid, upload fails gracefully
- Document is still saved locally even if Google upload fails
- Errors are logged but don't block the upload process
- Delete operation continues even if Google cleanup fails

## Testing

To test the functionality:

1. Set your Google API key in environment variables
2. Upload a PDF document via the existing endpoint
3. Check the response - it should include `googleFileSearchStoreName`
4. Call the list endpoint to see all stores
5. Delete the document and verify the store is removed from Google

## Future Enhancements

- Batch upload to existing stores
- Update store metadata
- Search within stores
- Store analytics and usage statistics


# Giải pháp lỗi 429 - Quota Exceeded

## Vấn đề
```
HTTP Error 429: You exceeded your current quota
Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests
```

## Nguyên nhân
- Gemini API Free Tier có giới hạn:
  - **15 requests/phút** cho model gemini-2.0-flash-exp
  - **50 requests/phút** cho model gemini-2.5-flash
  - Token limits khác nhau cho mỗi model

## Giải pháp

### 1. Thay đổi Model (KHUYẾN NGHỊ) ✅

Sửa trong `application.yml`:
```yaml
gemini:
  api-key: ${GEMINI_API_KEY:your-key}
  model: gemini-2.0-flash-exp  # Giữ nguyên cho RAG service
  file-search:
    model: gemini-2.5-flash     # Thay đổi model này
```

**Các model khả dụng cho File Search:**
- `gemini-2.5-flash` - Fast, quota cao hơn (KHUYẾN NGHỊ)
- `gemini-2.5-pro` - Chất lượng tốt nhất
- `gemini-1.5-flash` - Legacy, ổn định
- `gemini-1.5-pro` - Legacy, chất lượng tốt

### 2. Đợi và Retry

API sẽ báo cần đợi bao lâu:
```
"retryDelay": "39s"
```

Đợi khoảng 40-60 giây rồi thử lại.

### 3. Sử dụng API Key khác

Nếu có nhiều API keys, rotate chúng:
```yaml
gemini:
  api-key: ${GEMINI_API_KEY:key-backup-here}
```

### 4. Upgrade lên Paid Tier

Truy cập: https://ai.google.dev/pricing
- Paid tier có quota cao hơn nhiều
- Pay-as-you-go model

## Kiểm tra Usage

Xem usage hiện tại tại: https://ai.dev/usage?tab=rate-limit

## Test sau khi sửa

1. Restart rag-service
2. Đợi 1-2 phút
3. Test lại API:

```bash
curl -X POST "http://localhost:8093/api/v1/file-search/query" \
  -H "Content-Type: application/json" \
  -d '{
    "fileStoreName": "fileSearchStores/your-store-name",
    "query": "Test query"
  }'
```

## So sánh Models

| Model | RPM (Free) | Tốc độ | Chất lượng | Khuyến nghị |
|-------|-----------|--------|------------|-------------|
| gemini-2.5-flash | 50 | Nhanh | Tốt | ✅ Tốt nhất |
| gemini-2.5-pro | 10 | Chậm | Xuất sắc | Nếu cần quality |
| gemini-2.0-flash-exp | 15 | Nhanh | Tốt | Dễ hết quota |
| gemini-1.5-flash | 60 | Nhanh | OK | Nếu 2.5 hết quota |

## Best Practices

1. **Cache responses:** Tránh query lại cùng câu hỏi
2. **Debounce user input:** Đợi user nhập xong mới gửi
3. **Use appropriate model:** Chọn model phù hợp với use case
4. **Monitor quota:** Check usage thường xuyên
5. **Handle errors gracefully:** Show user-friendly error message

## Error Message cho User

Thay vì show raw error, hiển thị:
```
"Đã vượt quá giới hạn API. Vui lòng thử lại sau 1 phút."
```

Hoặc implement retry logic tự động với exponential backoff.


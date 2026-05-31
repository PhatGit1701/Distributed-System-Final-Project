# Báo cáo mô hình nhất quán — Distributed Stock System

## Mô hình đã chọn

**ROWA-A (Read One Write All – Available)** kết hợp **eventual consistency**, **hinted handoff** qua `recovery_log`, và **background anti-entropy** — **không** cam kết **strict linearizability**.

| Khía cạnh | Mô tả |
|-----------|--------|
| Ghi (Write) | Ghi vào mọi replica **ACTIVE** trong replica set; node DOWN được ghi nhận bằng **recovery log** (hinted handoff). |
| Đọc (Read) | Ba chế độ: **Read One** (có thể stale), **Read Latest** (max `version` trên ACTIVE), **Quorum Read** (cần ≥ R replica cùng `version`). |
| Sửa lệch khi đọc | **Read repair**: replica ACTIVE có `version` thấp hơn bản thắng được cập nhật ngay. |
| Khôi phục node | Pull/push recovery + đồng bộ nền 10s; so sánh **`version`** (logical), `updated_at` chỉ là tie-break. |
| Phiên bản | Mỗi lần ghi: `version` tăng monotonic (per SKU trên coordinator) + `write_id` (UUID). |

### Không phải linearizability

Hệ thống **cho phép stale read** (Read One), **không đồng bộ đồng thời tuyệt đối** giữa client và mọi replica, và **chỉ hội tụ dần** (eventual consistency) nhờ recovery log, read repair, và background sync.

## Replica set

- **FULL**: `NODE_1`, `NODE_2`, `NODE_3` (N = 3).
- **PARTIAL**: 2 node theo `abs(sku.hashCode()) % 3` (N = 2).

## Quorum đọc (R)

Cấu hình trong `application.properties`:

```properties
replication.quorum.read.full=2
replication.quorum.read.partial=2
```

**Quorum Read**: đọc tất cả replica ACTIVE trong set; chỉ trả kết quả nếu có ít nhất **R** bản ghi cùng **`version`**; chọn bản có `updated_at` mới nhất trong nhóm đó; sau đó **read repair** các node còn lại.

## API đọc

```
GET /api/stock/{sku}?replicationMode=FULL|PARTIAL&readMode=ONE|LATEST|QUORUM
```

## Kịch bản demo gợi ý

1. Ghi FULL → kiểm tra `version` trên 3 node.
2. Offline một node, ghi lại → recovery log + version mới.
3. Online node → recovery theo version.
4. Cố tình lệch version (ghi một node khi node kia stale) → **Read Latest** / **Quorum** + read repair hội tụ.
5. **Read One** ngay sau ghi partial → có thể thấy dữ liệu cũ (stale).

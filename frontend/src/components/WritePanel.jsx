import { useState } from 'react';
import { getReplicaSet } from '../utils/replication';

export default function WritePanel({ onLog, onSkuChange, onModeChange, onRequestTrigger }) {
  const [mode, setMode] = useState('FULL');
  const [sku, setSku] = useState('SKU-100');
  const [quantity, setQuantity] = useState('15');

  const replicaSet = getReplicaSet(sku, mode);

  const handleWrite = async () => {
    if (!sku || !quantity) {
      onLog('Thiếu thông tin SKU hoặc số lượng để ghi.', 'error');
      return;
    }

    // Trigger visualization packet animation
    onRequestTrigger?.({
      type: 'WRITE',
      sku,
      targetNodes: replicaSet,
    });

    onLog(`🚀 Đang gửi yêu cầu ghi Stock: SKU = ${sku}, Qty = ${quantity}, Mode = ${mode}...`, 'info');

    try {
      const response = await fetch(
        `/api/stock?sku=${encodeURIComponent(sku)}&quantity=${quantity}&replicationMode=${mode}`,
        { method: 'POST' }
      );

      if (response.ok) {
        const text = await response.text();
        onLog(`✅ Thành công: ${text}`, 'success');
      } else {
        const errText = await response.text();
        onLog(`❌ Ghi thất bại: ${errText || 'Lỗi không xác định'}`, 'error');
      }
    } catch (err) {
      onLog(`❌ Lỗi kết nối máy chủ: ${err.message}`, 'error');
    }
  };

  return (
    <div className="op-card">
      <div className="op-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5">
          <path d="M12 5v14M5 12h14" />
        </svg>
        <span>Ghi Dữ Liệu (Write Stock)</span>
      </div>

      <div className="form-group">
        <label>Chế độ sao chép (Replication Mode)</label>
        <select className="form-control" value={mode} onChange={(e) => { setMode(e.target.value); onModeChange?.(e.target.value); }}>
          <option value="FULL">FULL REPLICATION (Tất cả Node)</option>
          <option value="PARTIAL">PARTIAL REPLICATION (2/3 Node - Modulo Hash)</option>
        </select>
      </div>

      <div className="form-group">
        <label>Mã sản phẩm (SKU ID)</label>
        <input
          type="text"
          className="form-control"
          placeholder="Ví dụ: SKU-100, APPLE, SAMSUNG"
          value={sku}
          onChange={(e) => { setSku(e.target.value); onSkuChange?.(e.target.value); }}
        />
        <div className="replica-preview-info">
          <span>Target Replica Set:</span>
          <span className="nodes-list">{replicaSet.join(', ')}</span>
        </div>
      </div>

      <div className="form-group">
        <label>Số lượng (Quantity)</label>
        <input
          type="number"
          className="form-control"
          placeholder="Số lượng sản phẩm"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
      </div>

      <button className="btn-action" onClick={handleWrite}>Thực Hiện Ghi (ROWA-A Write)</button>
    </div>
  );
}

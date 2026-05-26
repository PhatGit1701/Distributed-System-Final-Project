import { useState } from 'react';
import { getReplicaSet } from '../utils/replication';

export default function ReadPanel({ onLog }) {
  const [mode, setMode] = useState('FULL');
  const [sku, setSku] = useState('SKU-100');
  const [result, setResult] = useState(null);

  const replicaSet = getReplicaSet(sku, mode);

  const handleRead = async () => {
    if (!sku) {
      onLog('Thiếu mã SKU để đọc.', 'error');
      return;
    }

    onLog(`🔍 Đang gửi yêu cầu đọc Stock: SKU = ${sku}, Mode = ${mode}...`, 'info');

    try {
      const response = await fetch(`/api/stock/${encodeURIComponent(sku)}?replicationMode=${mode}`);

      if (response.ok) {
        const data = await response.json();
        onLog(`✅ Đọc thành công! SKU = ${data.sku}, Qty = ${data.quantity}, Served by = ${data.servedByNode}`, 'success');
        setResult(data);
      } else {
        const errText = await response.text();
        onLog(`❌ Đọc thất bại: ${errText || 'Lỗi không xác định'}`, 'error');
        setResult(null);
      }
    } catch (err) {
      onLog(`❌ Lỗi kết nối máy chủ: ${err.message}`, 'error');
      setResult(null);
    }
  };

  return (
    <div className="op-card">
      <div className="op-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2.5">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.3-4.3" />
        </svg>
        <span>Đọc Dữ Liệu (Read Stock)</span>
      </div>

      <div className="form-group">
        <label>Chế độ đọc dữ liệu (Read Mode)</label>
        <select className="form-control" value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="FULL">FULL REPLICATION</option>
          <option value="PARTIAL">PARTIAL REPLICATION</option>
        </select>
      </div>

      <div className="form-group">
        <label>Mã sản phẩm cần đọc (SKU ID)</label>
        <input
          type="text"
          className="form-control"
          placeholder="Nhập SKU sản phẩm"
          value={sku}
          onChange={(e) => setSku(e.target.value)}
        />
        <div className="replica-preview-info">
          <span>Query Target Set:</span>
          <span className="nodes-list">{replicaSet.join(', ')}</span>
        </div>
      </div>

      <button className="btn-action btn-read" onClick={handleRead}>Thực Hiện Đọc (Read One)</button>

      {result && (
        <div className="result-panel">
          <div className="result-row">
            <span className="result-label">Mã SKU:</span>
            <span className="result-value">{result.sku}</span>
          </div>
          <div className="result-row">
            <span className="result-label">Số lượng trong kho:</span>
            <span className="result-value">{result.quantity}</span>
          </div>
          <div className="result-row">
            <span className="result-label">Warehouse ID:</span>
            <span className="result-value">{result.warehouseId}</span>
          </div>
          <div className="result-row">
            <span className="result-label">Phục vụ bởi Node:</span>
            <span className="result-value highlight-node">{result.servedByNode}</span>
          </div>
          <div className="result-row">
            <span className="result-label">Replica Set thiết kế:</span>
            <span className="result-value" style={{ color: '#a5b4fc' }}>
              {(result.replicaSet || []).join(', ')}
            </span>
          </div>
          <div className="result-row">
            <span className="result-label">Lưu thực tế tại các node:</span>
            <span className="result-value" style={{ color: '#34d399' }}>
              {(result.actualNodes || []).join(', ')}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}

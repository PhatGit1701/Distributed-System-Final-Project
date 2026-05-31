import { useEffect, useState } from 'react';

const READ_MODE_LABELS = {
  ONE: 'Read One — nhanh, có thể stale',
  LATEST: 'Read Latest — max version trên ACTIVE',
  QUORUM: 'Quorum Read — cần R replica cùng version',
};

export default function ReadPanel({ onLog, onRequestTrigger }) {
  const [replicationMode, setReplicationMode] = useState('FULL');
  const [readMode, setReadMode] = useState('ONE');
  const [sku, setSku] = useState('SKU-100');
  const [result, setResult] = useState(null);
  const [replicaSet, setReplicaSet] = useState(['NODE_1', 'NODE_2', 'NODE_3']);

  useEffect(() => {
    if (!sku) {
      setReplicaSet([]);
      return;
    }
    const params = new URLSearchParams({ sku, replicationMode });
    fetch(`/api/replica-set?${params}`)
      .then((r) => (r.ok ? r.json() : []))
      .then((nodes) => setReplicaSet(Array.isArray(nodes) ? nodes : []))
      .catch(() => setReplicaSet([]));
  }, [sku, replicationMode]);

  const handleRead = async () => {
    if (!sku) {
      onLog('Thiếu mã SKU để đọc.', 'error');
      return;
    }

    onLog(
      `🔍 Đọc SKU=${sku}, Replication=${replicationMode}, ReadMode=${readMode}...`,
      'info',
    );

    try {
      const params = new URLSearchParams({
        replicationMode,
        readMode,
      });
      const response = await fetch(
        `/api/stock/${encodeURIComponent(sku)}?${params}`,
      );

      if (response.ok) {
        const data = await response.json();
        const repairNote =
          data.readRepairedNodes?.length > 0
            ? ` | Read repair: ${data.readRepairedNodes.join(', ')}`
            : '';
        const staleNote = data.possiblyStale ? ' (có thể stale)' : '';
        onLog(
          `✅ Đọc [${data.readMode}] v${data.version} — Qty=${data.quantity}, Node=${data.servedByNode}${staleNote}${repairNote}`,
          'success',
        );
        setResult(data);

        onRequestTrigger?.({
          type: 'READ',
          sku,
          readMode: data.readMode,
          targetNodes: replicaSet,
          servedByNode: data.servedByNode,
          readRepairedNodes: data.readRepairedNodes || [],
        });
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

  const buttonLabel =
    readMode === 'ONE'
      ? 'Đọc (Read One)'
      : readMode === 'LATEST'
        ? 'Đọc (Read Latest)'
        : 'Đọc (Quorum Read)';

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
        <label>Chế độ nhân bản (Replication Mode)</label>
        <select
          className="form-control"
          value={replicationMode}
          onChange={(e) => setReplicationMode(e.target.value)}
        >
          <option value="FULL">FULL REPLICATION (N=3)</option>
          <option value="PARTIAL">PARTIAL REPLICATION (N=2)</option>
        </select>
      </div>

      <div className="form-group">
        <label>Chiến lược đọc (Read Mode)</label>
        <select
          className="form-control"
          value={readMode}
          onChange={(e) => setReadMode(e.target.value)}
        >
          {Object.entries(READ_MODE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
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
          <span className="nodes-list">{replicaSet.join(', ') || '—'}</span>
        </div>
      </div>

      <button type="button" className="btn-action btn-read" onClick={handleRead}>
        {buttonLabel}
      </button>

      {result && (
        <div className="result-panel">
          <div className="result-row">
            <span className="result-label">Read Mode:</span>
            <span className="result-value">{result.readMode}</span>
          </div>
          <div className="result-row">
            <span className="result-label">Version / Write ID:</span>
            <span className="result-value">
              v{result.version}
              {result.writeId ? ` · ${result.writeId.slice(0, 8)}…` : ''}
            </span>
          </div>
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
          {result.readMode === 'QUORUM' && (
            <div className="result-row">
              <span className="result-label">Quorum (R):</span>
              <span className="result-value">
                {result.quorumMet ? 'Đạt' : 'Không đạt'} — cần {result.quorumRequired}, liên hệ{' '}
                {result.replicasContacted} ACTIVE
              </span>
            </div>
          )}
          {result.possiblyStale && (
            <div className="result-row">
              <span className="result-label">Cảnh báo:</span>
              <span className="result-value" style={{ color: '#fbbf24' }}>
                Có thể stale (Read One)
              </span>
            </div>
          )}
          {(result.readRepairedNodes || []).length > 0 && (
            <div className="result-row">
              <span className="result-label">Read repair:</span>
              <span className="result-value" style={{ color: '#34d399' }}>
                {result.readRepairedNodes.join(', ')}
              </span>
            </div>
          )}
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

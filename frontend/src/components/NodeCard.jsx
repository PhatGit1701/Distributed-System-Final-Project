export default function NodeCard({ nodeId, status, stocks, logs, isReplicaTarget, onOffline, onOnline }) {
  const statusLower = (status || 'down').toLowerCase();

  return (
    <div
      className={`node-card ${statusLower}${isReplicaTarget ? ' replica-target' : ''}`}
      id={`card-${nodeId}`}
    >
      <div className="node-header">
        <span className="node-name">{nodeId.replace('_', ' ')}</span>
        <span className="status-badge">{status || 'DOWN'}</span>
      </div>

      <div className="node-db-view">
        <div className="db-section-title">📦 Stocks (stock_levels)</div>
        <ul className="db-list">
          {stocks === null ? (
            <li className="db-empty" style={{ color: 'var(--color-down)' }}>Offline (Không thể kết nối)</li>
          ) : stocks.length === 0 ? (
            <li className="db-empty">Không có sản phẩm</li>
          ) : (
            stocks.map((item, i) => (
              <li key={i} className="db-item stock-item">
                <span>{item.sku}</span>
                <span><b>{item.quantity}</b></span>
              </li>
            ))
          )}
        </ul>

        <div className="db-section-title">📋 Logs (recovery_log)</div>
        <ul className="db-list">
          {logs === null ? (
            <li className="db-empty" style={{ color: 'var(--color-down)' }}>Offline (Không thể kết nối)</li>
          ) : logs.length === 0 ? (
            <li className="db-empty">Không có nhật ký</li>
          ) : (
            logs.map((item, i) => (
              <li key={i} className="db-item log-item">
                <span style={{ fontSize: '0.75rem' }}>👉 {item.targetNode}</span>
                <span style={{ fontSize: '0.75rem' }}><b>{item.sku} ({item.quantity})</b></span>
              </li>
            ))
          )}
        </ul>
      </div>

      <div className="node-actions">
        <button className="btn-sim btn-offline" onClick={() => onOffline(nodeId)}>Offline</button>
        <button className="btn-sim btn-online" onClick={() => onOnline(nodeId)}>Online</button>
      </div>
    </div>
  );
}

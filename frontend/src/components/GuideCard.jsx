export default function GuideCard() {
  return (
    <div className="guide-card">
      <h3>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#a5b4fc" strokeWidth="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9m-13.73 13a3 3 0 0 0 5.46 0" />
        </svg>
        <span>Cách Thử Nghiệm Kịch Bản</span>
      </h3>
      <p className="guide-model-note">
        <strong>Mô hình:</strong> ROWA-A + eventual consistency + hinted handoff (<code>recovery_log</code>).
        Không đảm bảo linearizability. Mỗi lần ghi có <code>version</code> và <code>write_id</code>.
        Chi tiết: <code>REPORT.md</code>.
      </p>
      <div className="guide-steps">
        <div className="guide-step-item">
          <span className="guide-num">1</span>
          <div>
            <span className="highlight-bold">FULL Replication:</span> Ghi SKU với mode FULL — dữ liệu trên 3 node, cùng version sau mỗi lần ghi.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">2</span>
          <div>
            <span className="highlight-bold">PARTIAL Replication:</span> Ghi <code>SKU-100</code> PARTIAL — chỉ 2 node trong replica set (hash backend).
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">3</span>
          <div>
            <span className="highlight-bold">Read One:</span> Đọc nhanh từ replica đầu tiên có dữ liệu — có thể <span className="highlight-bold" style={{ color: '#fbbf24' }}>stale</span>.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">4</span>
          <div>
            <span className="highlight-bold">Read Latest:</span> So sánh <code>version</code> trên mọi ACTIVE — trả bản mới nhất, <span className="highlight-bold" style={{ color: '#34d399' }}>read repair</span> node cũ.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">5</span>
          <div>
            <span className="highlight-bold">Quorum Read (R=2):</span> Cần ≥2 replica cùng version; nếu lệch version → lỗi quorum.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">6</span>
          <div>
            <span className="highlight-bold">Node DOWN + ghi ROWA-A:</span> Ghi khi một node offline — recovery log trên node còn sống.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">7</span>
          <div>
            <span className="highlight-bold">Recovery:</span> Online node → RECOVERING → đồng bộ theo <code>version</code> từ log.
          </div>
        </div>
      </div>
    </div>
  );
}

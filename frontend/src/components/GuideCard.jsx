export default function GuideCard() {
  return (
    <div className="guide-card">
      <h3>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#a5b4fc" strokeWidth="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9m-13.73 13a3 3 0 0 0 5.46 0" />
        </svg>
        <span>Cách Thử Nghiệm Kịch Bản</span>
      </h3>
      <div className="guide-steps">
        <div className="guide-step-item">
          <span className="guide-num">1</span>
          <div>
            <span className="highlight-bold">Chạy ở chế độ FULL Replication:</span> Ghi một SKU (ví dụ: `APPLE`) với mode FULL. Bạn sẽ thấy SKU xuất hiện trên cả <span className="highlight-bold">3 Node</span>.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">2</span>
          <div>
            <span className="highlight-bold">Chạy ở chế độ PARTIAL Replication:</span> Ghi SKU `SKU-100` với mode PARTIAL. SKU sẽ chỉ được ghi vào <span className="highlight-bold">NODE_1 và NODE_2</span> (do hash % 3 == 0). NODE_3 hoàn toàn không lưu trữ!
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">3</span>
          <div>
            <span className="highlight-bold">Giả lập Node Sập (Offline):</span> Click nút <span className="highlight-bold" style={{ color: 'var(--color-down)' }}>Offline</span> tại NODE_2. Trạng thái của NODE_2 sẽ chuyển sang <span className="highlight-bold" style={{ color: 'var(--color-down)' }}>DOWN</span>.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">4</span>
          <div>
            <span className="highlight-bold">Ghi dữ liệu khi có node sập (ROWA-A):</span> Ghi lại SKU `SKU-100` khi NODE_2 đang DOWN. Hệ thống sẽ ghi thành công vào NODE_1, đồng thời sinh ra <span className="highlight-bold" style={{ color: 'var(--color-down)' }}>Recovery Log</span> tại NODE_1 để lưu vết cần khôi phục cho NODE_2.
          </div>
        </div>
        <div className="guide-step-item">
          <span className="guide-num">5</span>
          <div>
            <span className="highlight-bold">Khôi phục tự động (Recovery):</span> Click nút <span className="highlight-bold" style={{ color: 'var(--color-active)' }}>Online</span> tại NODE_2. Bạn sẽ thấy NODE_2 chuyển sang trạng thái <span className="highlight-bold" style={{ color: 'var(--color-recovering)' }}>RECOVERING</span>. Trong vòng vài giây, các bản ghi cũ từ NODE_1 sẽ tự động đồng bộ trả lại cho NODE_2, và nhật ký Recovery Log trên NODE_1 sẽ tự động biến mất! NODE_2 trở lại <span className="highlight-bold" style={{ color: 'var(--color-active)' }}>ACTIVE</span>.
          </div>
        </div>
      </div>
    </div>
  );
}

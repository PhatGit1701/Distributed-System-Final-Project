import { useEffect, useRef } from 'react';

export default function ConsoleLog({ logs, onClear }) {
  const outputRef = useRef(null);

  useEffect(() => {
    if (outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight;
    }
  }, [logs]);

  return (
    <div className="console-panel">
      <div className="console-header">
        <span>Nhật ký Giao dịch (Console Logs)</span>
        <button className="btn-clear-console" onClick={onClear}>Xoá nhật ký</button>
      </div>
      <div className="console-output" ref={outputRef}>
        {logs.map((log, index) => (
          <div className="console-line" key={index}>
            <span className="console-timestamp">{log.timestamp}</span>
            <span className={`console-text ${log.type}`}>{log.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

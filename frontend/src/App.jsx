import { useState, useEffect, useCallback, useRef } from 'react';
import Header from './components/Header';
import NodeCard from './components/NodeCard';
import WritePanel from './components/WritePanel';
import ReadPanel from './components/ReadPanel';
import GuideCard from './components/GuideCard';
import ConsoleLog from './components/ConsoleLog';
import { getReplicaSet } from './utils/replication';

const NODE_IDS = ['NODE_1', 'NODE_2', 'NODE_3'];

function App() {
  const [nodeStatuses, setNodeStatuses] = useState({
    NODE_1: 'ACTIVE',
    NODE_2: 'ACTIVE',
    NODE_3: 'ACTIVE',
  });

  const [nodeStocks, setNodeStocks] = useState({
    NODE_1: [],
    NODE_2: [],
    NODE_3: [],
  });

  const [nodeLogs, setNodeLogs] = useState({
    NODE_1: [],
    NODE_2: [],
    NODE_3: [],
  });

  const [consoleLogs, setConsoleLogs] = useState([
    {
      timestamp: '[Hệ Thống]',
      message: 'Hệ thống Visualizer đã sẵn sàng. Trạng thái Cluster được theo dõi trực tiếp.',
      type: 'info',
    },
  ]);

  // Track the current write SKU/mode for replica target highlighting
  const [writeSkuForHighlight, setWriteSkuForHighlight] = useState('SKU-100');
  const [writeModeForHighlight, setWriteModeForHighlight] = useState('FULL');

  const replicaTargets = getReplicaSet(writeSkuForHighlight, writeModeForHighlight);

  const logToConsole = useCallback((message, type = 'info') => {
    const now = new Date();
    const timestamp = `[${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}]`;
    setConsoleLogs((prev) => [...prev, { timestamp, message, type }]);
  }, []);

  const clearConsole = useCallback(() => {
    setConsoleLogs([]);
    const now = new Date();
    const timestamp = `[${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}]`;
    setConsoleLogs([{ timestamp, message: 'Đã xoá console.', type: 'info' }]);
  }, []);

  // Fetch node stocks
  const fetchNodeStocks = useCallback(async (nodeId) => {
    try {
      const res = await fetch(`/api/node/stocks/${nodeId}`);
      if (!res.ok) throw new Error();
      const stocks = await res.json();
      setNodeStocks((prev) => ({ ...prev, [nodeId]: stocks }));
    } catch {
      setNodeStocks((prev) => ({ ...prev, [nodeId]: null }));
    }
  }, []);

  // Fetch node logs
  const fetchNodeLogs = useCallback(async (nodeId) => {
    try {
      const res = await fetch(`/api/node/logs/${nodeId}`);
      if (!res.ok) throw new Error();
      const logs = await res.json();
      setNodeLogs((prev) => ({ ...prev, [nodeId]: logs }));
    } catch {
      setNodeLogs((prev) => ({ ...prev, [nodeId]: null }));
    }
  }, []);

  // Fetch cluster status
  const fetchStatus = useCallback(async () => {
    try {
      const res = await fetch('/api/status');
      const statusMap = await res.json();
      setNodeStatuses(statusMap);

      for (const nodeId of NODE_IDS) {
        fetchNodeStocks(nodeId);
        fetchNodeLogs(nodeId);
      }
    } catch {
      // Ignore silent polling errors
    }
  }, [fetchNodeStocks, fetchNodeLogs]);

  // Polling every 1.5 seconds
  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 1500);
    return () => clearInterval(interval);
  }, [fetchStatus]);

  // Node offline/online handlers
  const handleNodeOffline = useCallback(async (nodeId) => {
    try {
      const response = await fetch(`/api/node/offline/${nodeId}`, { method: 'POST' });
      const msg = await response.text();
      logToConsole(msg, 'warn');
      fetchStatus();
    } catch (err) {
      logToConsole(`Lỗi khi tắt node ${nodeId}: ${err.message}`, 'error');
    }
  }, [logToConsole, fetchStatus]);

  const handleNodeOnline = useCallback(async (nodeId) => {
    try {
      const response = await fetch(`/api/node/online/${nodeId}`, { method: 'POST' });
      const msg = await response.text();
      logToConsole(msg, 'success');
      fetchStatus();
    } catch (err) {
      logToConsole(`Lỗi khi mở lại node ${nodeId}: ${err.message}`, 'error');
    }
  }, [logToConsole, fetchStatus]);

  return (
    <>
      <div className="ambient-glow"></div>
      <div className="ambient-glow-2"></div>

      <div className="container">
        <Header />

        <div className="dashboard-grid">
          {/* Left Side: Cluster map & Operations */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>

            {/* Database Node status */}
            <div className="panel">
              <div className="panel-title">
                <span>Trạng thái Cụm Node DB (MySQL)</span>
                <span style={{ fontSize: '0.8rem', fontWeight: 'normal', color: '#9ca3af' }}>(Cập nhật tự động: 1.5s)</span>
              </div>

              <div className="nodes-container">
                {NODE_IDS.map((nodeId) => (
                  <NodeCard
                    key={nodeId}
                    nodeId={nodeId}
                    status={nodeStatuses[nodeId]}
                    stocks={nodeStocks[nodeId]}
                    logs={nodeLogs[nodeId]}
                    isReplicaTarget={replicaTargets.includes(nodeId)}
                    onOffline={handleNodeOffline}
                    onOnline={handleNodeOnline}
                  />
                ))}
              </div>
            </div>

            {/* Operations center */}
            <div className="panel">
              <div className="panel-title">Bảng Điều Khiển Giao Dịch</div>

              <div className="ops-grid">
                <WritePanel
                  onLog={logToConsole}
                  onSkuChange={setWriteSkuForHighlight}
                  onModeChange={setWriteModeForHighlight}
                />
                <ReadPanel onLog={logToConsole} />
              </div>
            </div>

          </div>

          {/* Right Side: Guide & Console */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <GuideCard />
            <ConsoleLog logs={consoleLogs} onClear={clearConsole} />
          </div>
        </div>
      </div>
    </>
  );
}

export default App;

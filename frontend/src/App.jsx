import { useState, useEffect, useCallback, useRef } from 'react';

const PACKET_ANIM_DURATION = 5000;
const PACKET_CLEANUP_DELAY = 5400;
import Header from './components/Header';
import NodeCard from './components/NodeCard';
import WritePanel from './components/WritePanel';
import ReadPanel from './components/ReadPanel';
import GuideCard from './components/GuideCard';
import ConsoleLog from './components/ConsoleLog';
import ClusterVisualization from './components/ClusterVisualization';
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

  // Active packets for visualization animation
  const [activePackets, setActivePackets] = useState([]);

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

  // Trigger packet animation on the cluster visualization
  const triggerPacketAnimation = useCallback((packetData) => {
    // packetData: { type: 'WRITE'|'READ', sku, targetNodes: [...], servedByNode?: string }
    const { type, sku, targetNodes, servedByNode } = packetData;

    if (type === 'WRITE') {
      // Spawn one packet per target node
      const newPackets = targetNodes.map((nodeId) => ({
        id: `${Date.now()}-${nodeId}-${Math.random().toString(36).slice(2, 6)}`,
        type: 'WRITE',
        targetNode: nodeId,
        label: `WR ${sku}`,
        direction: 'request',
      }));
      setActivePackets((prev) => [...prev, ...newPackets]);

      // Auto-remove after animation completes
      setTimeout(() => {
        setActivePackets((prev) =>
          prev.filter((p) => !newPackets.find((np) => np.id === p.id))
        );
      }, PACKET_CLEANUP_DELAY);
    } else if (type === 'READ') {
      const targetNode = servedByNode || targetNodes[0];
      // Outbound request packet
      const requestPacket = {
        id: `${Date.now()}-${targetNode}-req-${Math.random().toString(36).slice(2, 6)}`,
        type: 'READ',
        targetNode,
        label: `RD ${sku}`,
        direction: 'request',
      };
      setActivePackets((prev) => [...prev, requestPacket]);

      // After outbound finishes, spawn response packet
      setTimeout(() => {
        setActivePackets((prev) => prev.filter((p) => p.id !== requestPacket.id));

        const responsePacket = {
          id: `${Date.now()}-${targetNode}-res-${Math.random().toString(36).slice(2, 6)}`,
          type: 'READ',
          targetNode,
          label: `RES ${sku}`,
          direction: 'response',
        };
        setActivePackets((prev) => [...prev, responsePacket]);

        setTimeout(() => {
          setActivePackets((prev) => prev.filter((p) => p.id !== responsePacket.id));
        }, PACKET_CLEANUP_DELAY);
      }, PACKET_ANIM_DURATION);
    }
  }, []);

  // Auto-generate recovery sync packets when a node is RECOVERING
  const prevStatusesRef = useRef(nodeStatuses);
  useEffect(() => {
    const prev = prevStatusesRef.current;
    prevStatusesRef.current = nodeStatuses;

    for (const nodeId of NODE_IDS) {
      // Detect transition to RECOVERING, or sustain RECOVERING state
      if (nodeStatuses[nodeId] === 'RECOVERING') {
        // Find active nodes that will sync data to the recovering node
        const activeNodes = NODE_IDS.filter(
          (n) => n !== nodeId && nodeStatuses[n] === 'ACTIVE'
        );

        if (activeNodes.length > 0) {
          const syncPackets = activeNodes.map((sourceNode) => ({
            id: `${Date.now()}-sync-${sourceNode}-${nodeId}-${Math.random().toString(36).slice(2, 6)}`,
            type: 'SYNC',
            sourceNode,
            targetNode: nodeId,
            label: 'SYNC',
            direction: 'node-to-node',
          }));

          setActivePackets((prev) => [...prev, ...syncPackets]);

          setTimeout(() => {
            setActivePackets((prev) =>
              prev.filter((p) => !syncPackets.find((sp) => sp.id === p.id))
            );
          }, PACKET_CLEANUP_DELAY + 500);
        }
      }
    }
  }, [nodeStatuses]);

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
                  onRequestTrigger={triggerPacketAnimation}
                />
                <ReadPanel
                  onLog={logToConsole}
                  onRequestTrigger={triggerPacketAnimation}
                />
              </div>
            </div>

          </div>

          {/* Right Side: Console on top, Visualization below (aligned with transaction panel) */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <ConsoleLog logs={consoleLogs} onClear={clearConsole} />
            <ClusterVisualization
              nodeStatuses={nodeStatuses}
              activePackets={activePackets}
            />
          </div>
        </div>

        {/* Bottom: Guide card (full width, pushed below the main grid) */}
        <div className="guide-card-fullwidth">
          <GuideCard />
        </div>
      </div>
    </>
  );
}

export default App;

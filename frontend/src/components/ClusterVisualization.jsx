const ANIM_DUR = '5s';

const NODE_POSITIONS = {
  NODE_1: { x: 330, y: 60 },
  NODE_2: { x: 330, y: 160 },
  NODE_3: { x: 330, y: 260 },
};

const USER_POS = { x: 70, y: 160 };

function getStatusColor(status) {
  if (status === 'DOWN') return { main: '#ef4444', glow: 'rgba(239, 68, 68, 0.5)' };
  if (status === 'RECOVERING') return { main: '#f59e0b', glow: 'rgba(245, 158, 11, 0.5)' };
  return { main: '#10b981', glow: 'rgba(16, 185, 129, 0.4)' };
}

function getStatusLabel(status) {
  if (status === 'DOWN') return 'DOWN';
  if (status === 'RECOVERING') return 'RECOVERING';
  return 'ACTIVE';
}

function NodeSite({ nodeId, position, status }) {
  const colors = getStatusColor(status);
  const label = nodeId.replace('_', ' ');
  const isDown = status === 'DOWN';
  const isRecovering = status === 'RECOVERING';

  return (
    <g>
      {/* Outer glow ring */}
      <circle
        cx={position.x}
        cy={position.y}
        r={30}
        fill="none"
        stroke={colors.main}
        strokeWidth="1.5"
        opacity="0.3"
        className={isDown ? 'viz-pulse-red' : isRecovering ? 'viz-pulse-yellow' : 'viz-pulse-green'}
      />

      {/* Main circle */}
      <circle
        cx={position.x}
        cy={position.y}
        r={24}
        fill={isDown ? 'rgba(239, 68, 68, 0.12)' : 'rgba(17, 24, 39, 0.8)'}
        stroke={colors.main}
        strokeWidth="2"
      />

      {/* Database icon */}
      {!isDown ? (
        <g transform={`translate(${position.x - 10}, ${position.y - 12})`}>
          <ellipse cx="10" cy="4" rx="10" ry="4" fill="none" stroke={colors.main} strokeWidth="1.5" />
          <path d={`M0,4 v12 a10,4 0 0,0 20,0 v-12`} fill="none" stroke={colors.main} strokeWidth="1.5" />
          <path d={`M0,10 a10,4 0 0,0 20,0`} fill="none" stroke={colors.main} strokeWidth="1" opacity="0.5" />
        </g>
      ) : (
        <g transform={`translate(${position.x - 8}, ${position.y - 8})`}>
          <line x1="0" y1="0" x2="16" y2="16" stroke="#ef4444" strokeWidth="2.5" strokeLinecap="round" />
          <line x1="16" y1="0" x2="0" y2="16" stroke="#ef4444" strokeWidth="2.5" strokeLinecap="round" />
        </g>
      )}

      {/* Node name */}
      <text
        x={position.x}
        y={position.y + 42}
        textAnchor="middle"
        fill={colors.main}
        fontSize="11"
        fontWeight="700"
        fontFamily="'Outfit', sans-serif"
      >
        {label}
      </text>

      {/* Status badge */}
      <text
        x={position.x}
        y={position.y + 54}
        textAnchor="middle"
        fill={colors.main}
        fontSize="8"
        fontWeight="500"
        fontFamily="'Space Grotesk', monospace"
        opacity="0.7"
      >
        {getStatusLabel(status)}
      </text>
    </g>
  );
}

function UserIcon({ position }) {
  return (
    <g>
      {/* Outer glow */}
      <circle
        cx={position.x}
        cy={position.y}
        r={30}
        fill="none"
        stroke="#818cf8"
        strokeWidth="1.5"
        opacity="0.3"
        className="viz-pulse-indigo"
      />

      {/* Main circle */}
      <circle
        cx={position.x}
        cy={position.y}
        r={24}
        fill="rgba(99, 102, 241, 0.1)"
        stroke="#818cf8"
        strokeWidth="2"
      />

      {/* User icon (person silhouette) */}
      <g transform={`translate(${position.x - 9}, ${position.y - 11})`}>
        <circle cx="9" cy="5" r="4.5" fill="none" stroke="#a5b4fc" strokeWidth="1.5" />
        <path d="M0,20 Q0,12 9,12 Q18,12 18,20" fill="none" stroke="#a5b4fc" strokeWidth="1.5" />
      </g>

      {/* Label */}
      <text
        x={position.x}
        y={position.y + 42}
        textAnchor="middle"
        fill="#a5b4fc"
        fontSize="11"
        fontWeight="700"
        fontFamily="'Outfit', sans-serif"
      >
        CLIENT
      </text>
    </g>
  );
}

function getNodeToNodePath(sourceNode, targetNode, nodePositions) {
  const sourcePos = nodePositions[sourceNode];
  const targetPos = nodePositions[targetNode];
  if (!sourcePos || !targetPos) return '';

  const isDownward = sourcePos.y < targetPos.y;
  
  // Calculate correct start and end Y coordinates
  const startY = isDownward ? sourcePos.y + 24 : sourcePos.y - 24;
  const endY = isDownward ? targetPos.y - 24 : targetPos.y + 24;
  const startX = sourcePos.x;
  const endX = targetPos.x;

  const isFar = Math.abs(sourcePos.y - targetPos.y) > 150; // between NODE_1 and NODE_3

  if (isFar) {
    // Curve outwards to the right to avoid passing through NODE_2
    return `M ${startX},${startY} Q 385,160 ${endX},${endY}`;
  } else {
    // Straight vertical line
    return `M ${startX},${startY} L ${endX},${endY}`;
  }
}

function InterNodeConnectionLine({ fromNode, toNode, nodePositions, nodeStatuses }) {
  const pathData = getNodeToNodePath(fromNode, toNode, nodePositions);
  if (!pathData) return null;

  const isDown = nodeStatuses[fromNode] === 'DOWN' || nodeStatuses[toNode] === 'DOWN';
  const isRecovering = nodeStatuses[fromNode] === 'RECOVERING' || nodeStatuses[toNode] === 'RECOVERING';
  
  let strokeColor = 'rgba(255, 255, 255, 0.08)';
  if (isDown) {
    strokeColor = 'rgba(239, 68, 68, 0.15)';
  } else if (isRecovering) {
    strokeColor = 'rgba(245, 158, 11, 0.2)';
  }

  return (
    <path
      d={pathData}
      fill="none"
      stroke={strokeColor}
      strokeWidth="1.5"
      strokeDasharray={isDown ? '4,4' : isRecovering ? '3,3' : 'none'}
      className="viz-connection-line"
    />
  );
}

function ConnectionLine({ fromPos, toPos, nodeId, status }) {
  const isDown = status === 'DOWN';
  const lineColor = isDown ? 'rgba(239, 68, 68, 0.25)' : 'rgba(255, 255, 255, 0.08)';

  return (
    <line
      x1={fromPos.x + 24}
      y1={fromPos.y}
      x2={toPos.x - 24}
      y2={toPos.y}
      stroke={lineColor}
      strokeWidth="1.5"
      strokeDasharray={isDown ? '4,4' : 'none'}
      className={`viz-connection-line ${isDown ? 'viz-line-down' : ''}`}
    />
  );
}

function AnimatedPacket({ packet, nodePositions, userPos }) {
  // Handle node-to-node SYNC packets separately
  if (packet.direction === 'node-to-node') return null;

  const targetPos = nodePositions[packet.targetNode];
  if (!targetPos) return null;

  const startX = userPos.x + 24;
  const startY = userPos.y;
  const endX = targetPos.x - 24;
  const endY = targetPos.y;

  const isWrite = packet.type === 'WRITE';
  const isResponse = packet.direction === 'response';
  const color = isWrite ? '#10b981' : '#3b82f6';
  const bgColor = isWrite ? 'rgba(16, 185, 129, 0.15)' : 'rgba(59, 130, 246, 0.15)';

  // For response packets, animate from node to user
  const animFromX = isResponse ? endX : startX;
  const animFromY = isResponse ? endY : startY;
  const animToX = isResponse ? startX : endX;
  const animToY = isResponse ? startY : endY;

  return (
    <g className="viz-packet-group">
      {/* Animated glowing line overlay */}
      <line
        x1={startX}
        y1={startY}
        x2={endX}
        y2={endY}
        stroke={color}
        strokeWidth="2"
        opacity="0.4"
        className="viz-line-glow-active"
      />

      {/* Moving dot */}
      <circle
        r="4"
        fill={color}
        className="viz-packet-dot"
        style={{
          filter: `drop-shadow(0 0 6px ${color})`,
        }}
      >
        <animateMotion
          dur={ANIM_DUR}
          repeatCount="1"
          fill="freeze"
          path={`M${animFromX},${animFromY} L${animToX},${animToY}`}
        />
      </circle>

      {/* Moving label */}
      <g className="viz-packet-label-group">
        <animateMotion
          dur={ANIM_DUR}
          repeatCount="1"
          fill="freeze"
          path={`M${animFromX},${animFromY} L${animToX},${animToY}`}
        />
        <rect
          x="-30"
          y="-22"
          width="60"
          height="16"
          rx="4"
          fill={bgColor}
          stroke={color}
          strokeWidth="0.8"
        />
        <text
          x="0"
          y="-11"
          textAnchor="middle"
          fill={color}
          fontSize="7"
          fontWeight="700"
          fontFamily="'Space Grotesk', monospace"
        >
          {packet.label}
        </text>
      </g>
    </g>
  );
}

function SyncPacket({ packet, nodePositions }) {
  const pathData = getNodeToNodePath(packet.sourceNode, packet.targetNode, nodePositions);
  if (!pathData) return null;

  const color = '#f59e0b';
  const bgColor = 'rgba(245, 158, 11, 0.15)';

  return (
    <g className="viz-packet-group viz-sync-group">
      {/* Glowing connection line between nodes */}
      <path
        d={pathData}
        fill="none"
        stroke={color}
        strokeWidth="2"
        opacity="0.35"
        className="viz-line-glow-active"
      />

      {/* Moving dot */}
      <circle
        r="4"
        fill={color}
        className="viz-packet-dot"
        style={{ filter: `drop-shadow(0 0 8px ${color})` }}
      >
        <animateMotion
          dur={ANIM_DUR}
          repeatCount="1"
          fill="freeze"
          path={pathData}
        />
      </circle>

      {/* Moving label */}
      <g className="viz-packet-label-group">
        <animateMotion
          dur={ANIM_DUR}
          repeatCount="1"
          fill="freeze"
          path={pathData}
        />
        <rect
          x="-24"
          y="-22"
          width="48"
          height="16"
          rx="4"
          fill={bgColor}
          stroke={color}
          strokeWidth="0.8"
        />
        <text
          x="0"
          y="-11"
          textAnchor="middle"
          fill={color}
          fontSize="7"
          fontWeight="700"
          fontFamily="'Space Grotesk', monospace"
        >
          {packet.label}
        </text>
      </g>
    </g>
  );
}

export default function ClusterVisualization({ nodeStatuses, activePackets }) {
  return (
    <div className="panel viz-panel">
      <div className="panel-title">
        <span>Sơ Đồ Mạng Cluster</span>
        <span style={{ fontSize: '0.8rem', fontWeight: 'normal', color: '#9ca3af' }}>(Realtime)</span>
      </div>

      <div className="viz-svg-wrapper">
        <svg
          viewBox="0 0 420 320"
          xmlns="http://www.w3.org/2000/svg"
          className="viz-svg"
          preserveAspectRatio="xMidYMid meet"
        >
          {/* Defs for filters and gradients */}
          <defs>
            <filter id="glow-green" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
            <filter id="glow-red" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur stdDeviation="4" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>

          {/* Background grid pattern */}
          <defs>
            <pattern id="grid-pattern" width="20" height="20" patternUnits="userSpaceOnUse">
              <path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(255,255,255,0.02)" strokeWidth="0.5" />
            </pattern>
          </defs>
          <rect width="420" height="320" fill="url(#grid-pattern)" />

          {/* Connection lines (draw first, behind everything) */}
          {['NODE_1', 'NODE_2', 'NODE_3'].map((nodeId) => (
            <ConnectionLine
              key={`line-${nodeId}`}
              fromPos={USER_POS}
              toPos={NODE_POSITIONS[nodeId]}
              nodeId={nodeId}
              status={nodeStatuses[nodeId]}
            />
          ))}

          {/* Inter-node connections (links between cluster DB nodes) */}
          <InterNodeConnectionLine
            fromNode="NODE_1"
            toNode="NODE_2"
            nodePositions={NODE_POSITIONS}
            nodeStatuses={nodeStatuses}
          />
          <InterNodeConnectionLine
            fromNode="NODE_2"
            toNode="NODE_3"
            nodePositions={NODE_POSITIONS}
            nodeStatuses={nodeStatuses}
          />
          <InterNodeConnectionLine
            fromNode="NODE_1"
            toNode="NODE_3"
            nodePositions={NODE_POSITIONS}
            nodeStatuses={nodeStatuses}
          />

          {/* Animated packets (client ↔ node) */}
          {activePackets.filter((p) => p.direction !== 'node-to-node').map((packet) => (
            <AnimatedPacket
              key={packet.id}
              packet={packet}
              nodePositions={NODE_POSITIONS}
              userPos={USER_POS}
            />
          ))}

          {/* Sync packets (node ↔ node during recovery) */}
          {activePackets.filter((p) => p.direction === 'node-to-node').map((packet) => (
            <SyncPacket
              key={packet.id}
              packet={packet}
              nodePositions={NODE_POSITIONS}
            />
          ))}

          {/* User icon */}
          <UserIcon position={USER_POS} />

          {/* Node sites */}
          {['NODE_1', 'NODE_2', 'NODE_3'].map((nodeId) => (
            <NodeSite
              key={nodeId}
              nodeId={nodeId}
              position={NODE_POSITIONS[nodeId]}
              status={nodeStatuses[nodeId]}
            />
          ))}
        </svg>
      </div>
    </div>
  );
}

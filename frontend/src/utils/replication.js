// SKU consistent/modulo hashing to match backend
export function getSkuHash(sku) {
  let hash = 0;
  if (!sku || sku.trim() === '') return 0;
  for (let i = 0; i < sku.length; i++) {
    const char = sku.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash;
  }
  return Math.abs(hash);
}

export function getReplicaSet(sku, mode) {
  if (mode === 'PARTIAL') {
    if (!sku) return ['NODE_1', 'NODE_2'];
    const hash = getSkuHash(sku);
    const subset = hash % 3;
    if (subset === 0) return ['NODE_1', 'NODE_2'];
    if (subset === 1) return ['NODE_2', 'NODE_3'];
    return ['NODE_3', 'NODE_1'];
  } else {
    return ['NODE_1', 'NODE_2', 'NODE_3'];
  }
}

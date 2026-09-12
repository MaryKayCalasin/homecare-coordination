import client from './client'

export function getAuditLogs({ entityType, page = 0 } = {}) {
  const params = { page, size: 50 }
  if (entityType) params.entityType = entityType
  return client.get('/api/audit-logs', { params }).then((res) => res.data)
}

import client from './client'

export function getHandoverReports(page = 0) {
  return client.get('/api/handover-reports', { params: { page, size: 20 } }).then((res) => res.data)
}

export function generateHandoverReport({ shiftDate, shiftType, municipality }) {
  return client
    .post('/api/handover-reports/generate', null, { params: { shiftDate, shiftType, municipality } })
    .then((res) => res.data)
}

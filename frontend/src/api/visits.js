import client from './client'

export function getVisitsByDate(isoDate) {
  return client
    .get('/api/visits', { params: isoDate ? { date: isoDate } : {} })
    .then((res) => res.data)
}

export function startVisit(id) {
  return client.patch(`/api/visits/${id}/start`).then((res) => res.data)
}

export function completeVisit(id, notes) {
  return client
    .patch(`/api/visits/${id}/complete`, null, { params: notes ? { notes } : {} })
    .then((res) => res.data)
}

export function cancelVisit(id, reason) {
  return client
    .patch(`/api/visits/${id}/cancel`, null, { params: reason ? { reason } : {} })
    .then((res) => res.data)
}

export function markVisitMissed(id) {
  return client.patch(`/api/visits/${id}/missed`).then((res) => res.data)
}

import client from './client'

export function getUnresolvedUrgent() {
  return client.get('/api/observations/urgent').then((res) => res.data)
}

export function getObservationsByPatient(patientId, page = 0) {
  return client
    .get(`/api/observations/by-patient/${patientId}`, { params: { page, size: 20 } })
    .then((res) => res.data)
}

export function createObservation(payload) {
  return client.post('/api/observations', payload).then((res) => res.data)
}

export function resolveUrgentObservation(id) {
  return client.patch(`/api/observations/${id}/resolve-urgent`).then((res) => res.data)
}

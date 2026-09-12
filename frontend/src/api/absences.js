import client from './client'

export function getAllAbsences() {
  return client.get('/api/absences').then((res) => res.data)
}

export function registerAbsence(payload) {
  return client.post('/api/absences', payload).then((res) => res.data)
}

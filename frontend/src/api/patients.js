import client from './client'

export function getAllPatients() {
  return client.get('/api/patients', { params: { size: 500 } }).then((res) => res.data.content)
}

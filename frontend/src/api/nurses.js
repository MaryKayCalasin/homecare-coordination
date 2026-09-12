import client from './client'

export function getAllNurses() {
  return client.get('/api/nurses', { params: { size: 200 } }).then((res) => res.data.content)
}

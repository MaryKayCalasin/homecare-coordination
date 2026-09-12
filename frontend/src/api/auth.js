import client from './client'

export function login(username, password) {
  return client.post('/api/auth/login', { username, password }).then((res) => res.data)
}

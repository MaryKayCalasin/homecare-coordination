import axios from 'axios'

export const API_BASE_URL = 'http://localhost:8080'

const client = axios.create({
  baseURL: API_BASE_URL,
})

client.interceptors.request.use((config) => {
  const raw = localStorage.getItem('auth')
  if (raw) {
    const { token } = JSON.parse(raw)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth')
      if (!window.location.pathname.startsWith('/logg-inn')) {
        window.location.href = '/logg-inn'
      }
    }
    return Promise.reject(error)
  },
)

export default client

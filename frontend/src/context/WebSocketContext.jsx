import { Client } from '@stomp/stompjs'
import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { API_BASE_URL } from '../api/client'
import { useAuth } from './AuthContext'

const WebSocketContext = createContext(null)

export function WebSocketProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [connected, setConnected] = useState(false)
  const clientRef = useRef(null)
  const subscribersRef = useRef(new Map()) // topic -> Set<callback>
  const stompSubsRef = useRef(new Map()) // topic -> stomp subscription

  useEffect(() => {
    if (!isAuthenticated) return undefined

    const client = new Client({
      brokerURL: `${API_BASE_URL.replace(/^http/, 'ws')}/ws`,
      reconnectDelay: 4000,
      onConnect: () => {
        setConnected(true)
        for (const topic of subscribersRef.current.keys()) {
          subscribeStomp(topic)
        }
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    })

    function subscribeStomp(topic) {
      if (stompSubsRef.current.has(topic)) return
      const sub = client.subscribe(topic, (message) => {
        const payload = JSON.parse(message.body)
        for (const callback of subscribersRef.current.get(topic) ?? []) {
          callback(payload)
        }
      })
      stompSubsRef.current.set(topic, sub)
    }

    clientRef.current = { client, subscribeStomp }
    client.activate()

    return () => {
      client.deactivate()
      clientRef.current = null
      stompSubsRef.current.clear()
      setConnected(false)
    }
  }, [isAuthenticated])

  const value = useMemo(
    () => ({
      connected,
      subscribe(topic, callback) {
        if (!subscribersRef.current.has(topic)) {
          subscribersRef.current.set(topic, new Set())
        }
        subscribersRef.current.get(topic).add(callback)
        clientRef.current?.subscribeStomp(topic)

        return () => {
          subscribersRef.current.get(topic)?.delete(callback)
        }
      },
    }),
    [connected],
  )

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>
}

export function useWebSocket() {
  const ctx = useContext(WebSocketContext)
  if (!ctx) throw new Error('useWebSocket must be used within a WebSocketProvider')
  return ctx
}

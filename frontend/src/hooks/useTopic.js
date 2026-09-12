import { useEffect, useRef } from 'react'
import { useWebSocket } from '../context/WebSocketContext'

/**
 * Subscribes to a STOMP topic for the lifetime of the calling component.
 * The callback is read from a ref on every message, so callers can pass an
 * inline function that closes over current props/state without causing a
 * resubscribe (or worse, invoking a stale closure) on every render.
 */
export function useTopic(topic, onMessage) {
  const { subscribe } = useWebSocket()
  const callbackRef = useRef(onMessage)
  callbackRef.current = onMessage

  useEffect(() => {
    return subscribe(topic, (payload) => callbackRef.current(payload))
  }, [topic, subscribe])
}

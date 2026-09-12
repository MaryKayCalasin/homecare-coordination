import { useCallback, useState } from 'react'
import { useTopic } from '../hooks/useTopic'

let nextId = 1

export function NotificationCenter() {
  const [toasts, setToasts] = useState([])

  const push = useCallback((tone, text) => {
    const id = nextId++
    setToasts((prev) => [...prev, { id, tone, text }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 8000)
  }, [])

  useTopic(
    '/topic/alerts/urgent',
    useCallback(
      (msg) => push('urgent', `Akutt varsel: ${msg.patientName} – ${msg.description}`),
      [push],
    ),
  )

  useTopic(
    '/topic/absences/redistribution',
    useCallback(
      (msg) => {
        const reassigned = msg.reassignments?.length ?? 0
        const unassigned = msg.unassignedVisitIds?.length ?? 0
        const detail =
          unassigned > 0
            ? `${reassigned} besøk omfordelt, ${unassigned} står uten sykepleier`
            : `${reassigned} besøk omfordelt`
        push('info', `Fravær registrert for ${msg.absentNurseName}: ${detail}`)
      },
      [push],
    ),
  )

  useTopic(
    '/topic/handover-reports',
    useCallback(
      (msg) =>
        push(
          'info',
          `Ny vaktrapport (${msg.shiftType})${msg.unresolvedUrgentCount > 0 ? ` – ${msg.unresolvedUrgentCount} uløste akutte varsler` : ''}`,
        ),
      [push],
    ),
  )

  if (toasts.length === 0) return null

  return (
    <div className="fixed right-4 top-4 z-50 flex w-80 flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`rounded-lg border px-4 py-3 text-sm shadow-lg ${
            toast.tone === 'urgent'
              ? 'border-red-200 bg-red-50 text-red-800'
              : 'border-sky-200 bg-sky-50 text-sky-800'
          }`}
        >
          {toast.text}
        </div>
      ))}
    </div>
  )
}

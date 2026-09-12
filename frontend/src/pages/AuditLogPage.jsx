import { useCallback, useEffect, useState } from 'react'
import { getAuditLogs } from '../api/audit'
import { useAuth } from '../context/AuthContext'
import { AUDIT_ACTION_LABELS, formatDateTime } from '../utils/labels'

export function AuditLogPage() {
  const { user } = useAuth()
  const canView = user?.role === 'ADMIN' || user?.role === 'COORDINATOR'

  const [entityType, setEntityType] = useState('')
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback((filter) => {
    setLoading(true)
    setError(null)
    getAuditLogs({ entityType: filter })
      .then((page) => setLogs(page.content))
      .catch(() => setError('Klarte ikke å hente revisjonsloggen.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (canView) load(undefined)
  }, [canView, load])

  if (!canView) {
    return (
      <div className="rounded-md border border-gray-200 bg-white px-4 py-8 text-center text-sm text-gray-500">
        Du har ikke tilgang til revisjonsloggen.
      </div>
    )
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-gray-900">GDPR-revisjonslogg</h1>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            load(entityType || undefined)
          }}
          className="flex items-center gap-2"
        >
          <input
            placeholder="Filtrer på type, f.eks. Patient"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
          />
          <button
            type="submit"
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          >
            Filtrer
          </button>
        </form>
      </div>

      {error && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-gray-500">Laster …</p>
      ) : logs.length === 0 ? (
        <p className="rounded-md border border-dashed border-gray-300 px-4 py-8 text-center text-sm text-gray-500">
          Ingen loggoppføringer funnet.
        </p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
              <tr>
                <th className="px-4 py-2">Tidspunkt</th>
                <th className="px-4 py-2">Handling</th>
                <th className="px-4 py-2">Type</th>
                <th className="px-4 py-2">Objekt-ID</th>
                <th className="px-4 py-2">Utført av</th>
                <th className="px-4 py-2">IP</th>
                <th className="px-4 py-2">Detaljer</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {logs.map((log) => (
                <tr key={log.id}>
                  <td className="whitespace-nowrap px-4 py-2 text-gray-700">{formatDateTime(log.createdAt)}</td>
                  <td className="px-4 py-2 text-gray-700">{AUDIT_ACTION_LABELS[log.action] ?? log.action}</td>
                  <td className="px-4 py-2 text-gray-700">{log.entityType}</td>
                  <td className="max-w-[10rem] truncate px-4 py-2 text-xs text-gray-500" title={log.entityId}>
                    {log.entityId ?? '–'}
                  </td>
                  <td className="px-4 py-2 text-gray-700">{log.performedBy}</td>
                  <td className="px-4 py-2 text-xs text-gray-500">{log.ipAddress ?? '–'}</td>
                  <td className="px-4 py-2 text-gray-500">{log.details ?? '–'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

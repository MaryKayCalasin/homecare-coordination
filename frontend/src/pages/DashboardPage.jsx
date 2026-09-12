import { useCallback, useEffect, useState } from 'react'
import { cancelVisit, completeVisit, getVisitsByDate, markVisitMissed, startVisit } from '../api/visits'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../context/AuthContext'
import { useTopic } from '../hooks/useTopic'
import { VISIT_TYPE_LABELS, formatDateLong, formatTime, todayIsoDate } from '../utils/labels'

const STATUS_ORDER = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'MISSED', 'CANCELLED']

function addDays(isoDate, delta) {
  // Pure calendar-date arithmetic in local time - avoids the UTC round-trip
  // that toISOString() would do, which shifts the date near midnight for
  // any timezone ahead of UTC (including Europe/Oslo).
  const [year, month, day] = isoDate.split('-').map(Number)
  const date = new Date(year, month - 1, day + delta)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

export function DashboardPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'COORDINATOR'
  const [isoDate, setIsoDate] = useState(todayIsoDate)
  const [visits, setVisits] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [actingOnId, setActingOnId] = useState(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getVisitsByDate(isoDate)
      data.sort((a, b) => new Date(a.scheduledStart) - new Date(b.scheduledStart))
      setVisits(data)
    } catch {
      setError('Klarte ikke å hente besøk. Er backend-tjenesten startet?')
    } finally {
      setLoading(false)
    }
  }, [isoDate])

  useEffect(() => {
    refresh()
  }, [refresh])

  useTopic(
    '/topic/visits',
    useCallback(() => {
      refresh()
    }, [refresh]),
  )

  async function runAction(id, action) {
    setActingOnId(id)
    try {
      await action(id)
      await refresh()
    } catch {
      setError('Handlingen kunne ikke gjennomføres.')
    } finally {
      setActingOnId(null)
    }
  }

  const counts = STATUS_ORDER.reduce((acc, status) => {
    acc[status] = visits.filter((v) => v.status === status).length
    return acc
  }, {})

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Dagens besøk</h1>
          <p className="text-sm capitalize text-gray-500">
            {formatDateLong(new Date(`${isoDate}T00:00:00`))}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setIsoDate((d) => addDays(d, -1))}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          >
            ← Forrige dag
          </button>
          <button
            type="button"
            onClick={() => setIsoDate(todayIsoDate())}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          >
            I dag
          </button>
          <button
            type="button"
            onClick={() => setIsoDate((d) => addDays(d, 1))}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          >
            Neste dag →
          </button>
        </div>
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-5">
        {STATUS_ORDER.map((status) => (
          <div key={status} className="rounded-lg border border-gray-200 bg-white p-3 text-center">
            <div className="text-2xl font-semibold text-gray-900">{counts[status]}</div>
            <div className="mt-1">
              <StatusBadge status={status} />
            </div>
          </div>
        ))}
      </div>

      {error && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-gray-500">Laster besøk …</p>
      ) : visits.length === 0 ? (
        <p className="rounded-md border border-dashed border-gray-300 px-4 py-8 text-center text-sm text-gray-500">
          Ingen besøk registrert denne dagen.
        </p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
              <tr>
                <th className="px-4 py-2">Tid</th>
                <th className="px-4 py-2">Pasient</th>
                <th className="px-4 py-2">Sykepleier</th>
                <th className="px-4 py-2">Type</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2">Sted</th>
                <th className="px-4 py-2 text-right">Handling</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {visits.map((visit) => (
                <tr key={visit.id} className="align-top">
                  <td className="whitespace-nowrap px-4 py-3 text-gray-700">
                    {formatTime(visit.scheduledStart)}–{formatTime(visit.scheduledEnd)}
                  </td>
                  <td className="px-4 py-3 font-medium text-gray-900">{visit.patientName}</td>
                  <td className="px-4 py-3 text-gray-700">
                    {visit.nurseName ?? <span className="italic text-red-600">Ikke tildelt</span>}
                  </td>
                  <td className="px-4 py-3 text-gray-700">{VISIT_TYPE_LABELS[visit.visitType] ?? visit.visitType}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={visit.status} />
                  </td>
                  <td className="px-4 py-3 text-gray-500">{visit.location ?? '–'}</td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {visit.status === 'SCHEDULED' && (
                        <button
                          type="button"
                          disabled={actingOnId === visit.id}
                          onClick={() => runAction(visit.id, startVisit)}
                          className="rounded-md border border-teal-300 px-2 py-1 text-xs font-medium text-teal-700 hover:bg-teal-50 disabled:opacity-50"
                        >
                          Start
                        </button>
                      )}
                      {visit.status === 'IN_PROGRESS' && (
                        <button
                          type="button"
                          disabled={actingOnId === visit.id}
                          onClick={() => runAction(visit.id, (id) => completeVisit(id))}
                          className="rounded-md border border-emerald-300 px-2 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-50 disabled:opacity-50"
                        >
                          Fullfør
                        </button>
                      )}
                      {canManage && (visit.status === 'SCHEDULED' || visit.status === 'IN_PROGRESS') && (
                        <>
                          <button
                            type="button"
                            disabled={actingOnId === visit.id}
                            onClick={() => runAction(visit.id, markVisitMissed)}
                            className="rounded-md border border-red-300 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
                          >
                            Ikke utført
                          </button>
                          <button
                            type="button"
                            disabled={actingOnId === visit.id}
                            onClick={() => runAction(visit.id, (id) => cancelVisit(id))}
                            className="rounded-md border border-gray-300 px-2 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                          >
                            Avlys
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

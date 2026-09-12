import { useCallback, useEffect, useMemo, useState } from 'react'
import { generateHandoverReport, getHandoverReports } from '../api/handover'
import { getAllNurses } from '../api/nurses'
import { useTopic } from '../hooks/useTopic'
import { SHIFT_TYPE_LABELS, formatDateTime, todayIsoDate } from '../utils/labels'

const SHIFT_TYPES = Object.keys(SHIFT_TYPE_LABELS)

export function HandoverReportsPage() {
  const [reports, setReports] = useState([])
  const [loading, setLoading] = useState(true)
  const [municipalities, setMunicipalities] = useState([])

  const [shiftDate, setShiftDate] = useState(todayIsoDate)
  const [shiftType, setShiftType] = useState(SHIFT_TYPES[0])
  const [municipality, setMunicipality] = useState('')
  const [generating, setGenerating] = useState(false)
  const [generateError, setGenerateError] = useState(null)

  const loadReports = useCallback(() => {
    setLoading(true)
    getHandoverReports()
      .then((page) => setReports(page.content))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    loadReports()
    getAllNurses().then((nurses) => {
      const unique = [...new Set(nurses.map((n) => n.municipality))].sort()
      setMunicipalities(unique)
      setMunicipality((current) => current || unique[0] || '')
    })
  }, [loadReports])

  useTopic('/topic/handover-reports', useCallback(() => loadReports(), [loadReports]))

  const canGenerate = useMemo(() => shiftDate && shiftType && municipality, [shiftDate, shiftType, municipality])

  async function handleGenerate(event) {
    event.preventDefault()
    setGenerateError(null)
    setGenerating(true)
    try {
      await generateHandoverReport({ shiftDate, shiftType, municipality })
      loadReports()
    } catch (err) {
      setGenerateError(err.response?.data?.message ?? 'Klarte ikke å generere rapporten.')
    } finally {
      setGenerating(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <div>
        <h1 className="mb-4 text-xl font-semibold text-gray-900">Generer vaktrapport</h1>
        <p className="mb-4 text-sm text-gray-500">
          Rapporter genereres automatisk ved hver vaktovergang (07:00 / 15:00 / 23:00), men kan også
          utløses manuelt her.
        </p>
        <form
          onSubmit={handleGenerate}
          className="flex flex-col gap-4 rounded-lg border border-gray-200 bg-white p-5"
        >
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="shift-date">
                Dato
              </label>
              <input
                id="shift-date"
                type="date"
                required
                value={shiftDate}
                onChange={(e) => setShiftDate(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="shift-type">
                Vakt
              </label>
              <select
                id="shift-type"
                value={shiftType}
                onChange={(e) => setShiftType(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              >
                {SHIFT_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {SHIFT_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="shift-municipality">
              Kommune
            </label>
            <select
              id="shift-municipality"
              required
              value={municipality}
              onChange={(e) => setMunicipality(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            >
              {municipalities.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </div>

          {generateError && <p className="text-sm text-red-600">{generateError}</p>}

          <button
            type="submit"
            disabled={generating || !canGenerate}
            className="rounded-md bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700 disabled:opacity-60"
          >
            {generating ? 'Genererer …' : 'Generer rapport'}
          </button>
        </form>
      </div>

      <div>
        <h2 className="mb-4 text-xl font-semibold text-gray-900">Tidligere rapporter</h2>
        {loading ? (
          <p className="text-sm text-gray-500">Laster …</p>
        ) : reports.length === 0 ? (
          <p className="text-sm text-gray-500">Ingen vaktrapporter generert ennå.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {reports.map((r) => (
              <li key={r.id} className="rounded-lg border border-gray-200 bg-white p-4 text-sm">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-medium text-gray-900">
                    {r.municipality} – {SHIFT_TYPE_LABELS[r.shiftType] ?? r.shiftType}
                  </span>
                  <span className="text-xs text-gray-500">{formatDateTime(r.createdAt)}</span>
                </div>
                <div className="mt-1 text-xs text-gray-500">Vaktdato: {r.shiftDate}</div>
                <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-gray-600 sm:grid-cols-4">
                  <span>Fullført: {r.visitsCompletedCount}</span>
                  <span>Ikke utført: {r.visitsMissedCount}</span>
                  <span>Avlyst: {r.visitsCancelledCount}</span>
                  <span>Ventende: {r.visitsPendingCount}</span>
                  <span>Observasjoner: {r.observationsCount}</span>
                  <span>Akutte: {r.urgentObservationsCount}</span>
                  <span className={r.unresolvedUrgentCount > 0 ? 'font-medium text-red-600' : ''}>
                    Uløste akutte: {r.unresolvedUrgentCount}
                  </span>
                </div>
                {r.summary && <p className="mt-2 whitespace-pre-line text-gray-700">{r.summary}</p>}
                <div className="mt-2 text-xs text-gray-400">Generert av {r.generatedBy}</div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

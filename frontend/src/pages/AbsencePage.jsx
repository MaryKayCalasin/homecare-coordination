import { useCallback, useEffect, useState } from 'react'
import { getAllAbsences, registerAbsence } from '../api/absences'
import { getAllNurses } from '../api/nurses'
import { useTopic } from '../hooks/useTopic'
import { ABSENCE_REASON_LABELS, formatDateTime } from '../utils/labels'

const REASONS = Object.keys(ABSENCE_REASON_LABELS)

function toInstant(datetimeLocalValue) {
  return new Date(datetimeLocalValue).toISOString()
}

export function AbsencePage() {
  const [nurses, setNurses] = useState([])
  const [absences, setAbsences] = useState([])
  const [loading, setLoading] = useState(true)

  const [nurseId, setNurseId] = useState('')
  const [startDateTime, setStartDateTime] = useState('')
  const [endDateTime, setEndDateTime] = useState('')
  const [reason, setReason] = useState(REASONS[0])
  const [notes, setNotes] = useState('')

  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [waitingForNurseId, setWaitingForNurseId] = useState(null)
  const [outcome, setOutcome] = useState(null)

  const loadAbsences = useCallback(() => {
    getAllAbsences()
      .then((data) => {
        data.sort((a, b) => new Date(b.startDateTime) - new Date(a.startDateTime))
        setAbsences(data)
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    Promise.all([getAllNurses(), getAllAbsences()])
      .then(([nurseData, absenceData]) => {
        setNurses(nurseData.filter((n) => n.active))
        absenceData.sort((a, b) => new Date(b.startDateTime) - new Date(a.startDateTime))
        setAbsences(absenceData)
      })
      .finally(() => setLoading(false))
  }, [])

  useTopic(
    '/topic/absences/redistribution',
    useCallback(
      (msg) => {
        if (msg.absentNurseId === waitingForNurseId) {
          setOutcome(msg)
          setWaitingForNurseId(null)
          loadAbsences()
        }
      },
      [waitingForNurseId, loadAbsences],
    ),
  )

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitError(null)
    setOutcome(null)
    setSubmitting(true)
    try {
      const response = await registerAbsence({
        nurseId,
        startDateTime: toInstant(startDateTime),
        endDateTime: toInstant(endDateTime),
        reason,
        notes: notes || null,
      })
      setWaitingForNurseId(response.nurseId)
      setNotes('')
      loadAbsences()
    } catch (err) {
      setSubmitError(
        err.response?.data?.message ?? 'Klarte ikke å registrere fraværet. Sjekk at start er før slutt.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <div>
        <h1 className="mb-4 text-xl font-semibold text-gray-900">Meld fravær</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-lg border border-gray-200 bg-white p-5">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="nurse">
              Sykepleier
            </label>
            <select
              id="nurse"
              required
              value={nurseId}
              onChange={(e) => setNurseId(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            >
              <option value="" disabled>
                Velg sykepleier …
              </option>
              {nurses.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.fullName} ({n.municipality})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="start">
                Fra
              </label>
              <input
                id="start"
                type="datetime-local"
                required
                value={startDateTime}
                onChange={(e) => setStartDateTime(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="end">
                Til
              </label>
              <input
                id="end"
                type="datetime-local"
                required
                value={endDateTime}
                onChange={(e) => setEndDateTime(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="reason">
              Årsak
            </label>
            <select
              id="reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            >
              {REASONS.map((r) => (
                <option key={r} value={r}>
                  {ABSENCE_REASON_LABELS[r]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="notes">
              Notat (valgfritt)
            </label>
            <textarea
              id="notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            />
          </div>

          {submitError && <p className="text-sm text-red-600">{submitError}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="rounded-md bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700 disabled:opacity-60"
          >
            {submitting ? 'Registrerer …' : 'Registrer fravær'}
          </button>

          {waitingForNurseId && (
            <p className="text-sm text-gray-500">Fordeler berørte besøk til andre sykepleiere …</p>
          )}

          {outcome && (
            <div className="rounded-md border border-teal-200 bg-teal-50 px-4 py-3 text-sm text-teal-900">
              <p className="font-medium">Omfordeling fullført for {outcome.absentNurseName}</p>
              {outcome.reassignments.length > 0 && (
                <ul className="mt-2 list-inside list-disc">
                  {outcome.reassignments.map((r) => (
                    <li key={r.visitId}>Besøk omfordelt til {r.newNurseName}</li>
                  ))}
                </ul>
              )}
              {outcome.unassignedVisitIds.length > 0 && (
                <p className="mt-2 text-red-700">
                  {outcome.unassignedVisitIds.length} besøk kunne ikke omfordeles automatisk og trenger
                  manuell oppfølging.
                </p>
              )}
              {outcome.reassignments.length === 0 && outcome.unassignedVisitIds.length === 0 && (
                <p className="mt-1">Ingen berørte besøk i dette tidsrommet.</p>
              )}
            </div>
          )}
        </form>
      </div>

      <div>
        <h2 className="mb-4 text-xl font-semibold text-gray-900">Registrerte fravær</h2>
        {loading ? (
          <p className="text-sm text-gray-500">Laster …</p>
        ) : absences.length === 0 ? (
          <p className="text-sm text-gray-500">Ingen fravær registrert ennå.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {absences.map((a) => (
              <li key={a.id} className="rounded-lg border border-gray-200 bg-white p-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900">{a.nurseName}</span>
                  <span className="text-xs text-gray-500">{ABSENCE_REASON_LABELS[a.reason] ?? a.reason}</span>
                </div>
                <div className="mt-1 text-gray-600">
                  {formatDateTime(a.startDateTime)} – {formatDateTime(a.endDateTime)}
                </div>
                {a.notes && <div className="mt-1 text-gray-500">{a.notes}</div>}
                <div className="mt-1 text-xs">
                  {a.redistributed ? (
                    <span className="text-emerald-700">Omfordelt</span>
                  ) : (
                    <span className="text-gray-400">Ikke omfordelt</span>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

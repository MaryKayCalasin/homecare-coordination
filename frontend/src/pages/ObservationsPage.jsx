import { useCallback, useEffect, useState } from 'react'
import { getAllNurses } from '../api/nurses'
import {
  createObservation,
  getObservationsByPatient,
  getUnresolvedUrgent,
  resolveUrgentObservation,
} from '../api/observations'
import { getAllPatients } from '../api/patients'
import { useTopic } from '../hooks/useTopic'
import { MOOD_LEVEL_LABELS, OBSERVATION_TYPE_LABELS, formatDateTime } from '../utils/labels'

const OBSERVATION_TYPES = Object.keys(OBSERVATION_TYPE_LABELS)
const MOOD_LEVELS = Object.keys(MOOD_LEVEL_LABELS)

export function ObservationsPage() {
  const [patients, setPatients] = useState([])
  const [nurses, setNurses] = useState([])
  const [urgent, setUrgent] = useState([])
  const [loadingUrgent, setLoadingUrgent] = useState(true)
  const [resolvingId, setResolvingId] = useState(null)

  const [patientId, setPatientId] = useState('')
  const [nurseId, setNurseId] = useState('')
  const [observationType, setObservationType] = useState(OBSERVATION_TYPES[0])
  const [medicationName, setMedicationName] = useState('')
  const [medicationDosage, setMedicationDosage] = useState('')
  const [medicationGiven, setMedicationGiven] = useState(true)
  const [moodLevel, setMoodLevel] = useState(MOOD_LEVELS[2])
  const [description, setDescription] = useState('')
  const [urgentFlag, setUrgentFlag] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)

  const [journalPatientId, setJournalPatientId] = useState('')
  const [journal, setJournal] = useState([])
  const [loadingJournal, setLoadingJournal] = useState(false)

  const loadUrgent = useCallback(() => {
    setLoadingUrgent(true)
    getUnresolvedUrgent()
      .then(setUrgent)
      .catch(() => {})
      .finally(() => setLoadingUrgent(false))
  }, [])

  useEffect(() => {
    Promise.all([getAllPatients(), getAllNurses()]).then(([patientData, nurseData]) => {
      setPatients(patientData.filter((p) => p.active))
      setNurses(nurseData.filter((n) => n.active))
    })
    loadUrgent()
  }, [loadUrgent])

  useEffect(() => {
    if (!journalPatientId) {
      setJournal([])
      return
    }
    setLoadingJournal(true)
    getObservationsByPatient(journalPatientId)
      .then((page) => setJournal(page.content))
      .catch(() => {})
      .finally(() => setLoadingJournal(false))
  }, [journalPatientId])

  useTopic('/topic/alerts/urgent', useCallback(() => loadUrgent(), [loadUrgent]))

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitError(null)
    setSubmitting(true)
    try {
      await createObservation({
        patientId,
        recordedByNurseId: nurseId,
        observationType,
        medicationName: observationType === 'MEDICATION' ? medicationName : null,
        medicationDosage: observationType === 'MEDICATION' ? medicationDosage || null : null,
        medicationGiven: observationType === 'MEDICATION' ? medicationGiven : null,
        moodLevel: observationType === 'MOOD' ? moodLevel : null,
        description,
        urgent: urgentFlag,
      })
      setDescription('')
      setMedicationName('')
      setMedicationDosage('')
      setUrgentFlag(false)
      loadUrgent()
      if (journalPatientId === patientId) {
        getObservationsByPatient(patientId).then((page) => setJournal(page.content))
      }
    } catch (err) {
      setSubmitError(err.response?.data?.message ?? 'Klarte ikke å registrere observasjonen.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResolve(id) {
    setResolvingId(id)
    try {
      await resolveUrgentObservation(id)
      loadUrgent()
    } finally {
      setResolvingId(null)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <div>
        <h1 className="mb-4 text-xl font-semibold text-gray-900">Registrer observasjon</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-lg border border-gray-200 bg-white p-5">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="obs-patient">
                Pasient
              </label>
              <select
                id="obs-patient"
                required
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              >
                <option value="" disabled>
                  Velg pasient …
                </option>
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.fullName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="obs-nurse">
                Registrert av
              </label>
              <select
                id="obs-nurse"
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
                    {n.fullName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="obs-type">
              Type
            </label>
            <select
              id="obs-type"
              value={observationType}
              onChange={(e) => setObservationType(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            >
              {OBSERVATION_TYPES.map((t) => (
                <option key={t} value={t}>
                  {OBSERVATION_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </div>

          {observationType === 'MEDICATION' && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="med-name">
                  Medikament
                </label>
                <input
                  id="med-name"
                  required
                  value={medicationName}
                  onChange={(e) => setMedicationName(e.target.value)}
                  className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="med-dosage">
                  Dosering
                </label>
                <input
                  id="med-dosage"
                  value={medicationDosage}
                  onChange={(e) => setMedicationDosage(e.target.value)}
                  className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                />
              </div>
              <label className="col-span-2 flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={medicationGiven}
                  onChange={(e) => setMedicationGiven(e.target.checked)}
                  className="rounded border-gray-300 text-teal-600 focus:ring-teal-500"
                />
                Medikament ble gitt
              </label>
            </div>
          )}

          {observationType === 'MOOD' && (
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="mood">
                Humørnivå
              </label>
              <select
                id="mood"
                value={moodLevel}
                onChange={(e) => setMoodLevel(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              >
                {MOOD_LEVELS.map((m) => (
                  <option key={m} value={m}>
                    {MOOD_LEVEL_LABELS[m]}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="obs-description">
              Beskrivelse
            </label>
            <textarea
              id="obs-description"
              required
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            />
          </div>

          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={urgentFlag}
              onChange={(e) => setUrgentFlag(e.target.checked)}
              className="rounded border-gray-300 text-red-600 focus:ring-red-500"
            />
            Merk som akutt (utløser umiddelbart varsel)
          </label>

          {submitError && <p className="text-sm text-red-600">{submitError}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="rounded-md bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700 disabled:opacity-60"
          >
            {submitting ? 'Registrerer …' : 'Registrer observasjon'}
          </button>
        </form>
      </div>

      <div className="flex flex-col gap-6">
        <div>
          <h2 className="mb-4 text-xl font-semibold text-gray-900">Uløste akutte varsler</h2>
          {loadingUrgent ? (
            <p className="text-sm text-gray-500">Laster …</p>
          ) : urgent.length === 0 ? (
            <p className="text-sm text-gray-500">Ingen uløste akutte varsler.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {urgent.map((o) => (
                <li key={o.id} className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-red-900">{o.patientName}</span>
                    <span className="text-xs text-red-700">{formatDateTime(o.recordedAt)}</span>
                  </div>
                  <div className="mt-1 text-red-800">{o.description}</div>
                  <div className="mt-1 text-xs text-red-600">Registrert av {o.recordedByNurseName}</div>
                  <button
                    type="button"
                    disabled={resolvingId === o.id}
                    onClick={() => handleResolve(o.id)}
                    className="mt-2 rounded-md border border-red-300 bg-white px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
                  >
                    Marker som løst
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div>
          <h2 className="mb-4 text-xl font-semibold text-gray-900">Pasientjournal</h2>
          <select
            value={journalPatientId}
            onChange={(e) => setJournalPatientId(e.target.value)}
            className="mb-3 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
          >
            <option value="">Velg pasient for å se journal …</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>
                {p.fullName}
              </option>
            ))}
          </select>
          {loadingJournal ? (
            <p className="text-sm text-gray-500">Laster …</p>
          ) : journalPatientId && journal.length === 0 ? (
            <p className="text-sm text-gray-500">Ingen registrerte observasjoner for denne pasienten.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {journal.map((o) => (
                <li key={o.id} className="rounded-lg border border-gray-200 bg-white p-3 text-sm">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-gray-900">
                      {OBSERVATION_TYPE_LABELS[o.observationType] ?? o.observationType}
                    </span>
                    <span className="text-xs text-gray-500">{formatDateTime(o.recordedAt)}</span>
                  </div>
                  <div className="mt-1 text-gray-700">{o.description}</div>
                  {o.medicationName && (
                    <div className="mt-1 text-xs text-gray-500">
                      {o.medicationName} {o.medicationDosage ? `(${o.medicationDosage})` : ''} –{' '}
                      {o.medicationGiven ? 'gitt' : 'ikke gitt'}
                    </div>
                  )}
                  {o.moodLevel && (
                    <div className="mt-1 text-xs text-gray-500">{MOOD_LEVEL_LABELS[o.moodLevel]}</div>
                  )}
                  {o.urgent && (
                    <div className="mt-1 text-xs font-medium text-red-600">
                      {o.urgentResolved ? 'Akutt (løst)' : 'Akutt – uløst'}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}

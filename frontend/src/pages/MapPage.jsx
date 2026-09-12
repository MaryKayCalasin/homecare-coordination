import { useEffect, useMemo, useState } from 'react'
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { getAllPatients } from '../api/patients'
import { getVisitsByDate } from '../api/visits'
import { pinIcon } from '../components/MarkerIcon'
import { StatusBadge } from '../components/StatusBadge'
import { useTopic } from '../hooks/useTopic'
import { colorForNurse, UNASSIGNED_COLOR } from '../utils/colors'
import { geocodeAddress } from '../utils/geocode'
import { VISIT_TYPE_LABELS, formatTime, todayIsoDate } from '../utils/labels'

const OSLO_CENTER = [59.9139, 10.7522]

function FitBounds({ positions }) {
  const map = useMap()
  useEffect(() => {
    if (positions.length === 0) return
    if (positions.length === 1) {
      map.setView(positions[0], 13)
      return
    }
    map.fitBounds(positions, { padding: [40, 40] })
  }, [map, positions])
  return null
}

export function MapPage() {
  const [visits, setVisits] = useState([])
  const [patientsById, setPatientsById] = useState({})
  const [locations, setLocations] = useState({}) // patientId -> { lat, lon } | null
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [geocoding, setGeocoding] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [visitData, patientData] = await Promise.all([
          getVisitsByDate(todayIsoDate()),
          getAllPatients(),
        ])
        if (cancelled) return
        setVisits(visitData)
        setPatientsById(Object.fromEntries(patientData.map((p) => [p.id, p])))
      } catch {
        if (!cancelled) setError('Klarte ikke å hente dagens besøk eller pasienter.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  useTopic('/topic/visits', () => {
    getVisitsByDate(todayIsoDate()).then(setVisits).catch(() => {})
  })

  // One visit-entry per patient (the day's earliest visit), since the map shows locations, not a schedule.
  const patientVisits = useMemo(() => {
    const byPatient = new Map()
    for (const visit of visits) {
      const existing = byPatient.get(visit.patientId)
      if (!existing || new Date(visit.scheduledStart) < new Date(existing.scheduledStart)) {
        byPatient.set(visit.patientId, visit)
      }
    }
    return [...byPatient.values()]
  }, [visits])

  const nurseIds = useMemo(
    () => [...new Set(patientVisits.map((v) => v.nurseId).filter(Boolean))].sort(),
    [patientVisits],
  )
  const nurseNameById = useMemo(() => {
    const map = new Map()
    for (const v of patientVisits) {
      if (v.nurseId) map.set(v.nurseId, v.nurseName)
    }
    return map
  }, [patientVisits])

  useEffect(() => {
    if (patientVisits.length === 0) return
    let cancelled = false

    async function geocodeAll() {
      setGeocoding(true)
      for (const visit of patientVisits) {
        if (cancelled) return
        if (locations[visit.patientId] !== undefined) continue
        const patient = patientsById[visit.patientId]
        const addressQuery = patient
          ? `${patient.address}, ${patient.postalCode} ${patient.city}, Norge`
          : `${visit.location}, Norge`
        try {
          const result = await geocodeAddress(addressQuery)
          if (!cancelled) {
            setLocations((prev) => ({ ...prev, [visit.patientId]: result }))
          }
        } catch {
          if (!cancelled) {
            setLocations((prev) => ({ ...prev, [visit.patientId]: null }))
          }
        }
      }
      if (!cancelled) setGeocoding(false)
    }

    geocodeAll()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [patientVisits, patientsById])

  const markers = patientVisits
    .map((visit) => ({ visit, position: locations[visit.patientId] }))
    .filter((m) => m.position)

  const failedCount = patientVisits.filter((v) => locations[v.patientId] === null).length

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Kart over dagens besøk</h1>
          <p className="text-sm text-gray-500">Pasientadresser og tildelt sykepleier for i dag</p>
        </div>
        {geocoding && <p className="text-xs text-gray-400">Geokoder adresser …</p>}
      </div>

      {error && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">
          {error}
        </div>
      )}
      {failedCount > 0 && (
        <div className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-800">
          {failedCount} pasientadresse(r) kunne ikke plasseres på kartet.
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
        <div className="h-[65vh] overflow-hidden rounded-lg border border-gray-200 lg:col-span-3">
          <MapContainer center={OSLO_CENTER} zoom={12} className="h-full w-full">
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>-bidragsytere'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <FitBounds positions={markers.map((m) => [m.position.lat, m.position.lon])} />
            {markers.map(({ visit, position }) => (
              <Marker
                key={visit.patientId}
                position={[position.lat, position.lon]}
                icon={pinIcon(colorForNurse(visit.nurseId, nurseIds))}
              >
                <Popup>
                  <div className="text-sm">
                    <div className="font-semibold">{visit.patientName}</div>
                    <div className="text-gray-600">{visit.location}</div>
                    <div className="mt-1">
                      {visit.nurseName ? (
                        <span>Sykepleier: {visit.nurseName}</span>
                      ) : (
                        <span className="text-red-600">Ikke tildelt sykepleier</span>
                      )}
                    </div>
                    <div className="text-gray-500">
                      {formatTime(visit.scheduledStart)} · {VISIT_TYPE_LABELS[visit.visitType] ?? visit.visitType}
                    </div>
                    <div className="mt-1">
                      <StatusBadge status={visit.status} />
                    </div>
                  </div>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        <aside className="rounded-lg border border-gray-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-gray-700">Sykepleiere</h2>
          {loading ? (
            <p className="text-sm text-gray-500">Laster …</p>
          ) : nurseIds.length === 0 ? (
            <p className="text-sm text-gray-500">Ingen tildelte besøk i dag.</p>
          ) : (
            <ul className="flex flex-col gap-2 text-sm">
              {nurseIds.map((id) => (
                <li key={id} className="flex items-center gap-2">
                  <span
                    className="h-3 w-3 shrink-0 rounded-full border border-white ring-1 ring-black/10"
                    style={{ backgroundColor: colorForNurse(id, nurseIds) }}
                  />
                  <span className="text-gray-700">{nurseNameById.get(id)}</span>
                </li>
              ))}
              {patientVisits.some((v) => !v.nurseId) && (
                <li className="flex items-center gap-2">
                  <span
                    className="h-3 w-3 shrink-0 rounded-full border border-white ring-1 ring-black/10"
                    style={{ backgroundColor: UNASSIGNED_COLOR }}
                  />
                  <span className="text-gray-700">Ikke tildelt</span>
                </li>
              )}
            </ul>
          )}
        </aside>
      </div>
    </div>
  )
}

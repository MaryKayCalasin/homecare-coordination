import client from './client'

export function getAllPatients() {
  return client.get('/api/patients', { params: { size: 500 } }).then((res) => res.data.content)
}

// Caches a geocoded address on the patient record so the backend can check
// travel time between consecutive visits, not just clock overlap.
export function updatePatientCoordinates(patientId, latitude, longitude) {
  return client
    .patch(`/api/patients/${patientId}/coordinates`, null, { params: { latitude, longitude } })
    .then((res) => res.data)
}

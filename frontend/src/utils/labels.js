export const VISIT_STATUS_LABELS = {
  SCHEDULED: 'Planlagt',
  IN_PROGRESS: 'Pågår',
  COMPLETED: 'Fullført',
  CANCELLED: 'Avlyst',
  MISSED: 'Ikke utført',
}

export const VISIT_STATUS_COLORS = {
  SCHEDULED: 'bg-sky-100 text-sky-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-gray-200 text-gray-700',
  MISSED: 'bg-red-100 text-red-800',
}

export const VISIT_TYPE_LABELS = {
  MEDICATION: 'Medisinering',
  PERSONAL_CARE: 'Personlig pleie',
  HEALTH_CHECK: 'Helsesjekk',
  MEAL_ASSISTANCE: 'Måltidshjelp',
  WOUND_CARE: 'Sårstell',
  SOCIAL_VISIT: 'Sosialt besøk',
  OTHER: 'Annet',
}

export const ABSENCE_REASON_LABELS = {
  SICK_LEAVE: 'Sykemelding',
  VACATION: 'Ferie',
  TRAINING: 'Opplæring/kurs',
  PARENTAL_LEAVE: 'Foreldrepermisjon',
  PERSONAL: 'Personlige forhold',
  OTHER: 'Annet',
}

export const ROLE_LABELS = {
  ADMIN: 'Administrator',
  COORDINATOR: 'Koordinator',
  NURSE: 'Sykepleier',
}

export const OBSERVATION_TYPE_LABELS = {
  MEDICATION: 'Medisinering',
  MOOD: 'Humør',
  VITAL_SIGNS: 'Vitale tegn',
  NUTRITION: 'Ernæring',
  WOUND_CARE: 'Sårstell',
  INCIDENT: 'Hendelse',
  GENERAL: 'Generelt',
}

export const MOOD_LEVEL_LABELS = {
  VERY_POOR: 'Svært dårlig',
  POOR: 'Dårlig',
  NEUTRAL: 'Nøytral',
  GOOD: 'God',
  VERY_GOOD: 'Svært god',
}

export const SHIFT_TYPE_LABELS = {
  DAY: 'Dagvakt (07–15)',
  EVENING: 'Kveldsvakt (15–23)',
  NIGHT: 'Nattevakt (23–07)',
}

export const AUDIT_ACTION_LABELS = {
  CREATE: 'Opprettet',
  READ: 'Lest',
  UPDATE: 'Endret',
  DELETE: 'Slettet',
  LOGIN: 'Innlogging',
  REASSIGN: 'Omfordelt',
  EXPORT: 'Eksportert',
}

export function formatTime(instant) {
  if (!instant) return '–'
  return new Date(instant).toLocaleTimeString('no-NO', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Europe/Oslo',
  })
}

export function formatDateTime(instant) {
  if (!instant) return '–'
  return new Date(instant).toLocaleString('no-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'Europe/Oslo',
  })
}

export function formatDateLong(date) {
  return date.toLocaleDateString('no-NO', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'Europe/Oslo',
  })
}

export function todayIsoDate() {
  const now = new Date()
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Oslo' }).format(now)
}

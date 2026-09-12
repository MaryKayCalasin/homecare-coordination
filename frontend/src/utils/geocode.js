const CACHE_PREFIX = 'geocode:'
const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search'
const MIN_INTERVAL_MS = 1100 // Nominatim's usage policy caps public requests at ~1/sec.

let lastRequestAt = 0

function readCache(key) {
  const raw = localStorage.getItem(CACHE_PREFIX + key)
  return raw ? JSON.parse(raw) : null
}

function writeCache(key, value) {
  localStorage.setItem(CACHE_PREFIX + key, JSON.stringify(value))
}

async function throttle() {
  const wait = lastRequestAt + MIN_INTERVAL_MS - Date.now()
  if (wait > 0) {
    await new Promise((resolve) => setTimeout(resolve, wait))
  }
  lastRequestAt = Date.now()
}

/**
 * Resolves an address to { lat, lon } using OpenStreetMap's Nominatim geocoder.
 * Results are cached in localStorage indefinitely (addresses don't move), since
 * Nominatim's free endpoint is rate-limited and not meant for repeated lookups
 * of the same query.
 */
export async function geocodeAddress(query) {
  const key = query.trim().toLowerCase()
  const cached = readCache(key)
  if (cached) return cached

  await throttle()
  const url = `${NOMINATIM_URL}?format=json&limit=1&countrycodes=no&q=${encodeURIComponent(query)}`
  const res = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!res.ok) throw new Error(`Geokoding feilet: ${res.status}`)
  const results = await res.json()
  const result = results[0] ? { lat: Number(results[0].lat), lon: Number(results[0].lon) } : null
  writeCache(key, result)
  return result
}

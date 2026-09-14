-- Nullable geographic coordinates for a patient's address, used to check
-- that a nurse has enough travel time between consecutive visits rather
-- than only checking for a time overlap. Deliberately not backed by a
-- server-side geocoding call: the frontend already geocodes patient
-- addresses client-side via Nominatim for the map view, so the same
-- lookup is reused here instead of adding an external HTTP dependency to
-- the backend. Both columns stay NULL for a patient whose address hasn't
-- been geocoded yet; every travel-time check treats missing coordinates
-- as "unknown, don't block the visit" rather than a hard failure.
ALTER TABLE public.patient
    ADD COLUMN latitude double precision,
    ADD COLUMN longitude double precision;

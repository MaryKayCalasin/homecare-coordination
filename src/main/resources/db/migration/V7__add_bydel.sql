-- Oslo (and only Oslo, today) delivers hjemmetjenesten through 15
-- bydeler, each with its own bydelsutvalg and administrasjon. The legal
-- duty to provide the service sits with the kommune (helse- og
-- omsorgstjenesteloven Sec. 3-1/3-2), not the bydel - "bydel" is Oslo's
-- own internal organizational delegation of that duty, not a separate
-- legal person. What the law actually constrains is access: helsepersonell-
-- loven Sec. 21 and Sec. 25 limit patient-data access to those with a
-- "tjenstlig behov" (professional need) for that specific patient's care.
-- A coordinator in one bydel normally has no care relationship to a
-- patient in another, so bydel-level access scoping is how Oslo's
-- kommune, as data controller, operationalizes that need-to-know
-- principle in practice - which is exactly how ELISE (Oslo's EPJ) scopes
-- it today.
--
-- Nullable everywhere: only Oslo has this subdivision, so every other
-- kommune's rows (and platform-wide accounts) simply leave it unset, and
-- CurrentUser only enforces it when both sides of a check have it set.
ALTER TABLE public.app_user ADD COLUMN bydel character varying(100);
ALTER TABLE public.patient ADD COLUMN bydel character varying(100);
ALTER TABLE public.nurse ADD COLUMN bydel character varying(100);

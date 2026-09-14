-- Ties every login account to the kommune it belongs to, closing a
-- cross-tenant data leak: GET /api/patients/{id} and similar endpoints
-- previously checked only role (ADMIN/COORDINATOR/NURSE), never that the
-- caller's own municipality matched the resource's. A COORDINATOR account
-- in one kommune could fetch any patient in any other kommune by ID.
--
-- Nullable by design: a NULL municipality marks a platform-wide account
-- (e.g. the initial bootstrap admin) that isn't scoped to any single
-- kommune. Every other account must have one - enforced in AuthService,
-- not at the database level, since the rule depends on role.
ALTER TABLE public.app_user
    ADD COLUMN municipality character varying(100);

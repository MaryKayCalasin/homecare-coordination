-- Adds the work-capacity flags introduced on the Nurse entity. Each column
-- is added with a DEFAULT in the same statement, which lets PostgreSQL
-- backfill existing rows and satisfy the NOT NULL constraint in one atomic,
-- non-blocking operation - unlike a bare "ADD COLUMN ... NOT NULL" with no
-- default, which fails outright against a table that already has rows.
--
-- Column names below were confirmed against Hibernate's own generated schema
-- for the current Nurse entity (Spring's naming strategy notably renders
-- canDoIV as can_doiv, not can_do_iv).

ALTER TABLE public.nurse
    ADD COLUMN can_give_medication boolean NOT NULL DEFAULT false,
    ADD COLUMN can_do_wound_care boolean NOT NULL DEFAULT false,
    ADD COLUMN can_doiv boolean NOT NULL DEFAULT false,
    ADD COLUMN can_lift_heavy boolean NOT NULL DEFAULT false,
    ADD COLUMN can_work_alone boolean NOT NULL DEFAULT false,
    ADD COLUMN can_handle_dementia boolean NOT NULL DEFAULT false,
    ADD COLUMN can_do_personal_care boolean NOT NULL DEFAULT true;

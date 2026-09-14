-- Foundation for tracking the statutory decision ("vedtak") that entitles a
-- patient to a home care service - see Vedtak's class Javadoc for scope.
-- visit.vedtak_id is nullable and optional: most existing visits predate
-- this table and have nothing to backfill it from.
CREATE TABLE public.vedtak (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    patient_id uuid NOT NULL,
    service_type character varying(50) NOT NULL,
    granted_hours_per_week numeric(5,2) NOT NULL,
    valid_from date NOT NULL,
    valid_to date,
    decided_by character varying(150) NOT NULL,
    decided_at timestamp(6) with time zone NOT NULL,
    status character varying(20) NOT NULL,
    notes character varying(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_vedtak_patient FOREIGN KEY (patient_id) REFERENCES public.patient (id)
);

CREATE INDEX idx_vedtak_patient ON public.vedtak (patient_id);

ALTER TABLE public.visit
    ADD COLUMN vedtak_id uuid,
    ADD CONSTRAINT fk_visit_vedtak FOREIGN KEY (vedtak_id) REFERENCES public.vedtak (id);

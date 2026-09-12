-- Baseline snapshot of the schema as it existed before Flyway was introduced,
-- when it was managed by Hibernate's ddl-auto=update. Captured via
-- `pg_dump --schema-only` against the live database. For an already-existing
-- database this migration is skipped (see app.flyway.baseline-* in
-- application.yml); for a brand new database it recreates the same starting
-- point so later migrations apply identically in both cases.

CREATE TABLE public.absence (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    end_date_time timestamp(6) with time zone NOT NULL,
    notes character varying(500),
    reason character varying(20) NOT NULL,
    redistributed boolean NOT NULL,
    start_date_time timestamp(6) with time zone NOT NULL,
    nurse_id uuid NOT NULL,
    CONSTRAINT absence_reason_check CHECK (((reason)::text = ANY ((ARRAY['SICK_LEAVE'::character varying, 'VACATION'::character varying, 'TRAINING'::character varying, 'PARENTAL_LEAVE'::character varying, 'PERSONAL'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.app_user (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    email character varying(150) NOT NULL,
    enabled boolean NOT NULL,
    full_name character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    username character varying(100) NOT NULL,
    CONSTRAINT app_user_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'COORDINATOR'::character varying, 'NURSE'::character varying])::text[])))
);

CREATE TABLE public.audit_log (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    action character varying(20) NOT NULL,
    details character varying(1000),
    entity_id character varying(100),
    entity_type character varying(100) NOT NULL,
    ip_address character varying(45),
    performed_by character varying(100) NOT NULL,
    CONSTRAINT audit_log_action_check CHECK (((action)::text = ANY ((ARRAY['CREATE'::character varying, 'READ'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying, 'LOGIN'::character varying, 'REASSIGN'::character varying, 'EXPORT'::character varying])::text[])))
);

CREATE TABLE public.nurse (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    active boolean NOT NULL,
    email character varying(150),
    employee_id character varying(30) NOT NULL,
    full_name character varying(150) NOT NULL,
    municipality character varying(100) NOT NULL,
    phone character varying(20),
    user_id uuid
);

CREATE TABLE public.nurse_qualification (
    nurse_id uuid NOT NULL,
    qualification character varying(40),
    CONSTRAINT nurse_qualification_qualification_check CHECK (((qualification)::text = ANY ((ARRAY['REGISTERED_NURSE'::character varying, 'AUXILIARY_NURSE'::character varying, 'HEALTHCARE_ASSISTANT'::character varying, 'WOUND_CARE_SPECIALIST'::character varying, 'PALLIATIVE_CARE_SPECIALIST'::character varying, 'DEMENTIA_CARE_SPECIALIST'::character varying, 'MEDICATION_ADMINISTRATION'::character varying])::text[])))
);

CREATE TABLE public.observation (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    description character varying(2000) NOT NULL,
    medication_dosage character varying(100),
    medication_given boolean,
    medication_name character varying(200),
    mood_level character varying(20),
    observation_type character varying(20) NOT NULL,
    recorded_at timestamp(6) with time zone NOT NULL,
    urgent boolean NOT NULL,
    urgent_resolved boolean NOT NULL,
    urgent_resolved_at timestamp(6) with time zone,
    urgent_resolved_by character varying(100),
    patient_id uuid NOT NULL,
    recorded_by_nurse_id uuid NOT NULL,
    visit_id uuid,
    CONSTRAINT observation_mood_level_check CHECK (((mood_level)::text = ANY ((ARRAY['VERY_POOR'::character varying, 'POOR'::character varying, 'NEUTRAL'::character varying, 'GOOD'::character varying, 'VERY_GOOD'::character varying])::text[]))),
    CONSTRAINT observation_observation_type_check CHECK (((observation_type)::text = ANY ((ARRAY['MEDICATION'::character varying, 'MOOD'::character varying, 'VITAL_SIGNS'::character varying, 'NUTRITION'::character varying, 'WOUND_CARE'::character varying, 'INCIDENT'::character varying, 'GENERAL'::character varying])::text[])))
);

CREATE TABLE public.patient (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    active boolean NOT NULL,
    address character varying(200),
    care_level character varying(20) NOT NULL,
    care_notes character varying(2000),
    city character varying(100),
    date_of_birth date,
    full_name character varying(150) NOT NULL,
    municipality character varying(100) NOT NULL,
    national_id character varying(11) NOT NULL,
    next_of_kin_name character varying(100),
    next_of_kin_phone character varying(20),
    phone character varying(20),
    postal_code character varying(10),
    primary_diagnosis character varying(500),
    CONSTRAINT patient_care_level_check CHECK (((care_level)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'PALLIATIVE'::character varying])::text[])))
);

CREATE TABLE public.shift_handover_report (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    generated_by character varying(100) NOT NULL,
    municipality character varying(100) NOT NULL,
    observations_count integer NOT NULL,
    shift_date date NOT NULL,
    shift_type character varying(20) NOT NULL,
    summary oid NOT NULL,
    unresolved_urgent_count integer NOT NULL,
    urgent_observations_count integer NOT NULL,
    visits_cancelled_count integer NOT NULL,
    visits_completed_count integer NOT NULL,
    visits_missed_count integer NOT NULL,
    visits_pending_count integer NOT NULL,
    CONSTRAINT shift_handover_report_shift_type_check CHECK (((shift_type)::text = ANY ((ARRAY['DAY'::character varying, 'EVENING'::character varying, 'NIGHT'::character varying])::text[])))
);

CREATE TABLE public.visit (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by character varying(100),
    updated_at timestamp(6) with time zone,
    updated_by character varying(100),
    version bigint,
    actual_end timestamp(6) with time zone,
    actual_start timestamp(6) with time zone,
    location character varying(200),
    notes character varying(1000),
    scheduled_end timestamp(6) with time zone NOT NULL,
    scheduled_start timestamp(6) with time zone NOT NULL,
    status character varying(20) NOT NULL,
    visit_type character varying(30) NOT NULL,
    nurse_id uuid,
    patient_id uuid NOT NULL,
    CONSTRAINT visit_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'MISSED'::character varying])::text[]))),
    CONSTRAINT visit_visit_type_check CHECK (((visit_type)::text = ANY ((ARRAY['MEDICATION'::character varying, 'PERSONAL_CARE'::character varying, 'HEALTH_CHECK'::character varying, 'MEAL_ASSISTANCE'::character varying, 'WOUND_CARE'::character varying, 'SOCIAL_VISIT'::character varying, 'OTHER'::character varying])::text[])))
);

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT absence_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.nurse
    ADD CONSTRAINT nurse_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.observation
    ADD CONSTRAINT observation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.patient
    ADD CONSTRAINT patient_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.shift_handover_report
    ADD CONSTRAINT shift_handover_report_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uk1j9d9a06i600gd43uu3km82jw UNIQUE (email);

ALTER TABLE ONLY public.nurse
    ADD CONSTRAINT uk21wfmxsw2nufjlm2va2ai1xln UNIQUE (employee_id);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uk3k4cplvh82srueuttfkwnylq0 UNIQUE (username);

ALTER TABLE ONLY public.patient
    ADD CONSTRAINT ukgfvwqe45nja6ktq7bjegpnpf8 UNIQUE (national_id);

ALTER TABLE ONLY public.visit
    ADD CONSTRAINT visit_pkey PRIMARY KEY (id);

CREATE INDEX idx_audit_entity ON public.audit_log USING btree (entity_type, entity_id);

CREATE INDEX idx_audit_performed_by ON public.audit_log USING btree (performed_by);

CREATE INDEX idx_observation_patient_recorded ON public.observation USING btree (patient_id, recorded_at);

CREATE INDEX idx_observation_urgent ON public.observation USING btree (urgent, urgent_resolved);

CREATE INDEX idx_visit_nurse_start ON public.visit USING btree (nurse_id, scheduled_start);

CREATE INDEX idx_visit_patient_start ON public.visit USING btree (patient_id, scheduled_start);

ALTER TABLE ONLY public.observation
    ADD CONSTRAINT fk42049fu8vxfydwyrib1gbvfxf FOREIGN KEY (recorded_by_nurse_id) REFERENCES public.nurse(id);

ALTER TABLE ONLY public.visit
    ADD CONSTRAINT fk5pj8hgwpgh4egtk0ucjqpbyx6 FOREIGN KEY (nurse_id) REFERENCES public.nurse(id);

ALTER TABLE ONLY public.nurse_qualification
    ADD CONSTRAINT fk61trnj77wxfy8fjbna61ylbce FOREIGN KEY (nurse_id) REFERENCES public.nurse(id);

ALTER TABLE ONLY public.visit
    ADD CONSTRAINT fkrban5yeabnx30seqm69jw44e FOREIGN KEY (patient_id) REFERENCES public.patient(id);

ALTER TABLE ONLY public.observation
    ADD CONSTRAINT fkrruxjcsqmgpqgyhuwro55smxj FOREIGN KEY (visit_id) REFERENCES public.visit(id);

ALTER TABLE ONLY public.observation
    ADD CONSTRAINT fks3mox9fs3h6uhhfmkf5qk7ygj FOREIGN KEY (patient_id) REFERENCES public.patient(id);

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT fksvflv0joa5b0tvgu7untj3o7t FOREIGN KEY (nurse_id) REFERENCES public.nurse(id);

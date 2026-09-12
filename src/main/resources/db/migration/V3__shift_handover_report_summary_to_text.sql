-- The `summary` column was created as `oid` because Hibernate's @Lob on a
-- String maps to a PostgreSQL large object by default, which requires
-- cursor-based streaming to read back. That fails outside an open
-- transaction, and with spring.jpa.open-in-view: false every read of an
-- existing report happens after the owning service's @Transactional method
-- has already returned - GET /api/handover-reports failed outright with
-- "Unable to access lob stream". A shift summary is plain text with no
-- practical size limit here, so it belongs in an ordinary `text` column.
--
-- lo_get()/convert_from() recover each row's text before the old oid column
-- is dropped, and lo_unlink() releases the now-unreferenced large object so
-- it isn't leaked in pg_largeobject.
ALTER TABLE public.shift_handover_report
    ADD COLUMN summary_text text;

UPDATE public.shift_handover_report
    SET summary_text = convert_from(lo_get(summary), 'UTF8')
    WHERE summary IS NOT NULL;

SELECT lo_unlink(summary) FROM public.shift_handover_report WHERE summary IS NOT NULL;

ALTER TABLE public.shift_handover_report
    ALTER COLUMN summary_text SET NOT NULL,
    DROP COLUMN summary;

ALTER TABLE public.shift_handover_report
    RENAME COLUMN summary_text TO summary;

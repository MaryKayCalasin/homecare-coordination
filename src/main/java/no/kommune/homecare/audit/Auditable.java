package no.kommune.homecare.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose invocation must be recorded in the GDPR audit
 * trail. Applied to any method that creates, reads, updates or deletes
 * personal or health data belonging to a patient.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

    AuditAction action();

    String entityType();

    String details() default "";
}

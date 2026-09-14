package no.kommune.homecare.visit;

import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Oslo and Bergen are roughly 300km apart as the crow flies - no nurse can
 * cross that gap between two back-to-back visits, which is exactly the kind
 * of scheduling mistake clock-only overlap checking would miss.
 */
class TravelTimeTest {

    private static final Instant NOW = Instant.now();

    @Test
    void rejectsAVisitTooSoonAfterAFarAwayPreviousVisit() {
        Patient oslo = patientAt("Oslo", 59.9139, 10.7522);
        Patient bergen = patientAt("Bergen", 60.3913, 5.3221);

        Visit previous = withId(Visit.builder()
                .patient(oslo)
                .scheduledStart(NOW)
                .scheduledEnd(NOW.plus(1, ChronoUnit.HOURS))
                .status(VisitStatus.SCHEDULED)
                .visitType(VisitType.PERSONAL_CARE)
                .build());

        Instant nextStart = NOW.plus(90, ChronoUnit.MINUTES);
        Instant nextEnd = nextStart.plus(1, ChronoUnit.HOURS);

        boolean feasible = TravelTime.isFeasible(List.of(previous), bergen, nextStart, nextEnd);

        assertThat(feasible).isFalse();
    }

    @Test
    void allowsBackToBackVisitsWhenTheGapCoversTheTravelTime() {
        Patient oslo = patientAt("Oslo", 59.9139, 10.7522);
        Patient nearbyInOslo = patientAt("Nearby", 59.9200, 10.7600);

        Visit previous = withId(Visit.builder()
                .patient(oslo)
                .scheduledStart(NOW)
                .scheduledEnd(NOW.plus(1, ChronoUnit.HOURS))
                .status(VisitStatus.SCHEDULED)
                .visitType(VisitType.PERSONAL_CARE)
                .build());

        Instant nextStart = NOW.plus(90, ChronoUnit.MINUTES);
        Instant nextEnd = nextStart.plus(1, ChronoUnit.HOURS);

        boolean feasible = TravelTime.isFeasible(List.of(previous), nearbyInOslo, nextStart, nextEnd);

        assertThat(feasible).isTrue();
    }

    @Test
    void treatsMissingCoordinatesAsAlwaysFeasible() {
        Patient noCoordinates = withId(Patient.builder().fullName("No Coordinates")
                .nationalId("01019012345").municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build());
        Patient bergen = patientAt("Bergen", 60.3913, 5.3221);

        Visit previous = withId(Visit.builder()
                .patient(bergen)
                .scheduledStart(NOW)
                .scheduledEnd(NOW.plus(1, ChronoUnit.HOURS))
                .status(VisitStatus.SCHEDULED)
                .visitType(VisitType.PERSONAL_CARE)
                .build());

        Instant nextStart = NOW.plus(70, ChronoUnit.MINUTES);
        Instant nextEnd = nextStart.plus(1, ChronoUnit.HOURS);

        boolean feasible = TravelTime.isFeasible(List.of(previous), noCoordinates, nextStart, nextEnd);

        assertThat(feasible).isTrue();
    }

    private static Patient patientAt(String city, double lat, double lon) {
        return withId(Patient.builder().fullName(city)
                .nationalId("01019012345")
                .municipality(city).careLevel(CareLevel.MEDIUM).active(true)
                .latitude(lat).longitude(lon).build());
    }

    private static <T extends no.kommune.homecare.common.entity.BaseEntity> T withId(T entity) {
        entity.setId(UUID.randomUUID());
        return entity;
    }
}

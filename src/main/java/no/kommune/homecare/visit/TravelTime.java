package no.kommune.homecare.visit;

import no.kommune.homecare.patient.Patient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Checks that a nurse has enough time to physically get from one patient's
 * address to the next, not just that two visits don't overlap on the
 * clock - often the real binding constraint in a spread-out kommune.
 * <p>
 * Deliberately a straight-line (haversine) distance at an assumed average
 * speed rather than a real routing API: this project has no server-side
 * geocoding or routing integration, and a conservative heuristic that
 * degrades gracefully when a patient has no coordinates yet is more
 * honest than pretending to model actual road distance. Treat this as a
 * floor on the real travel time, not an estimate of it.
 */
public final class TravelTime {

    private TravelTime() {
    }

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Conservative average speed for door-to-door home visits - local
     * roads, not highway, plus parking and walking to the entrance - used
     * to turn a straight-line distance into a minimum travel time.
     */
    private static final double ASSUMED_SPEED_KMH = 30.0;

    public static boolean hasCoordinates(Patient patient) {
        return patient.getLatitude() != null && patient.getLongitude() != null;
    }

    /** Straight-line distance between two patients' addresses. */
    public static double distanceKm(Patient a, Patient b) {
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }

    /** The minimum time it would take to get from one patient's address to the other's. */
    public static Duration minimumTravelTime(Patient a, Patient b) {
        double hours = distanceKm(a, b) / ASSUMED_SPEED_KMH;
        return Duration.ofSeconds(Math.round(hours * 3600));
    }

    /**
     * Whether a nurse could realistically travel to {@code patient} in time
     * for a visit from {@code start} to {@code end}, given the other visits
     * ({@code nearbyVisits}) already on their schedule. Visits that overlap
     * the new one are assumed to have already been rejected elsewhere - this
     * only looks at the nearest visit immediately before and after. A patient
     * (on either side) with no geocoded coordinates yet is always treated as
     * feasible: missing data should never block a visit outright.
     */
    public static boolean isFeasible(List<Visit> nearbyVisits, Patient patient, Instant start, Instant end) {
        if (!hasCoordinates(patient)) {
            return true;
        }
        for (Visit other : nearbyVisits) {
            if (!hasCoordinates(other.getPatient())) {
                continue;
            }
            if (!other.getScheduledEnd().isAfter(start)) {
                Duration gap = Duration.between(other.getScheduledEnd(), start);
                if (gap.compareTo(minimumTravelTime(other.getPatient(), patient)) < 0) {
                    return false;
                }
            } else if (!other.getScheduledStart().isBefore(end)) {
                Duration gap = Duration.between(end, other.getScheduledStart());
                if (gap.compareTo(minimumTravelTime(patient, other.getPatient())) < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}

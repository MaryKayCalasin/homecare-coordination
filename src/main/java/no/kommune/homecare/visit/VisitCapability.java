package no.kommune.homecare.visit;

import no.kommune.homecare.nurse.Nurse;

/**
 * Maps a visit type to the work-capacity flag on {@link Nurse} required to
 * carry it out. Only {@link VisitType#MEDICATION}, {@link VisitType#WOUND_CARE}
 * and {@link VisitType#PERSONAL_CARE} correspond to one of Nurse's capacity
 * flags today; the remaining visit types (health checks, meal assistance,
 * social visits, and the catch-all OTHER) can be carried out by any active
 * nurse. {@code canDoIV}, {@code canLiftHeavy}, {@code canWorkAlone} and
 * {@code canHandleDementia} aren't matched against anything here, since no
 * visit type currently models those needs.
 */
public final class VisitCapability {

    private VisitCapability() {
    }

    public static boolean isQualified(Nurse nurse, VisitType visitType) {
        return switch (visitType) {
            case MEDICATION -> nurse.isCanGiveMedication();
            case WOUND_CARE -> nurse.isCanDoWoundCare();
            case PERSONAL_CARE -> nurse.isCanDoPersonalCare();
            case HEALTH_CHECK, MEAL_ASSISTANCE, SOCIAL_VISIT, OTHER -> true;
        };
    }
}

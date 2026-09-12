package no.kommune.homecare.handover;

import java.time.LocalTime;

/**
 * The three shifts typically worked in Norwegian municipal home care:
 * dagvakt, kveldsvakt and nattevakt.
 */
public enum ShiftType {
    DAY(LocalTime.of(7, 0), LocalTime.of(15, 0)),
    EVENING(LocalTime.of(15, 0), LocalTime.of(23, 0)),
    NIGHT(LocalTime.of(23, 0), LocalTime.of(7, 0));

    private final LocalTime defaultStart;
    private final LocalTime defaultEnd;

    ShiftType(LocalTime defaultStart, LocalTime defaultEnd) {
        this.defaultStart = defaultStart;
        this.defaultEnd = defaultEnd;
    }

    public LocalTime defaultStart() {
        return defaultStart;
    }

    public LocalTime defaultEnd() {
        return defaultEnd;
    }

    /** Night shift crosses midnight, so its end time falls on the following calendar day. */
    public boolean crossesMidnight() {
        return this == NIGHT;
    }
}

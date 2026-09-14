package no.kommune.homecare.vedtak;

/**
 * The kind of service a {@link Vedtak} grants, grouped the way IPLOS
 * (Individbasert pleie- og omsorgsstatistikk - Norway's national statistics
 * registry for care services, run by Helsedirektoratet) categorizes home
 * care services. Names here are descriptive, not the literal numeric IPLOS
 * tjenestetype codes - a real integration would map each value below to its
 * registry code when submitting the annual statistics export, which this
 * project does not do.
 */
public enum IplosServiceType {
    HOME_NURSING,
    PRACTICAL_ASSISTANCE_DAILY_LIVING,
    PRACTICAL_ASSISTANCE_HOUSEHOLD,
    SUPPORT_CONTACT,
    RESPITE_CARE,
    DAY_ACTIVITY,
    SHORT_TERM_STAY,
    LONG_TERM_STAY
}

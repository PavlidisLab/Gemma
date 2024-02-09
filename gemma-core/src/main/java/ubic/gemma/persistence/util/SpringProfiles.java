package ubic.gemma.persistence.util;

/**
 * Constants used for various Spring profiles.
 * @author poirigui
 */
@SuppressWarnings("unused")
public class SpringProfiles {

    public static final String PRODUCTION = "production",
            DEV = "dev",
            TEST = "test";

    /**
     * A special profile that indicates that no database connection should be used.
     */
    public static final String NODB = "nodb";
}

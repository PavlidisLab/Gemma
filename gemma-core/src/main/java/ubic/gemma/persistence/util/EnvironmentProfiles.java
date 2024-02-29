package ubic.gemma.persistence.util;

/**
 * Environment profiles used in the Spring context.
 * <p>
 * These are mutually exclusive.
 * @author poirigui
 */
@SuppressWarnings("unused")
public class EnvironmentProfiles {

    public static final String
            PRODUCTION = "production",
            DEV = "dev",
            TEST = "test";
}

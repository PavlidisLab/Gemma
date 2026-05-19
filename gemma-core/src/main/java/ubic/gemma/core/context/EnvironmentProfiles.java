package ubic.gemma.core.context;

/**
 * Spring profile name constants used in the Gemma context.
 * <p>
 * The environment profiles ({@link #PRODUCTION}, {@link #DEV}, {@link #TEST}) are mutually exclusive: exactly one must
 * be active. The feature profiles ({@link #WEB}, {@link #CLI}, {@link #SCHEDULER}, {@link #METRICS},
 * {@link #PROFILING}) are independent toggles that may be combined with any environment profile.
 *
 * @author poirigui
 */
@SuppressWarnings("unused")
public class EnvironmentProfiles {

    /**
     * Mutually-exclusive environment profiles. Exactly one must be active at runtime.
     */
    public static final String
            PRODUCTION = "production",
            DEV = "dev",
            TEST = "test";

    /**
     * Independent feature toggles. Any combination (including zero) may be active alongside an environment profile.
     */
    public static final String
            WEB = "web",
            CLI = "cli",
            SCHEDULER = "scheduler",
            METRICS = "metrics",
            PROFILING = "profiling";
}

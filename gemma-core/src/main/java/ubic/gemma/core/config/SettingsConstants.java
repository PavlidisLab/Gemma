package ubic.gemma.core.config;

class SettingsConstants {

    /**
     * Prefix for system properties.
     */
    static final String SYSTEM_PROPERTY_PREFIX = "gemma.";

    /**
     * System property for loading a specific user configuration file.
     */
    static final String GEMMA_CONFIG_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "config";

    /**
     * The name of the file users can use to configure Gemma.
     */
    static final String USER_CONFIGURATION = "Gemma.properties";

    /**
     * Name of the resource that is used to configure Gemma internally.
     */
    static final String BUILTIN_CONFIGURATION = "project.properties";

    /**
     * Name of the resource containing defaults that the user can override (classpath)
     */
    static final String DEFAULT_CONFIGURATION = "default.properties";

    /**
     * List of default configurations.
     */
    static final String[] DEFAULT_CONFIGURATIONS = { DEFAULT_CONFIGURATION, BUILTIN_CONFIGURATION };
}

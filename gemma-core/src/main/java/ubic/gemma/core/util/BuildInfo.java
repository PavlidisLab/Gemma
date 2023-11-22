package ubic.gemma.core.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.DateFormatter;
import ubic.gemma.persistence.util.Settings;

import java.util.Date;
import java.util.Locale;

@Configuration
public class BuildInfo implements InitializingBean {

    private static final String MAVEN_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Retrieves the build info using {@link Settings}
     * @deprecated simply autowire this configuration to access build information at runtime
     */
    @Deprecated
    public static BuildInfo fromSettings() {
        try {
            return new BuildInfo( Settings.getString( "gemma.version" ),
                    Settings.getString( "gemma.build.timestamp" ),
                    Settings.getString( "gemma.build.gitHash" ) );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Value("${gemma.version}")
    private String version;
    @Value("${gemma.build.timestamp}")
    private String timestampAsString;
    @Value("${gemma.build.gitHash}")
    private String gitHash;

    private Date timestamp;

    public BuildInfo() {

    }

    private BuildInfo( String version, String timestampAsString, String gitHash ) throws Exception {
        this.version = version;
        this.timestampAsString = timestampAsString;
        this.gitHash = gitHash;
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        timestamp = new DateFormatter( MAVEN_DATETIME_PATTERN ).parse( timestampAsString, Locale.getDefault() );
    }

    public String getVersion() {
        return version;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getGitHash() {
        return gitHash;
    }
}

package ubic.gemma.core.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.util.hdf5.H5Utils;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

@Component
@CommonsLog
public class BuildInfo implements InitializingBean {

    private static final DateFormat MAVEN_DATETIME_PATTERN;
    /**
     * Format used by Maven 3.1.1.
     */
    private static final DateFormat MAVEN_3_1_1_DATETIME_PATTERN;

    static {
        MAVEN_DATETIME_PATTERN = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH );
        MAVEN_DATETIME_PATTERN.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        // we always build from America/Vancouver timezone, starting 1.32, this format will no-longer be used because we
        // will require Maven 3.6.3+ for building
        MAVEN_3_1_1_DATETIME_PATTERN = new SimpleDateFormat( "yyyyMMdd-HHmm", Locale.ENGLISH );
        MAVEN_3_1_1_DATETIME_PATTERN.setTimeZone( TimeZone.getTimeZone( "America/Vancouver" ) );
    }

    /**
     * Retrieve build information directly from the classpath.
     */
    public static BuildInfo fromClasspath() {
        Properties props = new Properties();
        try ( InputStream reader = new ClassPathResource( "/ubic/gemma/version.properties" ).getInputStream() ) {
            props.load( reader );
            return new BuildInfo( props.getProperty( "gemma.version" ),
                    props.getProperty( "gemma.build.timestamp" ),
                    props.getProperty( "gemma.build.gitHash" ) );
        } catch ( FileNotFoundException e ) {
            return new BuildInfo();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Value("${gemma.version:#{null}}")
    private String version;
    @Value("${gemma.build.timestamp:#{null}}")
    private String timestampAsString;
    @Value("${gemma.build.gitHash:#{null}}")
    private String gitHash;

    private Date timestamp;

    public BuildInfo() {

    }

    BuildInfo( String version, String timestampAsString, String gitHash ) {
        this.version = version;
        this.timestampAsString = timestampAsString;
        this.gitHash = gitHash;
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        if ( timestampAsString != null ) {
            try {
                timestamp = MAVEN_DATETIME_PATTERN.parse( timestampAsString );
            } catch ( ParseException e ) {
                try {
                    timestamp = MAVEN_3_1_1_DATETIME_PATTERN.parse( timestampAsString );
                } catch ( ParseException ex ) {
                    log.error( "Failed to parse build timestamp.", e );
                }
            }
        }
    }

    @Nullable
    public String getVersion() {
        return version;
    }

    @Nullable
    public Date getTimestamp() {
        return timestamp;
    }

    @Nullable
    public String getGitHash() {
        return gitHash;
    }

    /**
     * Obtain the version of HDF5 used by Gemma.
     */
    public String getHDF5Version() {
        return H5Utils.getH5Version();
    }

    @Override
    public String toString() {
        return String.format( "%s%s%s%s",
                version != null ? version : "?",
                timestamp != null || gitHash != null ? " built" : "",
                timestamp != null ? " on " + DateFormat.getDateTimeInstance().format( timestamp ) : "",
                gitHash != null ? " from " + gitHash : "" );
    }
}

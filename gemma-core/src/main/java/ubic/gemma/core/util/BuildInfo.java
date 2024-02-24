package ubic.gemma.core.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.datetime.DateFormatter;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

@CommonsLog
@Configuration
public class BuildInfo implements InitializingBean {

    private static final String MAVEN_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

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

    @Value("${gemma.version}")
    private String version;
    @Value("${gemma.build.timestamp}")
    private String timestampAsString;
    @Value("${gemma.build.gitHash}")
    private String gitHash;

    private Date timestamp;

    public BuildInfo() {

    }

    private BuildInfo( String version, String timestampAsString, String gitHash ) {
        this.version = version;
        this.timestampAsString = timestampAsString;
        this.gitHash = gitHash;
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        if ( timestampAsString != null ) {
            try {
                timestamp = new DateFormatter( MAVEN_DATETIME_PATTERN )
                        .parse( timestampAsString, Locale.getDefault() );

            } catch ( ParseException e ) {
                log.error( "Failed to parse build timestamp.", e );
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

    @Override
    public String toString() {
        return String.format( "%s%s%s%s",
                version != null ? version : "?",
                timestamp != null || gitHash != null ? " built" : "",
                timestamp != null ? " on " + DateFormat.getDateTimeInstance().format( timestamp ) : "",
                gitHash != null ? " from " + gitHash : "" );
    }
}

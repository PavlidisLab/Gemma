package ubic.gemma.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * A PropertyPlaceholderConfigurer that also can take a Configuration. I got the idea for this from
 * {@link http://mail-archives.apache.org/mod_mbox/jakarta-commons-dev/200603.mbox/%3Cbug-39068-7685@http.issues.apache.org/bugzilla/%3E}
 * <p>
 * Currently values in the configuration overrides any in the properties files.
 * 
 * @author pavlidis
 * @version $Id$
 * @see org.apache.commons.configuration.Configuration
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public class CommonsConfigurationPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private Log log = LogFactory.getLog( CommonsConfigurationPropertyPlaceholderConfigurer.class.getName() );
    Configuration configuration;

    /**
     * @param conf the conf to set
     */
    public void setConfiguration( PropertiesConfiguration configuration ) {
        this.configuration = configuration;
    }

    @Override
    protected Properties mergeProperties() throws IOException {
        Properties result = super.mergeProperties();

        // now load properties from configuration (build.properties), to override the
        // earlier properties from the xml
        if ( this.configuration != null ) {
            for ( Iterator it = configuration.getKeys(); it.hasNext(); ) {
                String key = ( String ) it.next();
                result.setProperty( key, configuration.getString( key ) );
                log.debug( key + "=" + configuration.getString( key ) );
            }
        } else {
            log.warn( "Configuration was null" );
        }

        return result;
    }
}

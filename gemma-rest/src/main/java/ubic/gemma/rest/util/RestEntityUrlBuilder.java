package ubic.gemma.rest.util;

import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.persistence.util.EntityUrlBuilder;

public class RestEntityUrlBuilder extends EntityUrlBuilder {

    public RestEntityUrlBuilder( @Value("${gemma.hosturl}") String hostUrl ) {
        super( hostUrl );
        setRestByDefault();
    }
}

package ubic.gemma.web.util;

import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.web.taglib.AbstractStaticAssetTag;

/**
 * Configuration for serving static assets externally.
 * @author poirigui
 * @see AbstractStaticAssetTag
 */
@Getter
@Component
@CommonsLog
public class StaticServer implements InitializingBean {

    @Value("${gemma.staticServer.enabled}")
    private boolean enabled;

    @Value("${gemma.staticServer.baseUrl}")
    private String baseUrl;

    @Override
    public void afterPropertiesSet() {
        if ( enabled ) {
            log.info( "Static assets will be served from " + baseUrl + "." );
        }
    }
}

package ubic.gemma.persistence.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.persistence.util.EntityUrl.EntityUrlChooser;

/**
 * @author poirigui
 */
@Component
public class EntityUrlBuilder {

    private final String hostUrl;

    @Autowired
    public EntityUrlBuilder( @Value("${gemma.hosturl}") String hostUrl ) {
        Assert.isTrue( !hostUrl.endsWith( "/" ), "The context path must not end with '/'." );
        this.hostUrl = hostUrl;
    }

    /**
     * Obtain an {@link EntityUrlChooser} for generating a URL relative to the host URL.
     */
    public EntityUrlChooser fromHostUrl() {
        return EntityUrl.of( hostUrl );
    }

    /**
     * Obtain an {@link EntityUrlChooser} for generating a URL relative to the context path.
     * <p>
     * Use this for relative URLs.
     */
    public EntityUrlChooser fromContextPath( String contextPath ) {
        Assert.isTrue( !contextPath.endsWith( "/" ), "The context path must not end with '/'." );
        return EntityUrl.of( contextPath );
    }
}

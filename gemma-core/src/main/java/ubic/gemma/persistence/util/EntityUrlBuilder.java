package ubic.gemma.persistence.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;

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
     * Obtain an {@link EntityUrl} for generating an URL relative to the host URL.
     * <p>
     * Use this for external URLs.
     */
    public <T extends Identifiable> EntityUrl<T> fromHostUrl( T entity ) {
        return EntityUrl.of( hostUrl, entity );
    }

    /**
     * Obtain an {@link EntityUrl} for generating an URL relative to the context path.
     * <p>
     * Use this for relative URLs.
     */
    public <T extends Identifiable> EntityUrl<T> fromContextPath( T entity, String contextPath ) {
        Assert.isTrue( !contextPath.endsWith( "/" ), "The context path must not end with '/'." );
        return EntityUrl.of( contextPath, entity );
    }
}

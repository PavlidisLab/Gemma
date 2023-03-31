package ubic.gemma.core.ontology.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.util.Configuration;
import ubic.gemma.persistence.util.AbstractAsyncFactoryBean;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Factory bean for baseCode's {@link OntologyService}.
 * @param <T> the type of ontology service this factory produces
 */
@CommonsLog
public class OntologyServiceFactory<T extends OntologyService> extends AbstractAsyncFactoryBean<T> {

    /**
     * Determine if ontologies are to be loaded on startup.
     */
    private static final boolean isAutoLoad = ( StringUtils.isBlank( Configuration.getString( "load.ontologies" ) ) || Configuration.getBoolean( "load.ontologies" ) );

    static {
        if ( !isAutoLoad ) {
            log.warn( "Auto-loading of ontologies is disabled, enable it by setting load.ontologies=true in Gemma.properties." );
        }
    }

    /**
     * Thread pool used to initialize ontologies.
     */
    private static final ExecutorService executor = Executors.newFixedThreadPool( 4 );

    @Nullable
    private Class<T> ontologyServiceClass;
    private boolean forceLoad = false;
    private boolean forceIndexing = false;

    /**
     * Class for the ontology service.
     * <p>
     * If null, {@link #createOntologyService()} must be implemented by a subclass.
     */
    public void setOntologyServiceClass( @Nullable Class<T> ontologyServiceClass ) {
        this.ontologyServiceClass = ontologyServiceClass;
    }

    /**
     * Force loading, regardless of the {@code load.{name}Ontology} property value.
     */
    public void setForceLoad( boolean forceLoad ) {
        this.forceLoad = forceLoad;
    }

    /**
     * Force indexing for full-text search.
     */
    public void setForceIndexing( boolean forceIndexing ) {
        this.forceIndexing = forceIndexing;
    }

    public boolean isEnabled() {
        // FIXME: this can be a blocking call if the service is not enabled via load.{ontologyName} but was forced and
        //        forceLoad is false
        return forceLoad || ( service != null && service.isEnabled() );
    }

    public OntologyServiceFactory() {
        super( executor );
    }

    @Nullable
    private T service = null;

    @Override
    protected final T createObject() {
        service = createOntologyService();
        if ( isAutoLoad ) {
            service.initialize( forceLoad, forceIndexing );
        }
        return service;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected T createOntologyService() {
        if ( ontologyServiceClass != null ) {
            return BeanUtils.instantiate( ontologyServiceClass );
        } else {
            throw new RuntimeException( "You must override this createOntologyService() if ontologyServiceClass is not provided." );
        }
    }

    @Override
    public String toString() {
        return service != null ? service.toString() : null;
    }
}
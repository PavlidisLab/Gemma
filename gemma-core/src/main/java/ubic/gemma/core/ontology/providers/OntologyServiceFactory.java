package ubic.gemma.core.ontology.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.util.Configuration;

/**
 * Factory bean for baseCode's {@link OntologyService}.
 * @param <T> the type of ontology service this factory produces
 */
@CommonsLog
public class OntologyServiceFactory<T extends OntologyService> implements FactoryBean<T>, DisposableBean {

    /**
     * Determine if ontologies are to be loaded on startup.
     */
    private static final boolean isAutoLoad = ( StringUtils.isBlank( Configuration.getString( "load.ontologies" ) ) || Configuration.getBoolean( "load.ontologies" ) );

    static {
        if ( !isAutoLoad ) {
            log.warn( "Auto-loading of ontologies is disabled, enable it by setting load.ontologies=true in Gemma.properties." );
        }
    }

    private final Class<T> ontologyServiceClass;
    private boolean forceLoad = false;
    private boolean forceIndexing = false;
    private boolean initializeInBackground = true;


    /**
     * @param ontologyServiceClass Class for the ontology service.
     */
    public OntologyServiceFactory( Class<T> ontologyServiceClass ) {
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

    /**
     * Initialize the service using a background thread.
     *
     * @see OntologyService#startInitializationThread(boolean, boolean)
     * @see OntologyService#initialize(boolean, boolean)
     */
    public void setInitializeInBackground( boolean initializeInBackground ) {
        this.initializeInBackground = initializeInBackground;
    }

    private T service = null;

    @Override
    public synchronized T getObject() {
        if ( service != null )
            return service;
        service = BeanUtils.instantiate( ontologyServiceClass );
        if ( isAutoLoad || forceLoad ) {
            if ( initializeInBackground ) {
                service.startInitializationThread( forceLoad, forceIndexing );
            } else {
                service.initialize( forceLoad, forceIndexing );
            }
        }
        return service;
    }

    @Override
    public Class<?> getObjectType() {
        return ontologyServiceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private T createOntologyService() {
        if ( ontologyServiceClass != null ) {
            return BeanUtils.instantiate( ontologyServiceClass );
        } else {
            throw new RuntimeException( "You must override this createOntologyService() if ontologyServiceClass is not provided." );
        }
    }

    @Override
    public void destroy() throws Exception {
        if ( service.isInitializationThreadAlive() ) {
            log.info( String.format( "Cancelling initialization thread for %s...", service.getClass().getName() ) );
            service.cancelInitializationThread();
        }
    }
}
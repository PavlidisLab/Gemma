package ubic.gemma.core.ontology.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.task.TaskExecutor;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.util.Configuration;

/**
 * Factory bean for baseCode's {@link OntologyService}.
 * @param <T> the type of ontology service this factory produces
 */
@CommonsLog
public class OntologyServiceFactory<T extends OntologyService> extends AbstractFactoryBean<T> {

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
    private boolean loadInBackground = true;
    private TaskExecutor ontologyTaskExecutor = null;
    private boolean enableInference = true;
    private boolean enableSearch = true;
    private boolean processImports = true;


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
    public void setLoadInBackground( boolean loadInBackground ) {
        this.loadInBackground = loadInBackground;
    }

    /**
     * Set the task executor used for initializing ontology service in background.
     */
    public void setTaskExecutor( TaskExecutor taskExecutor ) {
        this.ontologyTaskExecutor = taskExecutor;
    }

    /**
     * Enable inference for the ontology.
     */
    public void setEnableInference( boolean enableInference ) {
        this.enableInference = enableInference;
    }

    /**
     * Enable full-text search for the ontology.
     */
    public void setEnableSearch( boolean enableSearch ) {
        this.enableSearch = enableSearch;
    }

    public void setProcessImports( boolean processImports ) {
        this.processImports = processImports;
    }

    /**
     * Check if the ontology returned by this factory will be loaded.
     * <p>
     * This happens if either the {@code load.ontologies} configuration key is set to true or the loading is forced via
     * {@link #setForceLoad(boolean)}.
     */
    public boolean isAutoLoaded() {
        return isAutoLoad || forceLoad;
    }

    @Override
    public Class<?> getObjectType() {
        return ontologyServiceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    protected T createInstance() throws Exception {
        T service = BeanUtils.instantiate( ontologyServiceClass );
        service.setInferenceMode( enableInference ? OntologyService.InferenceMode.TRANSITIVE : OntologyService.InferenceMode.NONE );
        service.setSearchEnabled( enableSearch );
        service.setProcessImports( processImports );
        if ( isAutoLoad || forceLoad ) {
            if ( loadInBackground ) {
                if ( ontologyTaskExecutor != null ) {
                    ontologyTaskExecutor.execute( () -> service.initialize( forceLoad, forceIndexing ) );
                } else {
                    service.startInitializationThread( forceLoad, forceIndexing );
                }
            } else {
                service.initialize( forceLoad, forceIndexing );
            }
        }
        return service;
    }

    @Override
    protected void destroyInstance( T instance ) {
        if ( instance.isInitializationThreadAlive() ) {
            log.info( String.format( "Cancelling initialization thread for %s...", instance ) );
            instance.cancelInitializationThread();
        }
    }
}
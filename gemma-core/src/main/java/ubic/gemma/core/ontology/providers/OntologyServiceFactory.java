package ubic.gemma.core.ontology.providers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.task.TaskExecutor;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.util.Configuration;

import javax.annotation.Nullable;
import java.util.Set;

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

    private final Class<T> ontologyServiceClass;
    private boolean forceLoad = false;
    private boolean forceIndexing = false;
    private boolean loadInBackground = true;
    @Nullable
    private TaskExecutor ontologyTaskExecutor = null;
    private OntologyService.LanguageLevel languageLevel = OntologyService.LanguageLevel.FULL;
    private OntologyService.InferenceMode inferenceMode = OntologyService.InferenceMode.TRANSITIVE;
    private boolean enableSearch = true;
    private boolean processImports = true;
    @Nullable
    private Set<String> additionalPropertyUris = null;


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
     * <p>
     * If an executor is supplied via {@link #setTaskExecutor(TaskExecutor)}, it will be used for initializing the
     * ontology, otherwise one thread per ontology will be created via {@link OntologyService#startInitializationThread(boolean, boolean)}.
     * <p>
     * If false, the ontology will be initialized in the foreground. This will dramatically impact the startup time of
     * Gemma.
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
     * Set the supported OWL language level for the ontology.
     * @see OntologyService#setLanguageLevel(OntologyService.LanguageLevel)
     */
    public void setLanguageLevel( OntologyService.LanguageLevel languageLevel ) {
        this.languageLevel = languageLevel;
    }

    /**
     * Set the inference mode for the ontology.
     * @see OntologyService#setInferenceMode(OntologyService.InferenceMode)
     */
    public void setInferenceMode( OntologyService.InferenceMode inferenceMode ) {
        this.inferenceMode = inferenceMode;
    }

    /**
     * Enable full-text search for the ontology.
     * @see OntologyService#setSearchEnabled(boolean)
     */
    public void setEnableSearch( boolean enableSearch ) {
        this.enableSearch = enableSearch;
    }

    /**
     * Enable import processing for the ontology.
     * @see OntologyService#setProcessImports(boolean)
     */
    public void setProcessImports( boolean processImports ) {
        this.processImports = processImports;
    }

    /**
     * Set the URIs used for inferring additional properties.
     * <p>
     * If null, baseCode's defaults will be used. You may use an empty set to disable additional properties inference.
     * @see OntologyService#setAdditionalPropertyUris(Set)
     */
    public void setAdditionalPropertyUris( @Nullable Set<String> additionalPropertyUris ) {
        this.additionalPropertyUris = additionalPropertyUris;
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
        service.setLanguageLevel( languageLevel );
        service.setInferenceMode( inferenceMode );
        service.setSearchEnabled( enableSearch );
        service.setProcessImports( processImports );
        if ( additionalPropertyUris != null ) {
            service.setAdditionalPropertyUris( additionalPropertyUris );
        }
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
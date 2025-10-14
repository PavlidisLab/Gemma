package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.context.AbstractAsyncFactoryBean;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * Factory for {@link HomologeneService}.
 */
@CommonsLog
public class HomologeneServiceFactory extends AbstractAsyncFactoryBean<HomologeneService> {

    private static final String HOMOLOGENE_FILE_CONFIG = "ncbi.homologene.fileName";
    private static final String LOAD_HOMOLOGENE_CONFIG = "load.homologene";
    private static final boolean LOAD_HOMOLOGENE = Settings.getBoolean( HomologeneServiceFactory.LOAD_HOMOLOGENE_CONFIG, true );

    private final GeneService geneService;
    private final TaxonService taxonService;

    private Resource homologeneFile = new HomologeneNcbiFtpResource( Settings.getString( HOMOLOGENE_FILE_CONFIG ) );
    private boolean loadHomologene = LOAD_HOMOLOGENE;

    public HomologeneServiceFactory( GeneService geneService, TaxonService taxonService ) {
        this.geneService = geneService;
        this.taxonService = taxonService;
    }

    /**
     * Set the resource used for loading Homologene.
     */
    public void setHomologeneFile( Resource homologeneFile ) {
        preventModificationIfInitialized( "homologeneFile" );
        this.homologeneFile = homologeneFile;
    }

    /**
     * Set whether to load homologene or not.
     */
    public void setLoadHomologene( boolean loadHomologene ) {
        preventModificationIfInitialized( "loadHomologene" );
        this.loadHomologene = loadHomologene;
    }

    @Override
    protected HomologeneService createObject() throws Exception {
        HomologeneService homologeneService = new HomologeneServiceImpl( geneService, taxonService, homologeneFile );
        if ( loadHomologene ) {
            homologeneService.refresh();
        }
        return homologeneService;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private void preventModificationIfInitialized( String field ) {
        Assert.state( !isInitialized(), String.format( "The Homologene service has already been initialized, changing %s is not allowed.", field ) );
    }
}

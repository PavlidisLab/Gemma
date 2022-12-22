package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.AbstractAsyncFactoryBean;
import ubic.gemma.persistence.util.Settings;

/**
 * Factory for {@link HomologeneService}.
 */
@Component
public class HomologeneServiceFactory extends AbstractAsyncFactoryBean<HomologeneService> {

    private static final String HOMOLOGENE_FILE_CONFIG = "ncbi.homologene.fileName";
    private static final String LOAD_HOMOLOGENE_CONFIG = "load.homologene";
    private static final boolean LOAD_HOMOLOGENE = Settings.getBoolean( HomologeneServiceFactory.LOAD_HOMOLOGENE_CONFIG, true );

    @Autowired
    private GeneService geneService;
    @Autowired
    private TaxonService taxonService;
    private Resource homologeneFile = new HomologeneNcbiFtpResource( Settings.getString( HOMOLOGENE_FILE_CONFIG ) );

    /**
     * Set the resource used for loading Homologene.
     */
    public void setHomologeneFile( Resource homologeneFile ) {
        if ( isInitialized() ) {
            throw new IllegalStateException( "The Homologene service has already been initialized, changing the resource is not allowed." );
        }
        this.homologeneFile = homologeneFile;
    }

    @Override
    protected HomologeneService createObject() throws Exception {
        return new HomologeneServiceImpl( geneService, taxonService, homologeneFile, LOAD_HOMOLOGENE );
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.core.context.AsyncFactoryBean;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;

@Configuration
public class HomologeneConfig {

    @Value("${load.homologene}")
    private boolean loadHomologene;

    @Value("${ncbi.homologene.fileName}")
    private String homologeneFile;

    @Bean
    public AsyncFactoryBean<HomologeneService> homologeneService( GeneService geneService, TaxonService taxonService ) {
        return AsyncFactoryBean.singleton( () -> {
            HomologeneService s = new HomologeneServiceImpl( geneService, taxonService, new HomologeneNcbiFtpResource( homologeneFile ) );
            if ( loadHomologene ) {
                try {
                    s.refresh();
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
            return s;
        } );
    }
}

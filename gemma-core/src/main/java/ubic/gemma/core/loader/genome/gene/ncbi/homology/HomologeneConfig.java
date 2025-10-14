package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

@Configuration
public class HomologeneConfig {

    @Bean
    public HomologeneServiceFactory homologeneService( GeneService geneService, TaxonService taxonService ) {
        return new HomologeneServiceFactory( geneService, taxonService );
    }
}

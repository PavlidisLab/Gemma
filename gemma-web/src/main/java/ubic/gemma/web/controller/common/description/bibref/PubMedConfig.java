package ubic.gemma.web.controller.common.description.bibref;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;

@Configuration
public class PubMedConfig {

    @Bean
    public PubMedXMLFetcher pubMedXMLFetcher( @Value("${entrez.efetch.apikey}") String ncbiApiKey ) {
        return new PubMedXMLFetcher( ncbiApiKey );
    }
}

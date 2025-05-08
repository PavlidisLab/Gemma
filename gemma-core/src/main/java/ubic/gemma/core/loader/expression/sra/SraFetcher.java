package ubic.gemma.core.loader.expression.sra;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import ubic.gemma.core.loader.entrez.EntrezRetmode;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackageSet;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@CommonsLog
public class SraFetcher {

    private final SimpleRetry<IOException> retryTemplate;
    @Nullable
    private final String ncbiApiKey;

    public SraFetcher( SimpleRetryPolicy retryPolicy, @Nullable String ncbiApiKey ) {
        this.retryTemplate = new SimpleRetry<>( retryPolicy, IOException.class, SraFetcher.class.getName() );
        this.ncbiApiKey = ncbiApiKey;
    }

    /**
     * Fetch an SRA experiment.
     */
    public SraExperimentPackageSet fetch( String accession ) throws IOException {
        URL fetchUrl = EntrezUtils.fetchById( "sra", accession, EntrezRetmode.XML, "full", ncbiApiKey );
        log.info( "Fetching XML metadata for " + accession + " from " + fetchUrl + "..." );
        return retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream in = fetchUrl.openStream() ) {
                return new SraXmlParser().parse( in );
            }
        }, ncbiApiKey ), "fetching " + fetchUrl );
    }

    /**
     * Fetch an SRA experiment in the runinfo format.
     */
    public String fetchRunInfo( String accession ) throws IOException {
        URL fetchUrl = EntrezUtils.fetchById( "sra", accession, EntrezRetmode.TEXT, "runinfo", ncbiApiKey );
        return EntrezUtils.doNicely( () -> IOUtils.toString( fetchUrl, StandardCharsets.UTF_8 ), ncbiApiKey );
    }
}

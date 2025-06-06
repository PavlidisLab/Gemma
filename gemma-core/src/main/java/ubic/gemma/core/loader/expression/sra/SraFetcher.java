package ubic.gemma.core.loader.expression.sra;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import ubic.gemma.core.loader.entrez.EntrezRetmode;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackageSet;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

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
        return fetch( Collections.singletonList( accession ) );
    }

    /**
     * Fetch an SRA experiment by its GEO association.
     * <p>
     * This works for GSE and GSM accessions.
     */
    public SraExperimentPackageSet fetchByGeoAccession( String geoAccession ) throws IOException {
        String filter;
        if ( geoAccession.startsWith( "GSE" ) ) {
            filter = "gse";
        } else if ( geoAccession.startsWith( "GSM" ) ) {
            filter = "gsm";
        } else {
            throw new IllegalArgumentException( "Unrecognized GEO accession: " + geoAccession );
        }
        URL uidUrl = EntrezUtils.search( "gds", geoAccession + "[accn]" + filter + " [filter]", EntrezRetmode.XML, ncbiApiKey );
        log.debug( "Fetching UID for " + geoAccession + " from " + uidUrl + "..." );
        String uid = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = uidUrl.openStream() ) {
                Document doc = EntrezXmlUtils.parse( is );
                Collection<String> matches = EntrezXmlUtils.extractSearchIds( doc );
                if ( matches.isEmpty() ) {
                    log.warn( "Could not find a UID for " + geoAccession + ", cannot check if it has single-cell data in SRA." );
                    return null;
                } else if ( matches.size() > 1 ) {
                    throw new IllegalStateException( "Found more than one UID for " + geoAccession + ": " + matches + "." );
                }
                return matches.iterator().next();
            }
        }, ncbiApiKey ), uidUrl );
        if ( uid == null ) {
            return new SraExperimentPackageSet() {{
                setExperimentPackages( Collections.emptyList() );
            }};
        }
        URL linkUrl = EntrezUtils.linkById( "sra", "gds", uid, "neighbor", EntrezRetmode.XML, null );
        log.debug( "Fetching associated SRA entries for " + geoAccession + " from " + linkUrl + "..." );
        return retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = linkUrl.openStream() ) {
                Document doc = EntrezXmlUtils.parse( is );
                return fetch( EntrezXmlUtils.extractLinkIds( doc, "gds", "sra" ) );
            }
        }, ncbiApiKey ), linkUrl );
    }

    /**
     * TODO: batch retrieval
     */
    private SraExperimentPackageSet fetch( Collection<String> accessions ) throws IOException {
        URL fetchUrl = EntrezUtils.fetchById( "sra", String.join( ",", accessions ), EntrezRetmode.XML, "full", ncbiApiKey );
        log.debug( "Fetching XML metadata for " + String.join( ",", accessions ) + " from " + fetchUrl + "..." );
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
        return retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            return IOUtils.toString( fetchUrl, StandardCharsets.UTF_8 );
        }, ncbiApiKey ), "fetching " + fetchUrl );
    }
}

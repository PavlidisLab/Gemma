package ubic.gemma.core.loader.expression.sra;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import ubic.gemma.core.loader.entrez.EntrezQuery;
import ubic.gemma.core.loader.entrez.EntrezRetmode;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackage;
import ubic.gemma.core.loader.expression.sra.model.SraExperimentPackageSet;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@CommonsLog
public class SraFetcher {

    private static final int BATCH_SIZE = 30;
    private static final SraExperimentPackageSet EMPTY_PACKAGE_SET = new SraExperimentPackageSet() {{
        setExperimentPackages( Collections.emptyList() );
    }};

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
        if ( accession.startsWith( "SRX" ) ) {
            return fetch( Collections.singletonList( accession ) );
        } else {
            URL searchUrl = EntrezUtils.search( "sra", accession + "[accn]", EntrezRetmode.XML, ncbiApiKey );
            log.debug( "There are non-SRX accessions, performing a search with " + searchUrl + " to resolve their IDs..." );
            EntrezQuery query = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
                try ( InputStream in = searchUrl.openStream() ) {
                    return EntrezXmlUtils.getQuery( EntrezXmlUtils.parse( in ) );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }, ncbiApiKey ), "fetching " + searchUrl );
            List<SraExperimentPackage> experimentPackages = new ArrayList<>();
            for ( int i = 0; i < query.getTotalRecords(); i += BATCH_SIZE ) {
                URL fetchUrl = EntrezUtils.fetch( "sra", query, EntrezRetmode.XML, "full", i, BATCH_SIZE, ncbiApiKey );
                log.debug( "Fetching XML metadata for " + String.join( ",", accession ) + " from " + fetchUrl + "..." );
                SraExperimentPackageSet r = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
                    try ( InputStream in = fetchUrl.openStream() ) {
                        return new SraXmlParser().parse( in );
                    }
                }, ncbiApiKey ), "fetching " + fetchUrl );
                experimentPackages.addAll( r.getExperimentPackages() );
            }
            SraExperimentPackageSet result = new SraExperimentPackageSet();
            result.setExperimentPackages( experimentPackages );
            return result;
        }
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
        URL uidUrl = EntrezUtils.search( "gds", geoAccession + "[accn]" + " " + filter + "[filter]", EntrezRetmode.XML, ncbiApiKey );
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
            return EMPTY_PACKAGE_SET;
        }
        URL linkUrl = EntrezUtils.linkById( "sra", "gds", uid, "neighbor", EntrezRetmode.XML, null );
        log.debug( "Fetching associated SRA entries for " + geoAccession + " from " + linkUrl + "..." );
        Collection<String> linkIds = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = linkUrl.openStream() ) {
                Document doc = EntrezXmlUtils.parse( is );
                return EntrezXmlUtils.extractLinkIds( doc, "gds", "sra" );
            }
        }, ncbiApiKey ), linkUrl );
        if ( linkIds.isEmpty() ) {
            log.warn( "No SRA entries found for " + geoAccession + ", returning empty package set." );
            return EMPTY_PACKAGE_SET;
        } else if ( linkIds.size() <= BATCH_SIZE ) {
            return fetch( linkIds );
        } else {
            // if there are too many links, we need to fetch them in batches
            log.info( geoAccession + " has " + linkIds.size() + " SRA entries, fetching in batches of " + BATCH_SIZE + "..." );
            List<SraExperimentPackage> ep = new ArrayList<>();
            for ( List<String> batch : ListUtils.partition( new ArrayList<>( linkIds ), BATCH_SIZE ) ) {
                ep.addAll( fetch( batch ).getExperimentPackages() );
                log.info( "Fetched " + ep.size() + "/" + linkIds.size() + " SRA entries so far for " + geoAccession + "." );
            }
            SraExperimentPackageSet result = new SraExperimentPackageSet();
            result.setExperimentPackages( ep );
            return result;
        }
    }

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
        if ( accession.startsWith( "SRX" ) ) {
            URL fetchUrl = EntrezUtils.fetchById( "sra", accession, EntrezRetmode.TEXT, "runinfo", ncbiApiKey );
            return retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
                return IOUtils.toString( fetchUrl, StandardCharsets.UTF_8 );
            }, ncbiApiKey ), "fetching " + fetchUrl );
        } else {
            URL searchUrl = EntrezUtils.search( "sra", accession + "[accn]", EntrezRetmode.XML, ncbiApiKey );
            log.debug( "Requesting a non-SRX accession, performing a search with " + searchUrl + " to resolve its ID..." );
            EntrezQuery query = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
                try ( InputStream in = searchUrl.openStream() ) {
                    return EntrezXmlUtils.getQuery( EntrezXmlUtils.parse( in ) );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }, ncbiApiKey ), "fetching " + searchUrl );
            StringBuilder result = new StringBuilder();
            for ( int i = 0; i < query.getTotalRecords(); i += BATCH_SIZE ) {
                URL fetchUrl = EntrezUtils.fetch( "sra", query, EntrezRetmode.TEXT, "runinfo", i, BATCH_SIZE, ncbiApiKey );
                String s = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
                    return IOUtils.toString( fetchUrl, StandardCharsets.UTF_8 );
                }, ncbiApiKey ), "fetching " + fetchUrl );
                String[] headerAndRecords = s.split( "\n", 2 );
                // append header only once
                if ( i == 0 ) {
                    result.append( headerAndRecords[0] ).append( "\n" );
                }
                if ( headerAndRecords.length == 2 ) {
                    result.append( headerAndRecords[1] );
                }
            }
            return result.toString();
        }
    }
}

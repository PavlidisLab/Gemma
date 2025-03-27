package ubic.gemma.core.image.aba;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static ubic.gemma.core.util.XMLUtils.createDocumentBuilder;

/**
 * Provides the hooks to load data from aba
 */
class AbaLoader {

    private static final Log log = LogFactory.getLog( AbaLoader.class.getName() );

    /**
     * see
     * http://help.brain-map.org/display/api/Allen+Brain+Atlas+API and
     * http://help.brain-map.org/display/mousebrain/API
     * for documentation
     */
    private static final String API_BASE_URL = "https://api.brain-map.org/api/v2";

    /**
     * 1st parameter: Gene NCBI ID
     */
    private static final String GET_GENE_URL = "/data/Gene/query.xml?criteria=rma::criteria,[entrez_id$eq@]";

    /**
     * 1st parameter: Image ID, get through the IMAGESERIES ABA API
     * 2nd parameter: Downsampling factor
     * 3rd parameter: View type
     * - leave blank for normal picture
     * - "=expression" for expression highlight
     */
    private static final String GET_IMAGE_URL = "/image_download/@?downsample=@&view@";

    /**
     * 1st parameter: Gene NCBI ID
     * 2nd parameter: Image space
     * - 9 for coronal
     * - 10 for sagittal
     */
    private static final String GET_IMAGE_SERIES_URL = "/data/SectionDataSet/query.xml?criteria=rma::criteria,genes[entrez_id$eq@],reference_space[id$eq@],rma::include,section_images";

    private final Path cacheDir;
    private final long cacheTimeToLiveMillis;

    AbaLoader( Path cacheDir, long cacheTimeToLiveMillis ) {
        Assert.notNull( cacheDir, "A cache directory must be set." );
        Assert.isTrue( cacheTimeToLiveMillis >= 0, "TTL must be zero or greater." );
        this.cacheDir = cacheDir;
        this.cacheTimeToLiveMillis = cacheTimeToLiveMillis;
    }

    String getGeneUrl( Gene gene ) {
        String[] args = { gene.getNcbiGeneId().toString() };
        return this.buildUrlString( AbaLoader.GET_GENE_URL, args );
    }

    String getImageUrl( int id ) {
        String[] args = { String.valueOf( id ), "5", "" };
        return this.buildUrlString( AbaLoader.GET_IMAGE_URL, args );
    }

    @Nullable
    Document getAbaGeneXML( Gene gene ) {
        try {
            return this.getDocument( gene, "gene" );
        } catch ( IOException e ) {
            log.error( "An I/O error occurred while querying metadata for " + gene + ".", e );
            return null;
        }
    }

    @Nullable
    Document getAbaGeneSagittalImages( Gene gene ) {
        try {
            return this.getDocument( gene, "sagittal" );
        } catch ( IOException e ) {
            log.error( "An I/O error occurred while querying sagittal images for " + gene + ".", e );
            return null;
        }
    }

    private Document getDocument( Gene gene, String type ) throws IOException {
        Path outputFile = this.getFile( type + "_" + gene.getNcbiGeneId().toString() );
        if ( !Files.exists( outputFile ) || isStale( outputFile ) ) {
            try ( OutputStream out = Files.newOutputStream( outputFile ) ) {
                String url;
                switch ( type ) {
                    case "gene":
                        String[] args = { gene.getNcbiGeneId().toString() };
                        url = this.buildUrlString( AbaLoader.GET_GENE_URL, args );
                        break;
                    case "sagittal":
                        String[] args1 = { gene.getNcbiGeneId().toString(), "10" };
                        url = this.buildUrlString( AbaLoader.GET_IMAGE_SERIES_URL, args1 );
                        break;
                    default:
                        throw new IllegalArgumentException( "Invalid type: " + type );
                }
                IOUtils.copy( new URL( url ), out );
            }
        }
        try ( InputStream input = Files.newInputStream( outputFile ) ) {
            return createDocumentBuilder().parse( input );
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }
    }

    private Path getFile( String fileName ) throws IOException {
        Files.createDirectories( cacheDir );
        return cacheDir.resolve( "aba_" + fileName + ".xml" );
    }

    private boolean isStale( Path outputFile ) throws IOException {
        return Files.getLastModifiedTime( outputFile ).toMillis() < System.currentTimeMillis() - cacheTimeToLiveMillis;
    }

    /**
     * Given a predefined URL (one of the constants declared in the AllenBrainAtlasService) and a list of arguments will
     * return the correct REST URL for the desired method call
     *
     * @param urlPattern url pattern
     * @param args       arguments
     * @return full url
     */
    private String buildUrlString( String urlPattern, String[] args ) {
        for ( String arg : args ) {
            urlPattern = urlPattern.replaceFirst( "@", arg );
        }
        return AbaLoader.API_BASE_URL + urlPattern;
    }
}

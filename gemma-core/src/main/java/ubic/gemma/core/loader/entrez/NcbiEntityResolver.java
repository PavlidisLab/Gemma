package ubic.gemma.core.loader.entrez;

import lombok.extern.apachecommons.CommonsLog;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.InputStream;

/**
 * Resolve a bunch of NCBI DTDs in the classpath.
 * @author poirigui
 */
@CommonsLog
public class NcbiEntityResolver implements EntityResolver {

    private final String[] NAMESPACES = new String[] {
            "http://www.ncbi.nlm.nih.gov/entrez/query/DTD/",
            "https://eutils.ncbi.nlm.nih.gov/eutils/dtd/",
            "https://dtd.nlm.nih.gov/ncbi/pubmed/out/"
    };

    @Override
    public InputSource resolveEntity( String publicId, String systemId ) {
        for ( String namespace : NAMESPACES ) {
            if ( systemId.startsWith( namespace ) ) {
                InputStream is = getClass().getResourceAsStream( "/ubic/gemma/core/loader/dtd/" + systemId.substring( namespace.length() ) );
                if ( is != null ) {
                    InputSource source = new InputSource( is );
                    source.setPublicId( publicId );
                    source.setSystemId( systemId );
                    return source;
                }
            }
        }
        log.warn( String.format( "Could not find a schema for %s %s in the classpath.", publicId, systemId ) );
        return null;
    }
}

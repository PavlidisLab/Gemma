package ubic.gemma.core.loader.entrez;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolve a bunch of NCBI DTDs in the classpath.
 * @author poirigui
 */
@CommonsLog
class NcbiEntityResolver implements EntityResolver {

    private static final Map<String, byte[]> dtdCache = new WeakHashMap<>();

    private final String[] NAMESPACES = new String[] {
            "http://www.ncbi.nlm.nih.gov/entrez/query/DTD/",
            "https://eutils.ncbi.nlm.nih.gov/eutils/dtd/",
            "https://dtd.nlm.nih.gov/ncbi/pubmed/out/"
    };

    @Override
    public synchronized InputSource resolveEntity( String publicId, String systemId ) throws IOException {
        if ( dtdCache.containsKey( systemId ) ) {
            return createInputSource( publicId, systemId );
        }
        for ( String namespace : NAMESPACES ) {
            if ( systemId.startsWith( namespace ) ) {
                try ( InputStream is = getClass().getResourceAsStream( "/ubic/gemma/core/loader/dtd/" + systemId.substring( namespace.length() ) ) ) {
                    if ( is != null ) {
                        dtdCache.put( systemId, IOUtils.toByteArray( is ) );
                        return createInputSource( publicId, systemId );
                    }
                }
            }
        }
        log.warn( String.format( "Could not find a schema for %s %s in the classpath.", publicId, systemId ) );
        return null;
    }

    private InputSource createInputSource( String publicId, String systemId ) {
        InputSource source = new InputSource( new ByteArrayInputStream( dtdCache.get( systemId ) ) );
        source.setPublicId( publicId );
        source.setSystemId( systemId );
        return source;
    }
}

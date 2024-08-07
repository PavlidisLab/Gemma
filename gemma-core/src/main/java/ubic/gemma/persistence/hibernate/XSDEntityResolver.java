package ubic.gemma.persistence.hibernate;

import org.hibernate.internal.util.xml.DTDEntityResolver;
import org.xml.sax.InputSource;

import java.io.InputStream;

/**
 * Resolves Hibernate XSD schemas from the classpath.
 * @see org.hibernate.internal.util.xml.DTDEntityResolver
 * @author poirigui
 */
public class XSDEntityResolver extends DTDEntityResolver {

    private static final String HIBERNATE_NAMESPACE = "http://hibernate.org/xsd/";

    @Override
    public InputSource resolveEntity( String publicId, String systemId ) {
        if ( systemId.startsWith( HIBERNATE_NAMESPACE ) ) {
            InputStream is = XSDEntityResolver.class.getResourceAsStream( "/org/hibernate/" + systemId.substring( HIBERNATE_NAMESPACE.length() ) );
            InputSource inputSource = new InputSource( is );
            inputSource.setPublicId( publicId );
            inputSource.setSystemId( systemId );
            return inputSource;
        }
        return super.resolveEntity( publicId, systemId );
    }
}

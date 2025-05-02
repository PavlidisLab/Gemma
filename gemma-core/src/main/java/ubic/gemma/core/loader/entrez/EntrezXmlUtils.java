package ubic.gemma.core.loader.entrez;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.XMLUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Utilities for manipulating Entrez XML responses.
 * @author poirigui
 */
public class EntrezXmlUtils {

    /**
     * Parse an XML reply from Entrez.
     * <p>
     * This will check if there are any {@code ERROR} tags.
     */
    public static Document parse( InputStream is ) throws IOException {
        try {
            DocumentBuilder builder = createDocumentBuilder();
            Document doc = builder.parse( is );
            checkForErrors( doc );
            return doc;
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Parse an XML reply with a specific character encoding.
     * <p>
     * This is mainly useful for querying old MINiML documents from NCBI FTP server that are declared as UTF-8 but
     * actually use Windows-1252 encoding.
     */
    public static Document parse( InputStream is, String encoding ) throws IOException {
        try {
            DocumentBuilder builder = createDocumentBuilder();
            InputSource inputSource = new InputSource( is );
            inputSource.setEncoding( encoding );
            Document doc = builder.parse( inputSource );
            checkForErrors( doc );
            return doc;
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }
    }

    private static void checkForErrors( Document doc ) {
        NodeList error = doc.getDocumentElement().getElementsByTagName( "ERROR" );
        if ( error.item( 0 ) != null ) {
            List<String> errors = new ArrayList<>();
            for ( Node elem = error.item( 0 ); elem != null; elem = elem.getNextSibling() ) {
                errors.add( XMLUtils.getTextValue( elem ) );
            }
            throw new EntrezException( errors.get( 0 ), errors );
        }
    }

    /**
     * Create a document builder with {@link NcbiEntityResolver} as entity resolver.
     * <p>
     * This will work for most of not all XML files from NCBI Entrez and related services.
     */
    private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder builder = ubic.gemma.core.util.XMLUtils.createDocumentBuilder();
        builder.setEntityResolver( new NcbiEntityResolver() );
        return builder;
    }

    public static EntrezQuery getQuery( Document doc ) {
        return new EntrezQuery( getQueryId( doc ), getCookie( doc ), getCount( doc ) );
    }

    public static int getCount( Document document ) {
        return Integer.parseInt( XMLUtils.getTextValue( document.getElementsByTagName( "Count" ).item( 0 ) ) );
    }

    public static String getQueryId( Document document ) {
        return XMLUtils.getTextValue( XMLUtils.getUniqueItem( document.getElementsByTagName( "QueryKey" ) ) );
    }

    public static String getCookie( Document document ) {
        return XMLUtils.getTextValue( XMLUtils.getUniqueItem( document.getElementsByTagName( "WebEnv" ) ) );
    }

    public static Collection<String> extractIds( Document doc ) {
        NodeList idList = doc.getElementsByTagName( "Id" );
        Collection<String> result = new HashSet<>();
        for ( Node elem = idList.item( 0 ); elem != null; elem = elem.getNextSibling() ) {
            String val = XMLUtils.getTextValue( elem );
            if ( StringUtils.isBlank( val ) ) {
                continue;
            }
            result.add( val );
        }
        return result;
    }
}

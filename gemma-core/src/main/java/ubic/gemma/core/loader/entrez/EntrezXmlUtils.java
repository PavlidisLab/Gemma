package ubic.gemma.core.loader.entrez;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.XMLUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;
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

    private static final XPathExpression xCount = XMLUtils.compile( "/eSearchResult/Count" );
    private static final XPathExpression xQueryKey = XMLUtils.compile( "/eSearchResult/QueryKey" );
    private static final XPathExpression xCookie = XMLUtils.compile( "/eSearchResult/WebEnv" );
    private static final XPathExpression xSearchId = XMLUtils.compile( "/eSearchResult/IdList/Id" );
    private static final XPathExpression xFetchId = XMLUtils.compile( "/IdList/Id" );

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
            for ( int i = 0; i < error.getLength(); i++ ) {
                errors.add( XMLUtils.getTextValue( error.item( i ) ) );
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
        return Integer.parseInt( XMLUtils.evaluateToString( xCount, document ) );
    }

    public static String getQueryId( Document document ) {
        return XMLUtils.evaluateToString( xQueryKey, document );
    }

    public static String getCookie( Document document ) {
        return XMLUtils.evaluateToString( xCookie, document );
    }

    /**
     * Extract IDs from an ESearch call.
     * @see EntrezUtils#search(String, EntrezQuery, EntrezRetmode, int, int, String)
     */
    public static Collection<String> extractSearchIds( Document doc ) {
        NodeList idList = XMLUtils.evaluate( xSearchId, doc );
        Collection<String> result = new HashSet<>();
        for ( int i = 0; i < idList.getLength(); i++ ) {
            result.add( idList.item( i ).getTextContent() );
        }
        return result;
    }

    /**
     * Extract IDs from an EFetch call.
     * <p>
     * The {@code uilist} return type must be used in the call.
     * @see EntrezUtils#fetch(String, EntrezQuery, EntrezRetmode, String, int, int, String)
     * @see EntrezUtils#fetchById(String, String, EntrezRetmode, String, String)
     */
    public static Collection<String> extractFetchIds( Document doc ) {
        NodeList idList = XMLUtils.evaluate( xFetchId, doc );
        Collection<String> result = new HashSet<>();
        for ( int i = 0; i < idList.getLength(); i++ ) {
            result.add( idList.item( i ).getTextContent() );
        }
        return result;
    }

    /**
     * Extract IDs from an ELink call.
     * @see EntrezUtils#linkById(String, String, EntrezQuery, String, EntrezRetmode, String)
     * @see EntrezUtils#linkById(String, String, String, String, EntrezRetmode, String)
     */
    public static Collection<String> extractLinkIds( Document doc, String dbfrom, String dbto ) {
        XPathExpression xId = XMLUtils.compile( "/eLinkResult/LinkSet[DbFrom/text()='" + dbfrom + "']/LinkSetDb[DbTo/text()='" + dbto + "']/Link/Id" );
        Collection<String> ids2 = new ArrayList<>();
        NodeList nl = XMLUtils.evaluate( xId, doc );
        for ( int i = 0; i < nl.getLength(); i++ ) {
            ids2.add( nl.item( i ).getTextContent() );
        }
        return ids2;
    }
}

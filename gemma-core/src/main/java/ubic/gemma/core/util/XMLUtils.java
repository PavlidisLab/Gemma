/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Handy methods for dealing with XML.
 *
 * @author pavlidis
 */
// Possible external use
public class XMLUtils {

    private static final Log log = LogFactory.getLog( XMLUtils.class );

    /**
     * Create a new DocumentBuilder with some good presets for Gemma.
     * <p>
     * For security reasons (and also performance), the returned DocumentBuilder is not capable of resolving entities.
     * If you need to resolve DTDs or XSD schemas, you must implement an {@link org.xml.sax.EntityResolver}.
     */
    public static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        factory.setValidating( false );
        DocumentBuilder builder = factory.newDocumentBuilder();
        // the default behavior is to retrieve DTDs from the web, which is extremely dangerous (and slow)
        builder.setEntityResolver( ( systemId, publicId ) -> {
            throw new RuntimeException( "No entity resolver has been set." );
        } );
        return builder;
    }

    public static List<String> extractMultipleChildren( Node parent, String elementName ) {
        List<String> r = new ArrayList<>();

        NodeList jNodes = parent.getChildNodes();
        for ( int q = 0; q < jNodes.getLength(); q++ ) {
            Node jitem = jNodes.item( q );
            if ( !( jitem instanceof Element ) ) {
                continue;
            }
            if ( jitem.getNodeName().equals( elementName ) ) {
                r.add( XMLUtils.getTextValue( jitem ) );
            }
        }
        return r;
    }

    public static String extractOneChildText( Node parent, String elementName ) {
        NodeList jNodes = parent.getChildNodes();
        for ( int q = 0; q < jNodes.getLength(); q++ ) {
            Node jitem = jNodes.item( q );
            if ( !( jitem instanceof Element ) ) {
                continue;
            }
            if ( jitem.getNodeName().equals( elementName ) ) {
                return XMLUtils.getTextValue( jitem );
            }
        }
        return null;
    }

    public static Node extractOneChild( Node parent, String elementName ) {
        NodeList jNodes = parent.getChildNodes();
        for ( int q = 0; q < jNodes.getLength(); q++ ) {
            Node jitem = requireNonNull( jNodes.item( q ) );
            if ( jitem.getNodeName().equals( elementName ) ) {
                return jitem;
            }
        }
        return null;
    }

    /**
     * @param doc - the xml document to search through
     * @param tag -the name of the element we are looking for
     * @return a collection of strings that represent all the data contained within the given tag (for each instance of
     * that tag)
     */
    public static Collection<String> extractTagData( Document doc, String tag ) {
        Collection<String> result = new HashSet<>();
        if ( doc == null )
            return result;
        NodeList idList = doc.getElementsByTagName( tag );
        assert idList != null;
        XMLUtils.log.debug( "Got " + idList.getLength() );
        // NodeList idNodes = idList.item( 0 ).getChildNodes();
        // Node ids = idList.item( 0 );
        for ( int i = 0; i < idList.getLength(); i++ ) {
            Node item = idList.item( i );
            String value = XMLUtils.getTextValue( item );
            XMLUtils.log.debug( "Got " + value );
            result.add( value );
        }

        return result;
    }

    /**
     * Obtain an item from a {@link NodeList} at a given index.
     */
    public static Node getItem( NodeList nodeList, int i ) {
        return requireNonNull( nodeList.item( i ) );
    }

    /**
     * Obtain a single item from a {@link NodeList}.
     */
    public static Node getUniqueItem( NodeList nodeList ) {
        // nodelist is a linked list, so getting the size is O(n)
        if ( nodeList.item( 1 ) != null ) {
            throw new IllegalStateException( "Expected only one item, got " + nodeList.getLength() );
        }
        return requireNonNull( nodeList.item( 0 ) );
    }

    /**
     * Make the horrible DOM API slightly more bearable: get the text value we know this element contains.
     * Borrowed from the Spring API.
     * Using Node.getTextContent() to fix failing tests, if there is a problem, see history before Feb 22. 2018
     * Also, this is the previous note (related to the code I removed):
     * * Note that we can't really use the alternative Node.getTextContent() because it isn't supported by older Xerces
     * * implementations (1.x), which tend to leak into the classloader. Causes recurring problems with tests.
     *
     * @param ele element
     * @return text value
     */
    public static String getTextValue( Node ele ) {
        if ( ele == null )
            return null;
        StringBuilder value = new StringBuilder();
        NodeList nl = ele.getChildNodes();
        for ( int i = 0; i < nl.getLength(); i++ ) {
            Node node = requireNonNull( nl.item( i ) );
            if ( node.getTextContent() != null ) {
                value.append( StringUtils.strip( node.getTextContent() ) );
            }
        }
        return value.toString();
    }

    /**
     * Evaluate an XPath expression that produces a {@link NodeList}.
     */
    public static NodeList evaluate( XPathExpression xpath, Node item ) {
        try {
            return ( NodeList ) xpath.evaluate( item, XPathConstants.NODESET );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( e );
        }
    }
}

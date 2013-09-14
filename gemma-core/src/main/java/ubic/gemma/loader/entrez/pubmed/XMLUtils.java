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
package ubic.gemma.loader.entrez.pubmed;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Handy methods for dealing with XML.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class XMLUtils {

    protected static final Log log = LogFactory.getLog( XMLUtils.class );

    /**
     * When there are multiple elements of the same type.
     * 
     * @param parent
     * @param elementName
     * @return
     * @throws IOException
     */
    public static List<String> extractMultipleChildren( Node parent, String elementName ) throws IOException {
        List<String> r = new ArrayList<String>();

        NodeList jNodes = parent.getChildNodes();
        for ( int q = 0; q < jNodes.getLength(); q++ ) {
            Node jitem = jNodes.item( q );
            if ( !( jitem instanceof Element ) ) {
                continue;
            }
            if ( jitem.getNodeName().equals( elementName ) ) {
                r.add( getTextValue( ( Element ) jitem ) );
            }
        }
        return r;
    }

    /**
     * Useful if you only have a single child you care about.
     * 
     * @param parent
     * @param elementName
     * @return String value of the child.
     * @throws IOException
     * @throws TransformerException
     */
    public static String extractOneChild( Node parent, String elementName ) throws IOException {
        NodeList jNodes = parent.getChildNodes();
        for ( int q = 0; q < jNodes.getLength(); q++ ) {
            Node jitem = jNodes.item( q );
            if ( !( jitem instanceof Element ) ) {
                continue;
            }
            if ( jitem.getNodeName().equals( elementName ) ) {
                return getTextValue( ( Element ) jitem );
            }
        }
        return null;
    }

    /**
     * @param doc - the xml document to search through
     * @param tag -the name of the element we are looking for
     * @return a collection of strings that represent all the data contained within the given tag (for each instance of
     *         that tag)
     */
    public static Collection<String> extractTagData( Document doc, String tag ) {
        Collection<String> result = new HashSet<String>();
        if ( doc == null ) return result;
        NodeList idList = doc.getElementsByTagName( tag );
        assert idList != null;
        log.debug( "Got " + idList.getLength() );
        // NodeList idNodes = idList.item( 0 ).getChildNodes();
        // Node ids = idList.item( 0 );
        try {
            for ( int i = 0; i < idList.getLength(); i++ ) {
                Node item = idList.item( i );
                String value = XMLUtils.getTextValue( ( Element ) item );
                log.debug( "Got " + value );
                result.add( value );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return result;
    }

    /**
     * Make the horrible DOM API slightly more bearable: get the text value we know this element contains.
     * <p>
     * Borrowed from the Spring API.
     * <p>
     * Note that we can't really use the alternative Node.getTextContent() because it isn't supported by older Xerces
     * implementations (1.x), which tend to leak into the classloader. Causes recurring problems with tests.
     * 
     * @throws IOException
     */
    public static String getTextValue( org.w3c.dom.Element ele ) throws IOException {
        if ( ele == null ) return null;
        StringBuilder value = new StringBuilder();
        org.w3c.dom.NodeList nl = ele.getChildNodes();
        for ( int i = 0; i < nl.getLength(); i++ ) {
            org.w3c.dom.Node item = nl.item( i );
            if ( item instanceof org.w3c.dom.CharacterData ) {
                if ( !( item instanceof org.w3c.dom.Comment ) ) {
                    value.append( item.getNodeValue() );
                }
            } else {
                throw new IOException( "element is just allowed to have text and comment nodes, not: "
                        + item.getClass().getName() );
            }
        }
        return value.toString();
    }

    /**
     * @param is
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document openAndParse( InputStream is ) throws IOException, ParserConfigurationException,
            SAXException {
        if ( is.available() == 0 ) {
            throw new IOException( "XML stream contains no data." );
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        // factory.setValidating( true );

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse( is );
        return document;
    }

}

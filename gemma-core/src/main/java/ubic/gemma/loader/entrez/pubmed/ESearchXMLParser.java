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
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ESearchXMLParser {

    protected static final Log log = LogFactory.getLog( ESearchXMLParser.class );

    /**
     * @param is
     * @return collection of identifiers retrieved.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public Collection<String> parse( InputStream is ) throws IOException, ParserConfigurationException, SAXException {
        Document document = openAndParse( is );
        return extractIds( document );
    }

    /**
     * @param is
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private Document openAndParse( InputStream is ) throws IOException, ParserConfigurationException, SAXException {
        //        if ( is.available() == 0 ) {
        //            throw new IOException( "XML stream contains no data." );
        //        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        // factory.setValidating( true );

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse( is );
        return document;
    }

    public int getCount( InputStream is ) throws IOException, ParserConfigurationException, SAXException {
        Document document = openAndParse( is );
        NodeList idList = document.getElementsByTagName( "Count" );
        Node item = idList.item( 0 );
        String value = XMLUtils.getTextValue( ( Element ) item );
        log.debug( "Got " + value );
        return Integer.parseInt( value );
    }

    /**
     * @param document
     * @return
     */
    private Collection<String> extractIds( Document doc ) {
        Collection<String> result = new HashSet<String>();
        NodeList idList = doc.getElementsByTagName( "Id" );
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

}

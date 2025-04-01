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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.XMLUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static ubic.gemma.core.loader.entrez.NcbiXmlUtils.createDocumentBuilder;

/**
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ESearchXMLParser {

    private static final Log log = LogFactory.getLog( ESearchXMLParser.class );

    public Collection<String> parse( InputStream is ) throws IOException, ParserConfigurationException, SAXException, ESearchException {
        Document document = this.openAndParse( is );
        return this.extractIds( document );
    }

    public int getCount( InputStream is ) throws IOException, ParserConfigurationException, SAXException, ESearchException {
        Document document = this.openAndParse( is );
        NodeList idList = document.getElementsByTagName( "Count" );
        if ( idList.getLength() < 1 ) {
            return 0;
        }
        Node item = idList.item( 0 );
        String value = XMLUtils.getTextValue( ( Element ) item );
        ESearchXMLParser.log.debug( "Got " + value );
        return Integer.parseInt( value );
    }

    private Document openAndParse( InputStream is ) throws IOException, ParserConfigurationException, SAXException, ESearchException {
        DocumentBuilder builder = createDocumentBuilder();
        Document doc = builder.parse( is );
        NodeList error = doc.getDocumentElement().getElementsByTagName( "ERROR" );
        if ( error.getLength() > 0 ) {
            throw new ESearchException( error.item( 0 ).getTextContent() );
        }
        return doc;
    }

    private Collection<String> extractIds( Document doc ) {
        Collection<String> result = new HashSet<>();
        NodeList idList = doc.getElementsByTagName( "Id" );
        assert idList != null;
        ESearchXMLParser.log.debug( "Got " + idList.getLength() );

        for ( int i = 0; i < idList.getLength(); i++ ) {
            Node item = idList.item( i );
            String value = XMLUtils.getTextValue( ( Element ) item );
            ESearchXMLParser.log.debug( "Got " + value );
            result.add( value );
        }

        return result;
    }

}

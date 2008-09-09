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

import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handy methods for dealing with XML.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class XMLUtils {

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
}

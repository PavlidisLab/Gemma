/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.springframework.util.Assert;
import org.springframework.ws.server.endpoint.AbstractDomPayloadEndpoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ubic.gemma.security.authentication.ManualAuthenticationProcessing;
import ubic.gemma.util.ConfigUtils;

/**
 * @author gavin, klc Abstracts out the security and a few constants.
 * @version$Id$
 */

public abstract class AbstractGemmaEndpoint extends AbstractDomPayloadEndpoint {

    protected ManualAuthenticationProcessing manualAuthenticationProcessing;

    /**
     * Namespace of both request and response.
     */
    public static final String NAMESPACE_URI = "http://bioinformatics.ubc.ca/Gemma/ws";

    private static Log log = LogFactory.getLog( AbstractGemmaEndpoint.class );

    private static final String USER = "guest";

    private static final String PASSWORD = "";

    protected static final String REQUEST = "Request";

    protected static final String RESPONSE = "Response";

    private String localName;

    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    
    public AbstractGemmaEndpoint() {
        super();

    }

    public void setManualAuthenticationProcessing( ManualAuthenticationProcessing map ) {
        this.manualAuthenticationProcessing = map;

    }

    protected boolean authenticate() {
        this.manualAuthenticationProcessing.anonymousAuthentication();
        return true;
    }

    protected void setLocalName( String localName ) {
        this.localName = localName;
    }

    /**
     * Function that handles the retrieval of xml input. Use this method if there is only one value in the input but
     * generically, this method can also store multiple input values as well. This will depend on how the xml is parsed
     * by the client. TODO Still need to test on different types of client requests.
     * 
     * @param requestElement - xml request in node hierarchy
     * @param document -
     * @param tagName
     * @return a collection contain one string element
     */
    protected Collection<String> getNodeValues( Element requestElement, String tagName ) {
        StopWatch watch = new StopWatch();
        watch.start();

        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        log.info( "Starting " + localName + " endpoint" );
        authenticate();

        Collection<String> value = new HashSet<String>();
        String node = "";

        // get the Element with name = tagName
        NodeList children = requestElement.getElementsByTagName( tagName ).item( 0 ).getChildNodes();
        // iterate over the child nodes
        for ( int i = 0; i < children.getLength(); i++ ) {

            if ( children.item( i ).getNodeType() == Node.TEXT_NODE ) {
                node = children.item( i ).getNodeValue();
                // if (StringUtils.isNotEmpty(node)
                // && StringUtils.isNumeric(node)) {
                // break;
                // }
                value.add( node );

            }
            node = null;
        }

        if ( value == null ) {
            throw new IllegalArgumentException( "Could not find request text node" );
        }
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML request for " + localName + " endpoint read in " + time + "ms." );
        return value;
    }

    /**
     * A method written for array input from MATLAB clients. The more generic method to use is getNodeValues(). Column
     * Arrays and Horizontal Arrays from MATLAB both work, but it must be passed in directly (i.e. EEArray.ee_ids)
     * 
     * @param requestElement
     * @param document
     * @param tagName
     * @return
     */
    protected Collection<String> getArrayValues( Element requestElement, String tagName ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        log.info( "Starting " + localName + " endpoint" );
        authenticate();

        Collection<String> value = new HashSet<String>();
        String node = "";

        // get the Element with name = tagName
        // NodeList children =
        // requestElement.getElementsByTagName(tagName).item(0).getChildNodes().item(1).getChildNodes();

        NodeList children = requestElement.getElementsByTagName( tagName ).item( 0 ).getChildNodes();

        // iterate over the child nodes
        // but it appears that MATLAB encodes it so that every odd (ie. 1, 3, 5, 7, etc) great-grandchild holds the
        // array value
        for ( int i = 1; i < children.getLength(); i += 2 ) {

            // need to go one more level down into the great-grandchildren
            Node child = children.item( i ).getChildNodes().item( 0 );
            // Node child = children.item(i).getFirstChild();

            if ( child.getNodeType() == Node.TEXT_NODE ) {
                node = child.getNodeValue();
                // if (StringUtils.isNotEmpty(node)
                // && StringUtils.isNumeric(node)) {
                // break;
                // }
                value.add( node );

            }
            node = "";
        }

        if ( value == null ) {
            throw new IllegalArgumentException( "Could not find request text node" );
        }
        watch.stop();
        Long time = watch.getTime();
        log.info( "XML request for " + localName + " endpoint read in " + time + "ms." );
        return value;
    }

    /**
     * Function to handle the constructing of output in xml format for returning the response to the client. Use this
     * method for simple value returns such as single value or a single array of values. building Mapped values is not
     * supported with this method. If values being passed in are null or contain no values, then a string msg will be
     * returned
     * 
     * @param document
     * @param values - a collection of the values (in String format) to be returned to the client
     * @param elementName
     * @return
     */
    protected Element buildWrapper( Document document, Collection<String> values, String elementName ) {
        log.info( "Building " + localName + " XML response" );
        StopWatch watch = new StopWatch();
        watch.start();

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, localName );
        Element responseElement = document.createElementNS( NAMESPACE_URI, localName + RESPONSE );
        responseWrapper.appendChild( responseElement );

        if ( values == null || values.isEmpty() )
            responseElement.appendChild( document.createTextNode( "No " + elementName + " result" ) );
        else {
            // Need to create a list (array) of the geneIds
            for ( String value : values ) {
                Element e = document.createElement( elementName );
                e.appendChild( document.createTextNode( value ) );
                responseElement.appendChild( e );
            }
        }
        watch.stop();
        Long time = watch.getTime();
        log.info( "Finished generating result. Sending response to client." );
        log.info( "XML response for " + localName + " endpoint built in " + time + "ms." );
        return responseWrapper;
    }

    /**
     * Function to handle construction of output in xml for a bad response
     */
    protected Element buildBadResponse( Document document, String msg ) {
        log.info( "Building " + localName + " XML response" );
        StopWatch watch = new StopWatch();
        watch.start();

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, localName );
        Element responseElement = document.createElementNS( NAMESPACE_URI, localName + RESPONSE );
        responseWrapper.appendChild( responseElement );

        responseElement.appendChild( document.createTextNode( msg ) );

        log.error( msg );

        watch.stop();
        Long time = watch.getTime();

        log.info( "XML response for " + localName + " endpoint built in " + time + "ms." );
        return responseWrapper;
    }

    /**
     * @param data
     * @return a string delimited representation of the objects array passed in.
     */
    protected String encode( Object[] data ) {
        String DELIMITER = " ";
        StringBuffer result = new StringBuffer();

        for ( int i = 0; i < data.length; i++ ) {
            if ( i == 0 )
                result.append( data[i] );
            else
                result.append( DELIMITER + data[i] );
        }

        return result.toString();
    }


    /**
     * This method should/can only be used when the wrapper is manually built in the specific endpoints (ie. not using
     * the buildWrapper() in AbstractGemmaEndpoint).
     * 
     * @param responseWrapper - Manually built wrapper
     * @param reportType - directory of the report to store; the dir must exist for report to be written
     * @param filename - no xml extension is required
     */
    protected void writeReport( Element responseWrapper, Document document, String filename ) {
        String fullFileName = filename + ".xml";
        String path = HOME_DIR + File.separatorChar + "dataFiles" + File.separatorChar + "xml" +File.separatorChar;
        try {
            File file = new File( path, fullFileName );

            if ( !file.exists() ) {
                FileOutputStream out = new FileOutputStream( new File( path + fullFileName ) );
                OutputFormat format = new OutputFormat( document );
                format.setIndenting( true );
                // to generate a file output use fileoutputstream
                XMLSerializer serializer = new XMLSerializer( out, null );
                serializer.serialize( responseWrapper );

                log.info( "A report with the filename, " + fullFileName + ", has been created in path, " + path );
            } else
                log.info( "A report with the filename, " + fullFileName
                        + ", already exists.  A new report was not created." );

        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}


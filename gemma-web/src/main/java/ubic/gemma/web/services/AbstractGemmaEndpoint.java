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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.xml.sax.SAXException;

import ubic.gemma.security.authentication.ManualAuthenticationService;
import ubic.gemma.util.Settings;

/**
 * Abstracts out the security and a few constants.
 * 
 * @author gavin, klc
 * @version $Id$
 */
public abstract class AbstractGemmaEndpoint extends AbstractDomPayloadEndpoint {

    protected ManualAuthenticationService manualAuthenticationService;

    /**
     * Namespace of both request and response.
     */
    public static final String NAMESPACE_URI = "http://chibi.ubc.ca/Gemma/ws";

    private static Log log = LogFactory.getLog( AbstractGemmaEndpoint.class );

    protected static final String REQUEST = "Request";

    protected static final String RESPONSE = "Response";

    public static final String DELIMITER = " ";

    private String localName;

    private String HOME_DIR = Settings.getString( "gemma.appdata.home" );

    public AbstractGemmaEndpoint() {
        super();

    }

    public void setManualAuthenticationService( ManualAuthenticationService map ) {
        this.manualAuthenticationService = map;

    }

    protected boolean authenticate() {
        this.manualAuthenticationService.authenticateAnonymously();
        return true;
    }

    /**
     * Function to handle construction of output in xml for a bad response
     */
    protected Element buildBadResponse( Document document, String msg ) {
        Element responseWrapper = document.createElementNS( NAMESPACE_URI, localName );
        Element responseElement = document.createElementNS( NAMESPACE_URI, localName + RESPONSE );
        responseWrapper.appendChild( responseElement );

        responseElement.appendChild( document.createTextNode( msg ) );

        log.warn( localName + ": " + msg );
        return responseWrapper;
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
        return responseWrapper;
    }

    /**
     * @param data
     * @return a string delimited representation of the objects array passed in.
     */
    protected String encode( Object[] data ) {
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
     * A method written for array input from MATLAB clients. A more generic method to use is getNodeValues(). Column
     * Arrays and Horizontal Arrays from MATLAB both work, but it must be passed in directly (i.e. EEArray.ee_ids)
     * 
     * @param requestElement
     * @param document
     * @param tagName
     * @return
     */
    protected Collection<String> getArrayValues( Element requestElement, String tagName ) {
        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        authenticate();

        Collection<String> value = new HashSet<String>();
        String node = "";
        NodeList children = requestElement.getElementsByTagName( tagName ).item( 0 ).getChildNodes();

        // generic clients
        // iterate over the child nodes
        for ( int i = 0; i < children.getLength(); i++ ) {
            // need to go one more level down into the great-grandchildren
            Node child = children.item( i ).getChildNodes().item( 0 );
            // new check to see if the request is a Matlab one
            // Matlab seems to package the xml such that values are found in every odd (ie. 1, 3, 5, 7, etc)
            // great-grandchild. If at i=0, there is no value, then it IS a Matlab request.
            if ( i == 0 && child == null ) break;
            if ( child.getNodeType() == Node.TEXT_NODE ) {
                node = child.getNodeValue();
                value.add( node );
            }
            node = "";
        }

        if ( !value.isEmpty() ) return value;

        // MATLAB specific
        // but it appears that MATLAB encodes it so that every odd (ie. 1, 3, 5, 7, etc) great-grandchild holds the
        // array value
        value = new HashSet<String>();
        node = "";

        for ( int i = 1; i < children.getLength(); i = i + 2 ) {

            // need to go one more level down into the great-grandchildren
            Node child = children.item( i ).getChildNodes().item( 0 );
            // Node child = children.item(i).getFirstChild();

            if ( child.getNodeType() == Node.TEXT_NODE ) {
                node = child.getNodeValue();
                value.add( node );
            }
            node = null;
        }

        if ( value.isEmpty() ) {
            throw new IllegalArgumentException( "Could not find request text node" );
        }

        return value;

    }

    /**
     * basically Delegates to getSingleNodeValue and returns the just the last value.
     * 
     * @param requestElement
     * @param tagName
     * @return
     */
    protected String getLastSingleNodeValue( Element requestElement, String tagName ) {
        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        authenticate();
        String lastValue = null;
        String node = "";
        // get the Element with name = tagName
        NodeList children = requestElement.getElementsByTagName( tagName ).item( 0 ).getChildNodes();
        // iterate over the child nodes
        for ( int i = 0; i < children.getLength(); i++ ) {

            if ( children.item( i ).getNodeType() == Node.TEXT_NODE ) {
                node = children.item( i ).getNodeValue();
                lastValue = node;
            }
            node = null;
        }
        if ( lastValue == null || lastValue.isEmpty() ) {
            // throw new IllegalArgumentException( "Could not find request text node" );
        }
        return lastValue;
    }

    protected String getNodeValue( Element requestElement, String tagName ) {
        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        authenticate();

        Node node = requestElement.getElementsByTagName( tagName ).item( 0 );
        String value = node.getNodeValue();// .getNodeValue();

        return value;
    }

    protected String getOptionalNodeValue( Element requestElement, String tagName ) {
        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        authenticate();

        Node node = requestElement.getElementsByTagName( tagName ).item( 0 );
        if ( node == null ) return null;
        String value = node.getNodeValue();// .getNodeValue();

        return value;
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
    /*
     * TODO return value should be single string object. Note that many services will be affected should we make this
     * change.
     */
    protected Collection<String> getSingleNodeValue( Element requestElement, String tagName ) {
        Assert.isTrue( NAMESPACE_URI.equals( requestElement.getNamespaceURI() ), "Invalid namespace" );
        Assert.isTrue( localName.equals( requestElement.getLocalName() ), "Invalid local name" );
        authenticate();
        Collection<String> value = new HashSet<String>();
        String node = "";
        // get the Element with name = tagName
        NodeList children = requestElement.getElementsByTagName( tagName ).item( 0 ).getChildNodes();
        // iterate over the child nodes
        for ( int i = 0; i < children.getLength(); i++ ) {

            if ( children.item( i ).getNodeType() == Node.TEXT_NODE ) {
                node = children.item( i ).getNodeValue();
                value.add( node );
            }
            node = null;
        }
        if ( value.isEmpty() ) {
            // throw new IllegalArgumentException( "Could not find request text node" );
        }
        return value;
    }

    /**
     * Looks to parse a previously generated xml report that was saved to disk. Returns null if it fails to do so.
     * 
     * @param InputStream from an existing xml file
     * @return An XML document
     * @throws IOException
     */
    protected Document readReport( InputStream is ) throws IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        factory.setValidating( false );
        Document document = null;

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder = factory.newDocumentBuilder();
            document = builder.parse( is );
        } catch ( ParserConfigurationException pce ) {
            log.error( "Could not configure parser for reading report.  Error is: " + pce );
            throw ( new RuntimeException( pce ) );
        } catch ( SAXException se ) {
            log.error( "Could not parse report Error is: " + se );
            throw ( new RuntimeException( se ) );

        }

        return document;

    }

    /**
     * uses the default path of gemmaData/datafile/xml/ to look for reports.
     * 
     * @param filename needs the xml suffix
     * @return
     * @throws IOException
     */
    protected Document readReport( String filename ) throws IOException {
        String path = HOME_DIR + File.separatorChar + "dataFiles" + File.separatorChar + "xml" + File.separatorChar;
        return readReport( path, filename );

    }

    /**
     * @param path
     * @param fileName
     * @return xml document for the given path
     * @throws IOException
     */
    protected Document readReport( String path, String fileName ) throws IOException {

        File file = new File( path, fileName );

        if ( !file.exists() ) return null;

        // TODO: only load file if it is not out of date
        InputStream is = new FileInputStream( path + fileName );

        return readReport( is );
    }

    protected void setLocalName( String localName ) {
        this.localName = localName;
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
        String path = HOME_DIR + File.separatorChar + "dataFiles" + File.separatorChar + "xml" + File.separatorChar;
        try {
            File file = new File( path, fullFileName );

            if ( !file.exists() ) {
                new File( path ).mkdirs(); // in case of the subdirs doesn't exisit.
                FileOutputStream out = new FileOutputStream( path + fullFileName );
                OutputFormat format = new OutputFormat( document );
                format.setIndenting( true );
                // to generate a file output use fileoutputstream

                /*
                 * "It is recommended that new applications use the DOM Level 3 LSSerializer or JAXP's Transformation
                 * API for XML (TrAX) for serializing XML"
                 */
                XMLSerializer serializer = new XMLSerializer( out, null );
                serializer.serialize( responseWrapper );
                out.close();

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

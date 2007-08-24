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
package ubic.gemma.loader.expression.mage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.Common.MAGEJava;
import org.biomage.tools.xmlutils.MAGEContentHandler;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.util.parser.Parser;

/**
 * Class to parse MAGE-ML files and convert them into Source domain objects SDO.
 * 
 * @see ubic.gemma.loader.mage.MageMLConverter
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="mageMLParser" singleton="false"
 */
public class MageMLParser extends AbstractMageTool implements Parser {

    /**
     * 
     */
    private static final String XMLREADER = "org.apache.xerces.parsers.SAXParser";

    protected static final Log log = LogFactory.getLog( MageMLParser.class );

    private MAGEContentHandler cHandler;

    private Collection<Object> mageDomainObjects;
    private MAGEJava mageJava;
    private XMLReader parser;

    /**
     * Create a new MageMLParser
     */
    public MageMLParser() {
        super();
        try {
            parser = XMLReaderFactory.createXMLReader( XMLREADER );

            // This is needed to avoid the missing DTD problem
            parser.setEntityResolver( mageDtdResolver() );
            // parser.setProperty( "http://xml.org/sax/features/validation", Boolean.FALSE );
            // parser.setProperty( "http://xml.org/sax/features/external-parameter-entities", Boolean.FALSE );
        } catch ( SAXException e ) {
            throw new RuntimeException( e );
        }
        cHandler = new MAGEContentHandler();
        assert parser != null : "Parser was null, likely " + XMLREADER + " jar is not present";
        parser.setContentHandler( cHandler );
    }

    /**
     * Avoids the problem with the DTD being in an 'unknown' location
     * 
     * @return
     */
    private EntityResolver mageDtdResolver() {
        return new EntityResolver() {
            public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException {
                InputStream dtd = this.getClass()
                        .getResourceAsStream( "/ubic/gemma/loader/expression/mage/MAGE-ML.dtd" );
                assert dtd != null;
                return new InputSource( dtd );
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Parser#iterator()
     */
    public Collection<Object> getResults() {
        return mageDomainObjects;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Parser#parse(java.io.File)
     */
    public void parse( File f ) throws IOException {
        InputStream is = new FileInputStream( f );
        parse( is );
    }

    /**
     * Parse a MAGE-ML stream. This has to be called before any data can be retrieved. Use of this method means that
     * calling createSimplifiedXml must also be done by the programmer. This method is exposed primarily to allow
     * testing.
     * 
     * @param is
     * @throws SAXException
     * @throws IOException
     */
    public void parse( InputStream is ) throws IOException {
        try {
            parser.parse( new InputSource( is ) );
        } catch ( SAXException e ) {
            log.error( e, e );
            throw new IOException( e.getMessage() );
        }
        mageJava = cHandler.getMAGEJava();
        getDomainObjects();
    }

    /**
     * Parse a MAGE-ML file. This has to be called before any data can be retrieved. Creation of the simplified XML DOM
     * is also taken care of.
     * 
     * @param fileName
     * @throws IOException
     */
    public void parse( String fileName ) throws IOException {
        InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );
        try {
            parser.parse( new InputSource( is ) );

            // We no longer need to do this.
            // createSimplifiedXml( fileName );
        } catch ( SAXException e ) {
            log.error( e, e );
            throw new IOException( e.getMessage() );
        }
        mageJava = cHandler.getMAGEJava();
        is.close();
        this.getDomainObjects();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Integer> tally = new HashMap<String, Integer>();
        for ( Object element : mageDomainObjects ) {
            String clazz = element.getClass().getName();
            if ( !tally.containsKey( clazz ) ) {
                tally.put( clazz, new Integer( 0 ) );
            }
            tally.put( clazz, new Integer( ( tally.get( clazz ) ).intValue() + 1 ) );
        }

        for ( String clazz : tally.keySet() ) {
            buf.append( tally.get( clazz ) + " " + clazz + "s\n" );
        }

        return buf.toString();
    }

    /**
     * @param istMageXml Input MAGE-ML
     * @param istXSL XSL for transforming the MAGE-ML into a simpler format preserving key structure
     * @deprecated We no longer need to do this.
     */
    private void createSimplifiedXml( InputStream istMageXml, InputStream istXsl ) throws IOException,
            TransformerException {

        log.info( "Creating simplified XML" );

        if ( istXsl == null || istXsl.available() == 0 ) {
            throw new IllegalArgumentException( "Null or no bytes to read from the XSL stream" );
        }

        if ( istMageXml == null || istMageXml.available() == 0 ) {
            throw new IllegalArgumentException( "Null or no bytes to read from the MAGE-ML stream" );
        }

        SAXReader reader = new SAXReader();
        reader.setEntityResolver( mageDtdResolver() );
        Document xml;
        try {
            xml = reader.read( istMageXml );
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer( new StreamSource( istXsl ) );
            DocumentSource source = new DocumentSource( xml );
            DocumentResult result = new DocumentResult();
            transformer.transform( source, result );
            if ( result.getDocument() == null ) {
                throw new IOException( "Simplified XML creation failed" );
            }
            this.simplifiedXml = result.getDocument();
            if ( log.isDebugEnabled() ) {
                log.debug( "--------  Simplified XML ---------" );
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter( System.err, format );
                writer.write( simplifiedXml );
                log.debug( "-------------------------------------------" );
            }

        } catch ( DocumentException e ) {
            log.error( e, e );
            throw new RuntimeException( e );
        } catch ( TransformerConfigurationException e ) {
            log.error( e, e );
            throw new RuntimeException( e );
        }
    }

    /**
     * @param fileName
     * @throws IOException
     * @throws FileNotFoundException
     * @deprecated We no longer need to do this.
     */
    private void createSimplifiedXml( String fileName ) throws IOException, FileNotFoundException, TransformerException {
        InputStream is;
        is = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );
        InputStream isXsl = getSimplifyXslStream();
        createSimplifiedXml( is, isXsl );
        is.close();
    }

    /**
     * @return
     */
    private void getDomainObjects() {
        assert isParsed() : "Need to parse first";
        Package[] allPackages = Package.getPackages();

        if ( this.mageDomainObjects == null ) {
            this.mageDomainObjects = new ArrayList<Object>();
        } else {
            this.mageDomainObjects.clear();
        }
        // this is a little inefficient because it tries every possible package and class. - fix is to get just
        // the mage
        // packages!
        for ( int i = 0; i < allPackages.length; i++ ) {

            String name = allPackages[i].getName();
            if ( !name.startsWith( "org.biomage." ) || name.startsWith( "org.biomage.tools." )
                    || name.startsWith( "org.biomage.Interface" ) ) continue;

            for ( int j = 0; j < mageClasses.length; j++ ) {
                try {
                    Class c = Class.forName( name + "." + mageClasses[j] );
                    // Collection<Object> d = getConvertedData( c );
                    Collection<Object> d = getDomainObjectsForClass( c );
                    if ( d != null && d.size() > 0 ) {
                        log.debug( "Adding " + d.size() + " " + name + "." + mageClasses[j] + "s" );
                        mageDomainObjects.addAll( d );
                    }
                } catch ( ClassNotFoundException e ) {
                    // log.error( "Class not found: " + name + "." + mageClasses[j] );
                }
            }
        }
    }

    /**
     * Generic method to extract the desired MAGE objects. This is based on the assumption that each MAGE domain package
     * has a "getXXXX_package" method, which in turn has a "getXXX_list" method for each class it contains. Other
     * objects are only extracted during the process of conversion to Gemma objects. (This is basically a helper method)
     * <p>
     * 
     * @param type
     * @return Collection of MAGE domain objects.
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> getDomainObjectsForClass( Class type ) {

        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );

        String className = type.getName();
        String trimmedClassName = className.substring( className.lastIndexOf( '.' ) + 1 );

        String packageName = type.getPackage().getName();
        String trimmedPackageName = packageName.substring( packageName.lastIndexOf( '.' ) + 1 );
        String packageGetterMethodName = "get" + trimmedPackageName + "_package";

        try {

            Method packageGetterMethod = null;
            try {
                packageGetterMethod = mageJava.getClass().getMethod( packageGetterMethodName, new Class[] {} );
            } catch ( NoSuchMethodException e ) {
                return null; // that's okay - org.biomage.Common.MAGEJava.getCommon_package() triggers this.
            }

            Object packageOb = packageGetterMethod.invoke( mageJava, new Object[] {} );

            // Null is not an error: not all MAGE-ML files have all packages (in fact they don't in general)
            if ( packageOb == null ) return null;

            String listGetterMethodName = "get" + trimmedClassName + "_list";
            Method listGetterMethod = null;
            try {
                listGetterMethod = packageOb.getClass().getMethod( listGetterMethodName, new Class[] {} );
            } catch ( NoSuchMethodException e ) {
                return null; // that's okay, not everybody has one.
            }

            return ( Collection<Object> ) listGetterMethod.invoke( packageOb, new Object[] {} );

        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
        }
        return null;
    }

    /**
     * @return
     */
    private InputStream getSimplifyXslStream() {
        return this.getClass().getResourceAsStream( "MAGE-simplify.xsl" );
    }

    /**
     * Has the MAGE file already been parsed?
     * 
     * @return
     */
    private boolean isParsed() {
        return mageJava != null;
    }

    /**
     * This method is exposed primarily to allow testing.
     * 
     * @param mage
     * @throws IOException
     * @throws TransformerException
     */
    protected void createSimplifiedXml( InputStream mage ) throws IOException, TransformerException {
        InputStream isXsl = getSimplifyXslStream();
        this.createSimplifiedXml( mage, isXsl );
    }

}

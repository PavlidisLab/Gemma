/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
import org.biomage.Common.MAGEJava;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.tools.xmlutils.MAGEContentHandler;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;

import baseCode.util.FileTools;

/**
 * Class to parse MAGE-ML files and convert them into Gemma domain objects.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @see edu.columbia.gemma.loader.mage.MageMLConverter
 * @author pavlidis
 * @version $Id$
 */
public class MageMLParser {

    protected static final Log log = LogFactory.getLog( MageMLParser.class );

    private MAGEContentHandler cHandler;
    private Collection<Object> convertedResult;
    private boolean isConverted;
    private String[] mageClasses;
    private MAGEJava mageJava;
    private MageMLConverter mlc;
    private XMLReader parser;
    private Document simplifiedXml;

    /**
     * Create a new MageMLParser
     */
    public MageMLParser() {

        mlc = new MageMLConverter();

        ResourceBundle rb = ResourceBundle.getBundle( "mage" );
        String mageClassesString = rb.getString( "mage.classes" );
        mageClasses = mageClassesString.split( ", " );

        try {
            parser = XMLReaderFactory.createXMLReader( "org.apache.xerces.parsers.SAXParser" );
        } catch ( SAXException e ) {
            log.error( e, e );
        }

        cHandler = new MAGEContentHandler();
        parser.setContentHandler( cHandler );
    }

    /**
     * @return all the converted BioAssay objects.
     */
    public List<BioAssay> getConvertedBioAssays() {
        assert isConverted;
        List<BioAssay> result = new ArrayList<BioAssay>();
        for ( Object object : convertedResult ) {
            if ( object instanceof BioAssay ) {
                result.add( ( BioAssay ) object );
            }
        }
        log.info( "Found " + result.size() + " bioassays" );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.expression.mage.MageMLConverter#getBioAssayDesignElementDimension(org.biomage.BioAssay.BioAssay)
     */
    public List<DesignElement> getBioAssayDesignElementDimension( BioAssay bioAssay ) {
        assert isConverted;
        return this.mlc.getBioAssayDesignElementDimension( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.expression.mage.MageMLConverter#getBioAssayQuantitationTypeDimension(org.biomage.BioAssay.BioAssay)
     */
    public List<QuantitationType> getBioAssayQuantitationTypeDimension( BioAssay bioAssay ) {
        assert isConverted;
        return this.mlc.getBioAssayQuantitationTypeDimension( bioAssay );
    }

    /**
     * Convert all of the data from the parsed stream (convenience method)
     * 
     * @return Collection of Gemma domain objects.
     */
    public Collection getConvertedData() {
        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );
        Package[] allPackages = Package.getPackages();

        if ( convertedResult == null ) {
            convertedResult = new ArrayList<Object>();
        } else {
            convertedResult.clear();
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
                    Collection<Object> d = getConvertedData( c );
                    if ( d != null && d.size() > 0 ) {
                        log.info( "Adding " + d.size() + " converted " + name + "." + mageClasses[j] + "s" );
                        convertedResult.addAll( d );
                    }
                } catch ( ClassNotFoundException e ) {
                    // log.error( "Class not found: " + name + "." + mageClasses[j] );
                }
            }
        }
        this.isConverted = true;
        return convertedResult;
    }

    /**
     * Parse a MAGE-ML file. This has to be called before any data can be retrieved. Creation of the simplified XML DOM
     * is also taken care of.
     * 
     * @param fileName
     * @throws IOException
     * @throws SAXException
     */
    public void parse( String fileName ) throws IOException, SAXException, TransformerException {
        InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );
        parser.parse( new InputSource( is ) );
        mageJava = cHandler.getMAGEJava();
        is.close();

        createSimplifiedXml( fileName );
    }

    @Override
    public String toString() {
        assert isConverted;
        StringBuffer buf = new StringBuffer();
        Map<String, Integer> tally = new HashMap<String, Integer>();
        for ( Object element : convertedResult ) {
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
     * @param fileName
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void createSimplifiedXml( String fileName ) throws IOException, FileNotFoundException, TransformerException {
        InputStream is;
        is = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );
        InputStream isXsl = this.getClass().getResourceAsStream( "resource/MAGE-simplify.xsl" );
        createSimplifiedXml( is, isXsl );
        is.close();
    }

    /**
     * Generic method to extract desired data, converted to the Gemma domain objects.
     * 
     * @param type
     * @return
     */
    private Collection<Object> getConvertedData( Class type ) {
        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );
        Collection<Object> dataToConvert = getData( type );

        if ( dataToConvert == null ) return null;

        Collection<Object> localResult = new ArrayList<Object>();

        for ( Object element : dataToConvert ) {
            if ( element != null ) {
                Object converted = mlc.convert( element );
                if ( converted != null ) localResult.add( mlc.convert( element ) );
            }
        }
        return localResult;
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
    private Collection<Object> getData( Class type ) {

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
     * Has the stream already been parsed?
     * 
     * @return
     */
    private boolean isParsed() {
        return mageJava != null;
    }

    /**
     * This method is public primarily to allow testing.
     * 
     * @param istMageXml Input MAGE-ML
     * @param istXSL XSL for transforming the MAGE-ML into a simpler format preserving key structure
     */
    protected void createSimplifiedXml( InputStream istMageXml, InputStream istXsl ) throws IOException,
            TransformerException {

        log.info( "Creating simplified XML" );

        if ( istXsl == null || istXsl.available() == 0 ) {
            throw new IllegalArgumentException( "Null or no bytes to read from the XSL stream" );
        }

        if ( istMageXml == null || istMageXml.available() == 0 ) {
            throw new IllegalArgumentException( "Null or no bytes to read from the MAGE-ML stream" );
        }

        SAXReader reader = new SAXReader();
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
            assert mlc != null;
            mlc.setSimplifiedXml( this.simplifiedXml );

            if ( log.isDebugEnabled() ) {
                log.debug( "--------  Simplified XML ---------" );
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter( System.err, format );
                writer.write( simplifiedXml );
                log.debug( "-------------------------------------------" );
            }

        } catch ( DocumentException e ) {
            log.error( e, e );
        } catch ( TransformerConfigurationException e ) {
            log.error( e, e );
        }
    }

    /**
     * Parse a MAGE-ML stream. This has to be called before any data can be retrieved. Use of this method means that
     * calling createSimplifiedXml must also be done by the programmer. This method is public primarily to allow
     * testing.
     * 
     * @param is
     * @throws SAXException
     * @throws IOException
     */
    protected void parse( InputStream is ) throws IOException, SAXException {
        parser.parse( new InputSource( is ) );
        mageJava = cHandler.getMAGEJava();
    }
}

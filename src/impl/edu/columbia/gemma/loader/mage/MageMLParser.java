package edu.columbia.gemma.loader.mage;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.Common.MAGEJava;
import org.biomage.tools.xmlutils.MAGEContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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

    private XMLReader parser;
    private MAGEContentHandler cHandler;
    private MAGEJava mageJava;
    private MageMLConverter mlc;
    private String[] mageClasses;

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
     * Parse a MAGE-ML stream. This has to be called before any data can be retrieved.
     * 
     * @throws SAXException
     * @throws IOException
     * @param stream
     */
    public void parse( InputStream is ) throws IOException, SAXException {
        parser.parse( new InputSource( is ) );
        mageJava = cHandler.getMAGEJava();
    }

    /**
     * Convert all of the data from the parsed stream (convenience method)
     * 
     * @return Collection of Gemma domain objects.
     */
    public Collection getConvertedData() {
        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );
        Package[] allPackages = Package.getPackages();

        Collection result = new ArrayList();

        // todo: this is a little inefficient because it tries every possible package and class. - fix is to get just
        // the mage
        // packages!
        for ( int i = 0; i < allPackages.length; i++ ) {

            String name = allPackages[i].getName();
            if ( !name.startsWith( "org.biomage." ) || name.startsWith( "org.biomage.tools." )
                    || name.startsWith( "org.biomage.Interface" ) ) continue;

            for ( int j = 0; j < mageClasses.length; j++ ) {
                Class c = null;
                try {
                    c = Class.forName( name + "." + mageClasses[j] );
                    Collection d = getConvertedData( c );
                    if ( d != null && d.size() > 0 ) {
                        log.info( "Adding " + d.size() + " converted " + name + "." + mageClasses[j] + "s" );
                        result.addAll( d );
                    }
                } catch ( ClassNotFoundException e ) {
                    // log.error( "Class not found: " + name + "." + mageClasses[j] );
                }
            }
        }
        return result;
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
    public Collection getData( Class type ) {

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

            Object result = listGetterMethod.invoke( packageOb, new Object[] {} );
            assert result instanceof List; // it should be a whatever_list.
            return ( Collection ) result;

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
     * Generic method to extract desired data, converted to the Gemma domain objects.
     * 
     * @param type
     * @return
     */
    public Collection getConvertedData( Class type ) {
        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );
        Collection dataToConvert = getData( type );

        if ( dataToConvert == null ) return null;

        List result = new ArrayList();
        for ( Iterator iter = dataToConvert.iterator(); iter.hasNext(); ) {
            Object element = iter.next();
            if ( element != null ) {
                Object converted = mlc.convert( element );
                if ( converted != null ) result.add( mlc.convert( element ) );
            }
        }
        return result;
    }

    /**
     * Has the stream already been parsed?
     * 
     * @return
     */
    private boolean isParsed() {
        return mageJava != null;
    }

}

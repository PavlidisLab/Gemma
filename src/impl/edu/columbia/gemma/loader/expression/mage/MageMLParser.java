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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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

    private Collection<Object> convertedResult;

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

        if ( convertedResult == null ) {
            convertedResult = new ArrayList<Object>();
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
        return convertedResult;
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
    public Collection<Object> getData( Class type ) {

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

            return ( Collection ) listGetterMethod.invoke( packageOb, new Object[] {} );

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
    public Collection<Object> getConvertedData( Class type ) {
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
     * Has the stream already been parsed?
     * 
     * @return
     */
    private boolean isParsed() {
        return mageJava != null;
    }

    public String toString() {
        assert convertedResult != null;
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
}

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
 * Class to parse MAGE-ML files, and convert them into Gemma domain objects.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
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

    public MageMLParser() {

        mlc = new MageMLConverter();

        ResourceBundle rb = ResourceBundle.getBundle( "mage" );

        String mageClassesString = rb.getString( "mage.classes" );
        mageClasses = mageClassesString.split( ", " );

        try {
            parser = XMLReaderFactory.createXMLReader( "org.apache.xerces.parsers.SAXParser" );
        } catch ( SAXException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
     * Convert all of the data, returning a collection of Gemma domain objects.
     * 
     * @return
     */
    public Collection getConvertedData() {
        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );
        Package[] allPackages = Package.getPackages();

        Collection result = new ArrayList();

        for ( int i = 0; i < allPackages.length; i++ ) {

            String name = allPackages[i].getName();
            if ( !name.startsWith( "org.biomage." ) || name.startsWith( "org.biomage.tools." )
                    || name.startsWith( "org.biomage.Interface" ) ) continue;
            // todo: this is a little inefficient because it tries every possible class.
            for ( int j = 0; j < mageClasses.length; j++ ) {
                Class c = null;
                try {
                    c = Class.forName( name + "." + mageClasses[j] );
                    Collection d = getConvertedData( c );
                    if ( d != null ) result.addAll( d );
                } catch ( ClassNotFoundException e ) {
                    ;
                }
            }
        }
        return result;
    }

    /**
     * Generic method to extract the desired MAGE objects. This is based on the assumption that each MAGE domain
     * packages has a "getXXXX_package" method, which in turn has a "getXXX_list" method for each class it contains.
     * <p>
     * The alternative pattern is where an accessor is found in the class (e.g. Experiment) not the _package (e.g.
     * Experiment_package).
     * 
     * @param type
     * @return
     */
    public Collection getData( Class type ) {

        if ( !isParsed() ) throw new IllegalStateException( "Need to parse first" );

        String className = type.getName();
        String trimmedClassName = className.substring( className.lastIndexOf( '.' ) + 1 );

        // 1. find the package of type.
        String packageName = type.getPackage().getName();
        String trimmedPackageName = packageName.substring( packageName.lastIndexOf( '.' ) + 1 );
        String targetMethodName = "get" + trimmedPackageName + "_package";

        try {
            Method targetMethod = mageJava.getClass().getMethod( targetMethodName, new Class[] {} );

            if ( targetMethod == null )
                throw new NoSuchMethodException( "Couldn't locate method " + targetMethodName );

            // 2. get the _package class for that type.
            Object packageOb = targetMethod.invoke( mageJava, new Object[] {} );

            // Null is not an error: not all MAGE-ML files have all packages (in fact they don't in general)
            if ( packageOb == null ) return null;

            // 3. invoke the getXXX_list method for the _package.
            String secondMethodName = "get" + trimmedClassName + "_list";
            Method secondMethod = null;
            try {
                secondMethod = packageOb.getClass().getMethod( secondMethodName, new Class[] {} );
            } catch ( NoSuchMethodException e ) {
                log.debug( "No such method: " + secondMethodName );
                return null;
            }

            Object result = secondMethod.invoke( packageOb, new Object[] {} );
            assert result instanceof List; // it should be a whatever_list.
            return ( Collection ) result;

        } catch ( SecurityException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( NoSuchMethodException e ) {
            ;
        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private boolean isParsed() {
        return mageJava != null;
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
            result.add( mlc.convert( element ) );
        }
        return result;
    }
    // /**
    // * @return
    // */
    // public List getArrayDesigns( List dataToConvert ) {
    //
    // List result = new ArrayList();
    // for ( Iterator iter = dataToConvert.iterator(); iter.hasNext(); ) {
    // ArrayDesign fromObj = ( ArrayDesign ) iter.next();
    //
    // edu.columbia.gemma.expression.arrayDesign.ArrayDesign toOjb = mlc.convertArrayDesign( fromObj );
    // result.add( toOjb );
    // }
    // return result;
    // }
    //

    //
    // /**
    // * @return
    // */
    // public List getBioSequences() {
    // BioSequence_package pkg = mageJava.getBioSequence_package();
    //
    // if ( pkg == null ) return null;
    //
    // List fromList = pkg.getBioSequence_list();
    //
    // List result = new ArrayList( fromList.size() );
    // for ( Iterator iter = fromList.iterator(); iter.hasNext(); ) {
    // org.biomage.BioSequence.BioSequence fromObj = ( org.biomage.BioSequence.BioSequence ) iter.next();
    //
    // edu.columbia.gemma.sequence.biosequence.BioSequence toOjb = mlc.convertBioSequence( fromObj );
    // result.add( toOjb );
    //
    // }
    // return result;
    // }
    //

    //
    // /**
    // * @return
    // */
    // public List getCompositeSequences() {
    // DesignElement_package pkg = this.mageJava.getDesignElement_package();
    // if ( pkg == null ) return null;
    // List fromList = pkg.getCompositeSequence_list();
    // List result = new ArrayList( fromList.size() );
    //
    // List rcmap = pkg.getReporterCompositeMap_list(); // fixme
    //
    // for ( Iterator iter = fromList.iterator(); iter.hasNext(); ) {
    // org.biomage.DesignElement.CompositeSequence fromObj = ( org.biomage.DesignElement.CompositeSequence ) iter
    // .next();
    //
    // edu.columbia.gemma.expression.designElement.CompositeSequence toOjb = mlc
    // .convertCompositeSequence( fromObj );
    // result.add( toOjb );
    //
    // }
    // return result;
    // }
    //
    // /**
    // * @return
    // */
    // public List getReporters() {
    //
    // DesignElement_package pkg = this.mageJava.getDesignElement_package();
    // if ( pkg == null ) return null;
    // List fromList = pkg.getReporter_list();
    //
    // if ( fromList == null ) return null;
    //
    // List result = new ArrayList( fromList.size() );
    //
    // List frmap = pkg.getFeatureReporterMap_list();
    //
    // for ( Iterator iter = fromList.iterator(); iter.hasNext(); ) {
    // org.biomage.DesignElement.Reporter fromObj = ( org.biomage.DesignElement.Reporter ) iter.next();
    //
    // DesignElement toOjb = mlc.convertReporter( fromObj );
    // result.add( toOjb );
    //
    // }
    // return result;
    //
    // }
    //

}

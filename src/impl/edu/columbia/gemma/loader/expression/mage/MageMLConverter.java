package edu.columbia.gemma.loader.expression.mage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.QuantitationType.QuantitationType;
import org.dom4j.Document;

import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.loader.loaderutils.Converter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageMLConverter implements Converter {

    private static Log log = LogFactory.getLog( MageMLConverter.class.getName() );

    private MageMLConverterHelper mageConverterHelper;
    private Collection<Object> convertedResult;
    private String[] mageClasses;

    private boolean isConverted = false;

    private Document simplifiedXml;

    /**
     * @param xml Simplified XML created by a MageMLParser.
     */
    public MageMLConverter( Document xml ) {
        super();
        this.simplifiedXml = xml;
        ResourceBundle rb = ResourceBundle.getBundle( "mage" );
        String mageClassesString = rb.getString( "mage.classes" );
        mageClasses = mageClassesString.split( ", " );
        this.mageConverterHelper = new MageMLConverterHelper();
        mageConverterHelper.setSimplifiedXml( this.simplifiedXml );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    public Collection<Object> convert( Collection<Object> sourceDomainObjects ) {
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
                    Collection<Object> convertedObjects = getConvertedDataForType( c, sourceDomainObjects );
                    if ( convertedObjects != null && convertedObjects.size() > 0 ) {
                        log.info( "Adding " + convertedObjects.size() + " converted " + name + "." + mageClasses[j]
                                + "s" );
                        convertedResult.addAll( convertedObjects );
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
     * Generic method to extract desired data, converted to the Gemma domain objects.
     * 
     * @param type
     * @return
     */
    private Collection<Object> getConvertedDataForType( Class type, Collection<Object> mageDomainObjects ) {
        if ( mageDomainObjects == null ) return null;

        Collection<Object> localResult = new ArrayList<Object>();

        for ( Object element : mageDomainObjects ) {
            if ( element == null ) continue;
            if ( !( element.getClass().isAssignableFrom( type ) ) ) continue;

            Object converted = mageConverterHelper.convert( element );
            if ( converted != null ) localResult.add( mageConverterHelper.convert( element ) );
        }
        return localResult;
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
        return this.mageConverterHelper.getBioAssayDesignElementDimension( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.expression.mage.MageMLConverter#getBioAssayQuantitationTypeDimension(org.biomage.BioAssay.BioAssay)
     */
    public List<QuantitationType> getBioAssayQuantitationTypeDimension( BioAssay bioAssay ) {
        assert isConverted;
        return this.mageConverterHelper.getBioAssayQuantitationTypeDimension( bioAssay );
    }

    @Override
    public String toString() {
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

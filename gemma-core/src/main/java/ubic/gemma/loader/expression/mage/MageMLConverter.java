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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Class to parse MAGE-ML files and convert them into Gemma domain objects SDO.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="mageMLConverter" singleton="false"
 */
public class MageMLConverter extends AbstractMageTool implements Converter {

    private Collection<Object> convertedResult;
    private boolean isConverted = false;

    private MageMLConverterHelper mageConverterHelper;

    /**
     * default constructor
     */
    public MageMLConverter() {
        super();
        this.mageConverterHelper = new MageMLConverterHelper();
    }

    /**
     * This is provided for tests.
     * 
     * @param path
     */
    protected void addLocalExternalDataPath( String path ) {
        mageConverterHelper.addLocalExternalDataPath( path );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    public Collection<Object> convert( Collection objects ) {
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
                    Collection<Object> convertedObjects = getConvertedDataForType( c, objects );
                    if ( convertedObjects != null && convertedObjects.size() > 0 ) {
                        log.info( "Adding " + convertedObjects.size() + " converted " + name + "." + mageClasses[j]
                                + "s" );
                        convertedResult.addAll( convertedObjects );
                    }
                } catch ( ClassNotFoundException ignored ) {
                }
            }
        }

        // fillInBioMaterialFactorValues( convertedResult );
        fillInExpressionExperimentQuantitationTypes( convertedResult );
        this.isConverted = true;
        return convertedResult;
    }

    //
    // /**
    // * @param convertedResult
    // */
    // private void fillInBioMaterialFactorValues( Collection<Object> convertedResult ) {
    // ExpressionExperiment ee = null;
    // for ( Object object : convertedResult ) {
    // if ( object instanceof ExpressionExperiment ) ee = ( ExpressionExperiment ) object;
    // }
    // assert ee != null;
    //
    // Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
    // Collection<BioAssay> bioAssays = ee.getBioAssays();
    // for ( BioAssay assay : bioAssays ) {
    // for ( BioMaterial bm : assay.getSamplesUsed() ) {
    // log.info( "checking factor values on biomaterial " + bm );
    // Collection<FactorValue> factorValues = new HashSet<FactorValue>();
    // for ( FactorValue value : bm.getFactorValues() ) {
    // FactorValue efFactorValue = findMatchingFactorValue( value, experimentalFactors );
    // if ( efFactorValue == null )
    // throw new IllegalStateException( "No experimental-factor bound factor value found for " + value );
    // if ( efFactorValue.getExperimentalFactor() == null )
    // log.info( "experimental-factor bound factor value " + efFactorValue
    // + " has null experimental factor" );
    // factorValues.add( efFactorValue );
    // }
    // bm.setFactorValues( factorValues );
    // log.info( "biomaterial " + bm + " has " + factorValues.size() + " factor values: " + factorValues );
    // }
    // }
    // }

    // private FactorValue findMatchingFactorValue( FactorValue needle, Collection<ExperimentalFactor> haystack ) {
    // for ( ExperimentalFactor factor : haystack ) {
    // for ( FactorValue factorValue : factor.getFactorValues() ) {
    // // TODO find a better way to equate factor values
    // log.info( factorValue );
    // if ( needle.toString().equals( factorValue.toString() ) ) return factorValue;
    // }
    // }
    // return null;
    // }

    /**
     * @param convertedResult2
     */
    private void fillInExpressionExperimentQuantitationTypes( Collection<Object> convertedResult2 ) {
        ExpressionExperiment ee = null;
        for ( Object object : convertedResult ) {
            if ( object instanceof ExpressionExperiment ) {
                if ( ee != null )
                    throw new IllegalStateException( "Can't convert more than one EE from MAGE-ML at a time." );
                ee = ( ExpressionExperiment ) object;
            }
        }
        assert ee != null;

        for ( Object object : convertedResult ) {
            if ( object instanceof QuantitationType ) ee.getQuantitationTypes().add( ( QuantitationType ) object );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.mage.MageMLConverter#getBioAssayQuantitationTypeDimension(org.biomage.BioAssay.BioAssay)
     */
    public List<ubic.gemma.model.common.quantitationtype.QuantitationType> getBioAssayQuantitationTypeDimension(
            BioAssay bioAssay ) {
        assert isConverted;
        return this.mageConverterHelper.getBioAssayQuantitationTypeDimension( bioAssay );
    }

    /**
     * @return all the converted BioAssay objects.
     */
    public List<BioAssay> getConvertedBioAssays() {
        List<BioAssay> result = new ArrayList<BioAssay>();
        for ( Object object : convertedResult ) {
            if ( object instanceof BioAssay ) {
                result.add( ( BioAssay ) object );
            }
        }
        log.info( "Found " + result.size() + " bioassays" );
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
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
     * Generic method to extract desired data, converted to the Gemma domain objects.
     * 
     * @param type
     * @return
     */
    private Collection<Object> getConvertedDataForType( Class type, Collection<?> mageDomainObjects ) {
        if ( mageDomainObjects == null ) return null;

        Collection<Object> localResult = new ArrayList<Object>();

        for ( Object element : mageDomainObjects ) {
            if ( element == null ) continue;
            if ( !( element.getClass().isAssignableFrom( type ) ) ) continue;

            Object converted = convert( element );
            if ( converted != null ) {
                if ( converted instanceof Collection<?> ) {
                    localResult.addAll( ( Collection<?> ) converted );
                } else {
                    localResult.add( converted );
                }
            }
        }
        return localResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    public Object convert( Object mageObject ) {
        if ( mageObject == null ) return null;
        return mageConverterHelper.convert( mageObject );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.mage.MageMLConverterHelper#getBioAssayDimensions()
     */
    public BioAssayDimensions getBioAssayDimensions() {
        return this.mageConverterHelper.getBioAssayDimensions();
    }

    public Collection<BioAssay> getQuantitationTypeBioAssays() {
        return this.mageConverterHelper.getQuantitationTypeBioAssays();
    }
}

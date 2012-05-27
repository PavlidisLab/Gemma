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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.biomage.Array.Array;
import org.biomage.BioAssay.DerivedBioAssay;
import org.biomage.BioAssay.MeasuredBioAssay;
import org.biomage.BioAssay.PhysicalBioAssay;
import org.biomage.BioAssayData.BioAssayMap;

import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
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
 */
public class MageMLConverter extends AbstractMageTool implements Converter<Object, Object> {

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    @Override
    public Collection<Object> convert( Collection<? extends Object> objects ) {
        Package[] allPackages = Package.getPackages();

        Arrays.sort( allPackages, new Comparator<Package>() {
            @Override
            public int compare( Package o1, Package o2 ) {
                return o1.getName().compareTo( o2.getName() );
            }
        } );

        if ( convertedResult == null ) {
            convertedResult = new ArrayList<Object>();
            this.mageConverterHelper = new MageMLConverterHelper();
        } else {
            convertedResult.clear();
            this.mageConverterHelper = new MageMLConverterHelper();
        }

        Class<?>[] preConvert = new Class<?>[] { BioAssayMap.class, Array.class, DerivedBioAssay.class,
                MeasuredBioAssay.class, PhysicalBioAssay.class };
        List<Class<?>> preConvertL = Arrays.asList( preConvert );
        for ( Class<? extends Object> clazz : preConvertL ) {
            processMGEDClass( objects, clazz );
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
                    String className = name + "." + mageClasses[j];
                    Class<? extends Object> c = Class.forName( className );

                    if ( preConvertL.contains( c ) ) {
                        continue;
                    }

                    processMGEDClass( objects, c );
                } catch ( ClassNotFoundException ignored ) {
                }
            }
        }

        fillInBioMaterialFactorValues();
        fillInExpressionExperimentQuantitationTypes();
        cleanupBioAssays();

        validate();

        this.isConverted = true;
        return convertedResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    @Override
    public Object convert( Object mageObject ) {
        if ( mageObject == null ) return null;
        return mageConverterHelper.convert( mageObject );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.mage.MageMLConverter#getBioAssayQuantitationTypeDimension(org.biomage.BioAssay.BioAssay
     * )
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
            tally.put( clazz, tally.get( clazz ) + 1 );
        }

        for ( String clazz : tally.keySet() ) {
            buf.append( tally.get( clazz ) + " " + clazz + "s\n" );
        }

        return buf.toString();
    }

    /**
     * This is provided for tests.
     * 
     * @param path
     */
    protected void addLocalExternalDataPath( String path ) {
        mageConverterHelper.addLocalExternalDataPath( path );
    }

    /**
     * @param ee
     * @param array2BioAssay
     * @return
     */
    private ArrayDesign checkArrayDesigns( ExpressionExperiment ee,
            Map<String, Collection<org.biomage.BioAssay.BioAssay>> array2BioAssay ) {

        if ( array2BioAssay.size() == 0 ) {

            /*
             * I'm not sure this has ever been triggered.
             */
            log.info( ee.getBioAssays().size() + " bioassays associated with ee before cleanup." );
            Collection<ArrayDesign> availableArrayDesigns = new HashSet<ArrayDesign>();
            for ( Object convertedObject : this.convertedResult ) {
                if ( convertedObject instanceof ArrayDesign ) {
                    log.info( convertedObject );
                    availableArrayDesigns.add( ( ArrayDesign ) convertedObject );
                }
            }

            if ( availableArrayDesigns.size() > 1 ) {
                throw new IllegalStateException( "More than one array design without acceptable mapping to bioassays." );
            } else if ( availableArrayDesigns.size() == 0 ) {
                log.warn( "No arrayDesigns and none associated with the bioassays" );
                return null;
            }
            return availableArrayDesigns.iterator().next();

        }

        if ( ee.getBioAssays().size() < array2BioAssay.size() ) {
            throw new IllegalStateException(
                    "Something went wrong, the experiment has fewer bioassays than arrays used. Expected "
                            + array2BioAssay.size() + ", got " + ee.getBioAssays().size() );
        }

        log.info( array2BioAssay.size() + " assays based on array count; " + ee.getBioAssays().size()
                + " bioassays associated with ee before cleanup." );
        return null;

    }

    /**
     * @param a
     * @param that
     * @return
     */
    private boolean checkGuts( FactorValue a, FactorValue that ) {
        if ( a.getValue() != null ) {
            if ( that.getValue() == null ) return false;
            if ( a.getValue().equals( that.getValue() ) ) return true;
        }

        if ( a.getMeasurement() != null ) {
            if ( that.getMeasurement() == null ) return false;
            if ( a.getMeasurement().equals( that.getMeasurement() ) ) return true;
        }

        if ( a.getCharacteristics().size() > 0 ) {
            if ( that.getCharacteristics().size() != a.getCharacteristics().size() ) return false;

            for ( Characteristic c : a.getCharacteristics() ) {
                boolean match = false;
                for ( Characteristic c2 : that.getCharacteristics() ) {
                    if ( c.equals( c2 ) ) {
                        if ( match ) {
                            return false;
                        }
                        match = true;
                    }
                }
                if ( !match ) return false;
            }

            return true;
        }

        // everything is empy...
        return true;
    }

    /**
     * 
     */
    private void cleanupBioAssays() {
        ExpressionExperiment ee = null;
        for ( Object object : convertedResult ) {
            if ( object instanceof ExpressionExperiment ) {
                if ( ee != null )
                    throw new IllegalStateException( "Can't convert more than one EE from MAGE-ML at a time." );
                ee = ( ExpressionExperiment ) object;
            }
        }
        if ( ee == null ) {
            throw new IllegalStateException( "No experiment in the converted result" );
        }

        Collection<BioAssay> toRemove = new HashSet<BioAssay>();
        Collection<String> topLevelBioAssayIdentifiers = this.mageConverterHelper.getTopLevelBioAssayIdentifiers();
        Map<String, Collection<org.biomage.BioAssay.BioAssay>> array2BioAssay = this.mageConverterHelper
                .getArray2BioAssay();

        if ( ee.getBioAssays().size() == 0 ) {
            throw new IllegalStateException( "No bioassays" );
        }

        ArrayDesign singleArrayDesign = checkArrayDesigns( ee, array2BioAssay );

        Collection<String> usedArrayIds = new HashSet<String>();

        for ( BioAssay ba : ee.getBioAssays() ) {

            if ( topLevelBioAssayIdentifiers.size() > 0 && !topLevelBioAssayIdentifiers.contains( ba.getName() ) ) {
                log.info( "Removing bioassay with id=" + ba.getName() + ", it is not listed as being 'top level' (has "
                        + ba.getSamplesUsed().size() + " samplesUsed)" );
                toRemove.add( ba );
            } else if ( ba.getSamplesUsed().size() == 0 ) {
                log.warn( "Removing bioassay with no biomaterials: " + ba );
                toRemove.add( ba );
            } else {

                boolean keep = ( topLevelBioAssayIdentifiers.size() > 0 && topLevelBioAssayIdentifiers.contains( ba
                        .getName() ) );

                if ( !keep ) {

                    if ( array2BioAssay.size() == 0 ) {
                        /*
                         * The ArrayDesign package was missing from the MAGE-ML. We won't be able to check. We have to
                         * assume it's okay, but the array design might be null, so we try to set it if we only got a
                         * single array design in the first place.
                         */
                        if ( ba.getArrayDesignUsed() == null ) {

                            /*
                             * I don't think this every actually is false at this point.
                             */
                            if ( singleArrayDesign == null ) {
                                // / log.warn( "No array design available for " + ba );
                            } else {
                                ba.setArrayDesignUsed( singleArrayDesign );
                            }
                        }

                        keep = true;
                    } else {
                        for ( String arrayId : array2BioAssay.keySet() ) {
                            Collection<org.biomage.BioAssay.BioAssay> assay4Array = array2BioAssay.get( arrayId );
                            assert assay4Array.size() > 0;

                            for ( org.biomage.BioAssay.BioAssay arrayBa : assay4Array ) {
                                if ( arrayBa.getIdentifier().equals( ba.getName() ) ) {

                                    if ( usedArrayIds.contains( arrayId ) ) {
                                        log.warn( "Already have a BioAssay for array with id= " + arrayId );
                                    }

                                    // definitely keep;
                                    log.info( "Final bioassay on array " + arrayId + " ==> " + ba );
                                    keep = true;
                                    usedArrayIds.add( arrayId );
                                    break;
                                }
                            }
                        }
                    }
                }

                if ( !keep ) {
                    log.info( "Skipping bioassay that is not associated with array: " + ba );
                    toRemove.add( ba );
                } else {
                    /*
                     * Just in case we 'removed' it by accident.
                     */
                    toRemove.remove( ba );
                }

            }

        }

        ee.getBioAssays().removeAll( toRemove );

        /**
         * We need to put the NAME back... the ID (which we are storing in the name slot) is only used internally to the
         * MAGE-ML
         */
        for ( BioAssay ba : ee.getBioAssays() ) {
            String name = this.mageConverterHelper.getId2Name().get( ba.getName() );
            if ( log.isDebugEnabled() ) log.debug( ba.getName() + " --> " + name );
            if ( StringUtils.isNotBlank( name ) ) {
                ba.setName( name );
            }
        }

    }

    /**
     * 
     */
    private void fillInBioMaterialFactorValues() {
        ExpressionExperiment ee = null;
        for ( Object object : convertedResult ) {
            if ( object instanceof ExpressionExperiment ) ee = ( ExpressionExperiment ) object;
        }
        if ( ee == null ) {
            throw new IllegalStateException( "No experiment in the converted result" );
        }

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        Collection<BioAssay> bioAssays = ee.getBioAssays();

        for ( BioAssay assay : bioAssays ) {
            for ( BioMaterial bm : assay.getSamplesUsed() ) {

                /* First, don't bother doing this if they are already filled in */
                boolean ok = true;
                for ( FactorValue value : bm.getFactorValues() ) {
                    if ( value.getExperimentalFactor() == null ) {
                        ok = false;
                    }
                }

                if ( ok ) continue;

                log.info( "Checking factor values on biomaterial " + bm );
                Collection<FactorValue> factorValues = new HashSet<FactorValue>();
                for ( FactorValue value : bm.getFactorValues() ) {

                    FactorValue efFactorValue = findMatchingFactorValue( value, experimentalFactors );

                    if ( efFactorValue == null ) {
                        throw new IllegalStateException( "No experimental-factor bound factor value found for " + value );
                    }

                    if ( efFactorValue.getExperimentalFactor() == null )
                        log.info( "experimental-factor bound factor value " + efFactorValue
                                + " has null experimental factor" );
                    factorValues.add( efFactorValue );
                }
                bm.setFactorValues( factorValues );
                log.info( "biomaterial " + bm + " has " + factorValues.size() + " factor values: " + factorValues );
            }
        }
    }

    /**
     * Populate the quantitation types in the EE.
     */
    private void fillInExpressionExperimentQuantitationTypes() {
        ExpressionExperiment ee = null;
        for ( Object object : convertedResult ) {
            if ( object instanceof ExpressionExperiment ) {
                if ( ee != null )
                    throw new IllegalStateException( "Can't convert more than one EE from MAGE-ML at a time." );
                ee = ( ExpressionExperiment ) object;
            }
        }
        if ( ee == null ) {
            throw new IllegalStateException( "No experiment in the converted result" );
        }

        for ( Object object : convertedResult ) {
            if ( object instanceof QuantitationType ) {
                ee.getQuantitationTypes().add( ( QuantitationType ) object );
            }
        }
    }

    /**
     * @param needle
     * @param haystack
     * @return
     */
    private FactorValue findMatchingFactorValue( FactorValue needle, Collection<ExperimentalFactor> haystack ) {
        for ( ExperimentalFactor factor : haystack ) {
            for ( FactorValue candidate : factor.getFactorValues() ) {
                // log.info( factorValue );

                /*
                 * We don't use plain old equals here because the factor isn't filled in for our needle.
                 */
                boolean match = checkGuts( needle, candidate );

                if ( match ) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Generic method to extract desired data, converted to the Gemma domain objects.
     * 
     * @param type
     * @return
     */
    private Collection<Object> getConvertedDataForType( Class<? extends Object> type, Collection<?> mageDomainObjects ) {
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

    /**
     * @param objects
     * @param c
     * @return
     */
    private Collection<Object> processMGEDClass( Collection<? extends Object> objects, Class<? extends Object> c ) {
        Collection<Object> convertedObjects = getConvertedDataForType( c, objects );
        if ( convertedObjects != null && convertedObjects.size() > 0 ) {
            log.info( "Adding " + convertedObjects.size() + " converted " + c.getName() + "s" );
            convertedResult.addAll( convertedObjects );
        } else {
            log.debug( "Converted " + objects.size() + " " + c.getName() + "s" );
        }
        return convertedObjects;
    }

    /**
     * Check that we have a valid structure.
     */
    private void validate() {
        ExpressionExperiment ee = null;
        for ( Object object : convertedResult ) {
            if ( object instanceof ExpressionExperiment ) {
                if ( ee != null )
                    throw new IllegalStateException( "Can't convert more than one EE from MAGE-ML at a time." );
                ee = ( ExpressionExperiment ) object;
            }

            if ( ee != null ) {

                if ( ee.getSource() == null ) {
                    throw new IllegalStateException( "Should be source" );
                }

                if ( ee.getBioAssays().size() == 0 ) {
                    throw new IllegalStateException( "No bioassays" );
                }
                for ( BioAssay ba : ee.getBioAssays() ) {
                    if ( ba.getSamplesUsed().size() == 0 ) {
                        throw new IllegalStateException( "No biomaterials for bioassay " + ba );
                    }
                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
                        if ( bm.getSourceTaxon() == null ) {
                            throw new IllegalStateException( "No taxon for biomaterial: " + bm );
                        }

                        for ( FactorValue fv : bm.getFactorValues() ) {
                            if ( fv.getExperimentalFactor() == null ) {
                                throw new IllegalStateException(
                                        "Biomaterial factor value had no experimental factor defined: " + fv );
                            }
                        }

                    }
                }

                if ( ee.getQuantitationTypes().size() == 0 ) {
                    throw new IllegalStateException( "Zero quantitation types!" );
                }

                if ( ee.getExperimentalDesign().getExperimentalFactors().size() > 0 ) {
                    for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
                        if ( ef.getFactorValues().size() == 0 ) {
                            /*
                             * Let it go ...
                             */
                            log.warn( "Factor with no factor values: " + ef );
                        }

                        for ( FactorValue fv : ef.getFactorValues() ) {
                            if ( fv.getExperimentalFactor() == null || !fv.getExperimentalFactor().equals( ef ) ) {
                                throw new IllegalStateException(
                                        "Factor value didn't have experimental factor filled in correctly: " + fv
                                                + ", factor should have been " + ef );
                            }
                        }

                    }
                }
            }

        }
    }

}

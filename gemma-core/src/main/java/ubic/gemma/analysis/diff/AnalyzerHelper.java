/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A helper class for the analyzers. This class contains helper methods commonly needed when performing an analysis.
 * 
 * @spring.bean id="analyzerHelper"
 * @spring.property name="analysisHelperService" ref="analysisHelperService"
 * @author keshav
 * @version $Id$
 */
public class AnalyzerHelper {

    private static Log log = LogFactory.getLog( AnalyzerHelper.class );

    private AnalysisHelperService analysisHelperService = null;

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise.
     * 
     * @param expressionExperiment
     * @return boolean
     */
    public boolean blockComplete( ExpressionExperiment expressionExperiment ) {

        Exception ex = null;
        try {
            checkBlockDesign( expressionExperiment );
            checkBiologicalReplicates( expressionExperiment );
        } catch ( Exception e ) {
            e.printStackTrace();
            ex = e;
        } finally {
            if ( ex != null ) {
                log.info( "Incomplete block design." );
                return false;
            }
        }

        log.info( "Complete block design." );
        return true;
    }

    /**
     * Determines if each biomaterial in the expression experiment for the given quantitation type for the given
     * bioassay dimension has a factor value from each of the experimental factors.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    protected void checkBlockDesign( ExpressionExperiment expressionExperiment ) throws Exception {

        Collection<DesignElementDataVector> vectorsToUse = analysisHelperService.getVectors( expressionExperiment );
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectorsToUse );
        ExpressionDataDoubleMatrix matrix = builder.getMaskedPreferredData( null );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForAssays( matrix );

        /*
         * second, make sure each biomaterial has factor values from one experimental factor paired with factor values
         * from the other experimental factors
         */
        Collection<ExperimentalFactor> efs = expressionExperiment.getExperimentalDesign().getExperimentalFactors();
        checkBlockDesign( biomaterials, efs );
    }

    /**
     * Determines if each biomaterial has factor value from an experimental factor equal to experimental factor of one
     * of the supplied factor values.
     * 
     * @param biomaterials
     * @param factorValues
     * @throws Exception
     */
    protected void checkBlockDesign( Collection<BioMaterial> biomaterials,
            Collection<ExperimentalFactor> experimentalFactors ) throws Exception {

        Collection<HashSet> factorValuePairings = generateFactorValuePairings( experimentalFactors );

        /* check to see if the biomaterial's factor value pairing is one of the possible combinations */
        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            if ( factorValuesFromBioMaterial.size() < 2 )
                throw new Exception( "Biomaterial must have more than 1 factor value." );

            if ( !factorValuePairings.contains( factorValuesFromBioMaterial ) )
                throw new Exception(
                        "Biomaterial does not have a factor value from one of the experimental factors.  Incomplete block design." );

        }

    }

    /**
     * Checks if there are at at least 2 biological replicates for all of the groups.
     * 
     * @param expressionExperiment
     * @param quantitationType
     * @param bioAssayDimension
     * @throws Exception
     */
    protected void checkBiologicalReplicates( ExpressionExperiment expressionExperiment ) throws Exception {

        Collection<DesignElementDataVector> vectorsToUse = analysisHelperService.getVectors( expressionExperiment );
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectorsToUse );
        ExpressionDataDoubleMatrix matrix = builder.getMaskedPreferredData( null );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForAssays( matrix );

        Collection<BioMaterial> copyOfBiomaterials = biomaterials;

        /* second, make sure we have biological replicates */
        for ( BioMaterial biomaterial : biomaterials ) {

            Collection<FactorValue> factorValues = biomaterial.getFactorValues();

            boolean match = false;
            for ( BioMaterial m : copyOfBiomaterials ) {

                if ( biomaterial.equals( m ) ) continue;

                Collection<FactorValue> fvs = m.getFactorValues();

                if ( fvs.equals( factorValues ) ) {
                    log.debug( "Replicate found for biomaterial " + biomaterial + "." );
                    match = true;
                    break;
                }
            }
            if ( !match ) throw new Exception( "No replicate found for biomaterial " + biomaterial + "." );
        }
    }

    /**
     * Generates all possible factor value pairings for the given experimental factors.
     * 
     * @param experimentalFactors
     * @return A collection of hashsets, where each hashset is a pairing.
     */
    protected static Collection<HashSet> generateFactorValuePairings( Collection<ExperimentalFactor> experimentalFactors ) {
        /* set up the possible pairings */
        Collection<FactorValue> allFactorValues = new HashSet<FactorValue>();
        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            allFactorValues.addAll( experimentalFactor.getFactorValues() );
        }

        Collection<FactorValue> allFactorValuesCopy = allFactorValues;

        Collection<HashSet> factorValuePairings = new HashSet<HashSet>();

        for ( FactorValue factorValue : allFactorValues ) {
            for ( FactorValue f : allFactorValuesCopy ) {
                if ( f.getExperimentalFactor().equals( factorValue.getExperimentalFactor() ) ) continue;

                HashSet<FactorValue> factorValuePairing = new HashSet<FactorValue>();
                factorValuePairing.add( factorValue );
                factorValuePairing.add( f );

                if ( !factorValuePairings.contains( factorValuePairing ) ) {
                    factorValuePairings.add( factorValuePairing );
                }
            }
        }
        return factorValuePairings;
    }

    /**
     * Returns a collection of all the different types of biomaterials across all bioassays in the experiment. If there
     * is more than one biomaterial per bioassay, a {@link RuntimeException} is thrown.
     * 
     * @param expressionExperiment
     * @return Collection<BioMaterial>
     */
    public static Collection<BioMaterial> getBioMaterialsForBioAssays( ExpressionDataMatrix matrix ) {

        Collection<BioMaterial> biomaterials = null;

        Exception ex = null;
        try {
            biomaterials = getBioMaterialsForAssays( matrix );
        } catch ( Exception e ) {
            ex = e;
        } finally {
            if ( ex != null ) throw new RuntimeException( ex );
        }

        return biomaterials;
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    protected static Collection<BioMaterial> getBioMaterialsForAssays( ExpressionDataMatrix matrix ) throws Exception {

        Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        Collection<BioAssay> assays = new ArrayList<BioAssay>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bioassays = matrix.getBioAssaysForColumn( i );
            if ( bioassays.size() != 1 )
                throw new Exception( "Invalid number of bioassays for column " + i
                        + " of the matrix.  Expecting 1, got " + bioassays.size() + "." );
            assays.add( bioassays.iterator().next() );
        }

        for ( BioAssay assay : assays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 )
                throw new Exception( "Invalid number of biomaterials. Expecting 1 biomaterial/bioassay, got "
                        + materials.size() + "." );

            biomaterials.addAll( materials );

        }

        return biomaterials;
    }

    /**
     * Returns the factors that can be used by R for a two way anova. Each sample must have a factor value equal to one
     * of the supplied factor values.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public static List<String> getRFactorsFromFactorValuesForTwoWayAnova( Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {

            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();

            boolean match = false;

            for ( FactorValue factorValueFromBioMaterial : factorValuesFromBioMaterial ) {

                Collection<Characteristic> chs = factorValueFromBioMaterial.getCharacteristics();

                Characteristic ch = chs.iterator().next();

                for ( FactorValue f : factorValues ) {

                    if ( factorValueFromBioMaterial.getValue() == null && f.getValue() == null ) {
                        log.debug( "null factor value values.  Using characteristic value for the factor value check." );

                        Collection<Characteristic> cs = f.getCharacteristics();

                        if ( chs.size() != 1 || cs.size() != 1 )
                            throw new RuntimeException( "Only supports 1 characteristic per factor value." );

                        Characteristic c = cs.iterator().next();

                        if ( ch.getValue().equals( c.getValue() ) ) {
                            rFactors.add( ch.getValue() );
                            match = true;
                            break;
                        }

                    }

                    else if ( !factorValueFromBioMaterial.getValue().equals( f.getValue() ) ) {
                        continue;
                    }

                    else if ( factorValueFromBioMaterial.getValue().equals( f.getValue() ) ) {
                        rFactors.add( factorValueFromBioMaterial.getValue() );
                        match = true;
                        break;
                    }
                }
            }
            if ( !match )
                throw new RuntimeException(
                        "None of the Factor values of the biomaterial match the supplied factor values." );
        }

        return rFactors;
    }

    /**
     * Returns the factors that can be used by R for a one way anova. This can also be used for t-tests. There
     * requirement here is that there is only one factor value per biomaterial, and all factor values are from the same
     * experimental factor.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public static List<String> getRFactorsFromFactorValuesForOneWayAnova( Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {

        // TODO Use the experimental factor as input as this will assure all factor values are from the same
        // experimental factor.

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();

            if ( factorValuesFromBioMaterial.size() != 1 ) {
                throw new RuntimeException( "Only supports 1 factor value per biomaterial." );
            }

            FactorValue fv = factorValuesFromBioMaterial.iterator().next();

            Collection<Characteristic> chs = fv.getCharacteristics();

            Characteristic ch = chs.iterator().next();

            for ( FactorValue f : factorValues ) {

                if ( fv.getValue() == null && f.getValue() == null ) {
                    log.debug( "null factor value values.  Using characteristic value for the factor value check." );

                    Collection<Characteristic> cs = f.getCharacteristics();

                    if ( chs.size() != 1 || cs.size() != 1 )
                        throw new RuntimeException( "Only supports 1 characteristic per factor value." );

                    Characteristic c = cs.iterator().next();

                    if ( ch.getValue().equals( c.getValue() ) ) {
                        rFactors.add( ch.getValue() );
                        break;
                    }

                }

                else if ( fv.getValue().equals( f.getValue() ) ) {
                    rFactors.add( fv.getValue() );
                    break;
                }

            }
        }
        return rFactors;
    }

    public void setAnalysisHelperService( AnalysisHelperService analysisHelperService ) {
        this.analysisHelperService = analysisHelperService;
    }
}

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
package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A helper class for the differential expression analyzers. This class contains helper methods commonly needed when
 * performing an analysis.
 * 
 * @spring.bean id="differentialExpressionAnalysisHelperService"
 * @spring.property name="expressionDataMatrixService" ref="expressionDataMatrixService"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisHelperService {

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalysisHelperService.class );

    private ExpressionDataMatrixService expressionDataMatrixService = null;

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise.
     * 
     * @param expressionExperiment
     * @return boolean
     */
    public boolean blockComplete( ExpressionExperiment expressionExperiment ) {

        boolean completeBlock = checkBlockDesign( expressionExperiment );
        boolean hasAllReps = checkBiologicalReplicates( expressionExperiment );

        return completeBlock && hasAllReps;
    }

    /**
     * Determines if each biomaterial in the expression experiment for the given quantitation type for the given
     * bioassay dimension has a factor value from each of the experimental factors.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    protected boolean checkBlockDesign( ExpressionExperiment expressionExperiment ) {

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForBioAssays( dmatrix );

        /*
         * second, make sure each biomaterial has factor values from one experimental factor paired with factor values
         * from the other experimental factors
         */
        Collection<ExperimentalFactor> efs = expressionExperiment.getExperimentalDesign().getExperimentalFactors();
        return checkBlockDesign( biomaterials, efs );
    }

    /**
     * Determines if each biomaterial has factor value from an experimental factor equal to experimental factor of one
     * of the supplied factor values.
     * 
     * @param biomaterials
     * @param factorValues
     * @return false if not a complete block design.
     */
    protected boolean checkBlockDesign( Collection<BioMaterial> biomaterials,
            Collection<ExperimentalFactor> experimentalFactors ) {

        Collection<HashSet> factorValuePairings = generateFactorValuePairings( experimentalFactors );

        /* check to see if the biomaterial's factor value pairing is one of the possible combinations */
        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            if ( factorValuesFromBioMaterial.size() < 2 ) {
                log.warn( "Biomaterial must have more than 1 factor value." );
                return false;
            }

            if ( !factorValuePairings.contains( factorValuesFromBioMaterial ) ) {
                log
                        .warn( "Biomaterial does not have a factor value from one of the experimental factors.  Incomplete block design." );
                return false;
            }

        }
        return true;

    }

    /**
     * Checks if there are at at least 2 biological replicates for all of the groups.
     * 
     * @param expressionExperiment
     * @param quantitationType
     * @param bioAssayDimension
     * @return false if there are any factorvalues which do not have a matching BioMaterial.
     */
    protected boolean checkBiologicalReplicates( ExpressionExperiment expressionExperiment ) {

        ExpressionDataDoubleMatrix matrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForBioAssays( matrix );

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
            if ( !match ) {
                log.warn( "No replicate found for biomaterial " + biomaterial + "." );
                return false;
            }
        }
        return true;
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
     * @return
     * @throws Exception
     */
    public static List<BioMaterial> getBioMaterialsForBioAssays( ExpressionDataMatrix<?> matrix ) {

        List<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        Collection<BioAssay> assays = new ArrayList<BioAssay>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bioassays = matrix.getBioAssaysForColumn( i );
            if ( bioassays.size() != 1 ) {
                throw new RuntimeException( "Invalid number of bioassays for column " + i
                        + " of the matrix.  Expecting 1, got " + bioassays.size() + "." );
            }
            assays.add( bioassays.iterator().next() );
        }

        for ( BioAssay assay : assays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 ) {
                throw new RuntimeException( "Invalid number of biomaterials. Expecting 1 biomaterial/bioassay, got "
                        + materials.size() + "." );
            }

            biomaterials.addAll( materials );

        }

        return biomaterials;
    }

    /**
     * Returns the factors that can be used by R for a two way anova. Each sample must have a factor value equal to one
     * of the supplied factor values. This assumes that "equals" works correctly on the factor values.
     * 
     * @param experimentalFactor
     * @param samplesUsed the samples we want to assign to the various factors
     * @return R factor representation, in the same order as the given samplesUsed.
     */
    public static List<String> getRFactorsFromFactorValuesForTwoWayAnova( ExperimentalFactor experimentalFactor,
            List<BioMaterial> samplesUsed ) {

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();
            boolean match = false;

            for ( FactorValue factorValue : factorValuesFromBioMaterial ) {
                for ( FactorValue candidateMatch : experimentalFactor.getFactorValues() ) {
                    if ( candidateMatch.equals( factorValue ) ) {
                        rFactors.add( factorValue.getId().toString() );
                        match = true;
                        break;
                    }
                }
            }
            if ( !match )
                throw new IllegalStateException(
                        "None of the Factor values of the biomaterial match the supplied factor values." );
        }

        return rFactors;
    }

    /**
     * Returns the factors that can be used by R for a one way anova. This can also be used for t-tests. There
     * requirement here is that there is only one factor value per biomaterial, and all factor values are from the same
     * experimental factor.
     * <p>
     * FIXME use the ExperimentalFactor as the input, not the FactorValues.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return list of strings representing the factor, in the same order as the supplied samplesUsed.
     */
    public static List<String> getRFactorsFromFactorValuesForOneWayAnova( Collection<FactorValue> factorValues,
            List<BioMaterial> samplesUsed ) {

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();

            if ( factorValuesFromBioMaterial.size() != 1 ) {
                throw new RuntimeException( "Only supports 1 factor value per biomaterial." );
            }

            FactorValue fv = factorValuesFromBioMaterial.iterator().next();

            for ( FactorValue f : factorValues ) {
                if ( f.equals( fv ) ) {
                    rFactors.add( fv.getId().toString() );
                    break;
                }
            }
        }
        return rFactors;
    }

    public void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService ) {
        this.expressionDataMatrixService = expressionDataMatrixService;
    }

}

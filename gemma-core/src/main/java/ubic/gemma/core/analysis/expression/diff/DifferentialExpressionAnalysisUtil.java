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
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.*;

/**
 * A helper class for the differential expression analyzers. This class contains helper methods commonly needed when
 * performing an analysis.
 *
 * @author keshav
 */
public class DifferentialExpressionAnalysisUtil {

    private static final Log log = LogFactory.getLog( DifferentialExpressionAnalysisUtil.class );

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise. When determining completeness, a biomaterial's factor values are only considered if they are
     * equivalent to one of the input experimental factors.
     *
     * @param factors              to consider completeness for.
     * @param expressionExperiment the experiment
     * @return true if block complete
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    public static boolean blockComplete( BioAssaySet expressionExperiment, Collection<ExperimentalFactor> factors ) {

        Collection<BioMaterial> biomaterials = DifferentialExpressionAnalysisUtil
                .getBioMaterials( expressionExperiment );

        /*
         * Get biomaterials with only those factor values equal to the factor values in the input factors. Only these
         * factor values in each biomaterial will be used to determine completeness.
         */
        Collection<BioMaterial> biomaterialsWithGivenFactorValues = DifferentialExpressionAnalysisUtil
                .filterFactorValuesFromBiomaterials( factors, biomaterials );

        boolean completeBlock = DifferentialExpressionAnalysisUtil
                .checkBlockDesign( biomaterialsWithGivenFactorValues, factors );
        boolean hasAllReps = DifferentialExpressionAnalysisUtil
                .checkBiologicalReplicates( expressionExperiment, factors );

        return completeBlock && hasAllReps;
    }

    /**
     * See if there are at least two samples for each factor value combination.
     *
     * @param expressionExperiment the experiment
     * @param factors              factors
     * @return true if there are replicates
     */
    static boolean checkBiologicalReplicates( BioAssaySet expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        Collection<BioMaterial> biomaterials = DifferentialExpressionAnalysisUtil
                .getBioMaterials( expressionExperiment );

        for ( BioMaterial firstBm : biomaterials ) {

            Collection<FactorValue> factorValuesToCheck = DifferentialExpressionAnalysisUtil
                    .getRelevantFactorValues( factors, firstBm );

            boolean match = false;
            for ( BioMaterial secondBm : biomaterials ) {

                if ( firstBm.equals( secondBm ) )
                    continue;

                Collection<FactorValue> factorValuesToCompareTo = DifferentialExpressionAnalysisUtil
                        .getRelevantFactorValues( factors, secondBm );

                if ( factorValuesToCheck.size() == factorValuesToCompareTo.size() && factorValuesToCheck
                        .containsAll( factorValuesToCompareTo ) ) {
                    DifferentialExpressionAnalysisUtil.log.debug( "Replicate found for biomaterial " + firstBm + "." );
                    match = true;
                    break;
                }
            }
            if ( !match ) {
                DifferentialExpressionAnalysisUtil.log
                        .warn( "No replicate found for biomaterial " + firstBm + ", with factor values" + StringUtils
                                .join( factorValuesToCheck, "," ) );
                return false;
            }
        }

        return true;

    }

    /**
     * Check that the factorValues are measurements, or that there are at least two assays for at least one factor
     * value. Otherwise the model fit will be perfect and pvalues will not be returned.
     *
     * @param experimentalFactor   exp. factor
     * @param expressionExperiment the experiment
     * @return true if it's okay, false otherwise.
     */
    public static boolean checkValidForLm( BioAssaySet expressionExperiment, ExperimentalFactor experimentalFactor ) {

        if ( experimentalFactor.getFactorValues().size() < 2 ) {
            log.warn( "Cannot be analyzed: Only one factor value (level) for " + experimentalFactor );
            return false;
        }

        if ( experimentalFactor.getType().equals( FactorType.CONTINUOUS ) ) {
            return true;
        }

        /*
         * Check to make sure more than one factor value is actually used.
         */
        boolean replicatesok = false;
        Map<FactorValue, Integer> counts = new HashMap<>();
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( experimentalFactor ) ) {

                    if ( !counts.containsKey( fv ) ) {
                        counts.put( fv, 0 );
                    }

                    counts.put( fv, counts.get( fv ) + 1 );
                    if ( counts.get( fv ) > 1 ) {
                        replicatesok = true;
                    }

                }

            }
        }

        if ( counts.size() < 2 ) {
            DifferentialExpressionAnalysisUtil.log.warn( experimentalFactor + " has only " + counts.size()
                    + " levels used in the current set, it cannot be analyzed" );
            return false;
        }

        return replicatesok;
    }


    /**
     * Returns true if all of the following conditions hold true: each biomaterial has more than 2 factor values, each
     * biomaterial has a factor value from one of the input factors paired with a factor value from the other input
     * factors, and all factor values from 1 factor have been paired with all factor values from the other factors,
     * across all biomaterials.
     *
     * @param biomaterials        biomaterials
     * @param experimentalFactors exp. factors
     * @return false if not a complete block design.
     */
    private static boolean checkBlockDesign( Collection<BioMaterial> biomaterials,
            Collection<ExperimentalFactor> experimentalFactors ) {

        Collection<Set<FactorValue>> factorValuePairings = DifferentialExpressionAnalysisUtil
                .generateFactorValuePairings( experimentalFactors );

        /* check to see if the biomaterial's factor value pairing is one of the possible combinations */
        Map<Collection<FactorValue>, BioMaterial> seenPairings = new HashMap<>();
        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getAllFactorValues();

            if ( factorValuesFromBioMaterial.size() < experimentalFactors.size() ) {
                DifferentialExpressionAnalysisUtil.log
                        .warn( "Biomaterial must have at least " + experimentalFactors.size()
                                + "factor value.  Incomplete block design. " + m );
                return false;
            }

            /*
             * Find a combination of factors used in the model that this biomaterial has.
             */
            boolean ok = false;
            for ( Set<FactorValue> pairing : factorValuePairings ) {
                if ( factorValuesFromBioMaterial.containsAll( pairing ) ) {
                    ok = true;
                    break;
                }
            }

            if ( !ok ) {
                /*
                 * This amounts to a missing value.
                 */
                throw new IllegalArgumentException(
                        "Biomaterial does not have a combination of factors matching the model; design error?: " + m );
                // continue;
            }

            seenPairings.put( factorValuesFromBioMaterial, m );
        }
        if ( seenPairings.size() != factorValuePairings.size() ) {
            DifferentialExpressionAnalysisUtil.log
                    .warn( "Biomaterial not paired with all factor values for each of " + experimentalFactors.size()
                            + " experimental factors.  Found " + seenPairings.size() + " pairings but should have "
                            + factorValuePairings.size() + ".  Incomplete block design." );
            return false;
        }
        return true;

    }

    /**
     * Generates all possible factor value pairings for the given experimental factors.
     *
     * @param experimentalFactors exp. factors
     * @return A collection of hashSets, where each hashSet is a pairing.
     */
    private static Collection<Set<FactorValue>> generateFactorValuePairings(
            Collection<ExperimentalFactor> experimentalFactors ) {
        /* set up the possible pairings */
        Collection<FactorValue> allFactorValues = new HashSet<>();
        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            allFactorValues.addAll( experimentalFactor.getFactorValues() );
        }

        Collection<Set<FactorValue>> factorValuePairings = new HashSet<>();

        for ( FactorValue factorValue : allFactorValues ) {
            for ( FactorValue f : allFactorValues ) {
                if ( f.getExperimentalFactor().equals( factorValue.getExperimentalFactor() ) )
                    continue;

                HashSet<FactorValue> factorValuePairing = new HashSet<>();
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
     * Returns biomaterials with 'filtered' factor values. That is, each biomaterial will only contain those factor
     * values equivalent to a factor value from one of the input experimental factors.
     */
    private static Collection<BioMaterial> filterFactorValuesFromBiomaterials( Collection<ExperimentalFactor> factors,
            Collection<BioMaterial> biomaterials ) {

        assert !biomaterials.isEmpty();
        assert !factors.isEmpty();

        Collection<FactorValue> allFactorValuesFromGivenFactors = new HashSet<>();
        for ( ExperimentalFactor ef : factors ) {
            allFactorValuesFromGivenFactors.addAll( ef.getFactorValues() );
        }

        Collection<BioMaterial> biomaterialsWithGivenFactorValues = new HashSet<>();
        int numHaveAny = 0;
        for ( BioMaterial b : biomaterials ) {
            Collection<FactorValue> biomaterialFactorValues = b.getAllFactorValues();
            Set<FactorValue> factorValuesToConsider = new HashSet<>( biomaterialFactorValues );
            for ( FactorValue biomaterialFactorValue : biomaterialFactorValues ) {
                numHaveAny++;
                if ( !allFactorValuesFromGivenFactors.contains( biomaterialFactorValue ) ) {
                    factorValuesToConsider.remove( biomaterialFactorValue );
                }
            }
            b.setFactorValues( factorValuesToConsider );
            biomaterialsWithGivenFactorValues.add( b );
        }

        if ( numHaveAny == 0 ) {
            throw new IllegalStateException( "No biomaterials had any factor values" );
        }

        return biomaterialsWithGivenFactorValues;
    }

    /**
     * @return a collection of all the different types of biomaterials across all bioassays in the experiment.
     */
    private static List<BioMaterial> getBioMaterials( BioAssaySet ee ) {

        List<BioMaterial> biomaterials = new ArrayList<>();

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        for ( BioAssay assay : ee.getBioAssays() ) {
            BioMaterial material = assay.getSampleUsed();
            biomaterials.add( material );
        }

        return biomaterials;
    }

    /**
     * Isolate a biomaterial's factor values for a specific factor(s).
     *
     * @return the factor values the biomaterial has for the given factors.
     */
    private static Collection<FactorValue> getRelevantFactorValues( Collection<ExperimentalFactor> factors,
            BioMaterial biomaterial ) {
        Collection<FactorValue> factorValues = biomaterial.getAllFactorValues();

        Collection<FactorValue> factorValuesToCheck = new HashSet<>();
        for ( FactorValue factorValue : factorValues ) {
            if ( factors.contains( factorValue.getExperimentalFactor() ) ) {
                factorValuesToCheck.add( factorValue );
            }
        }
        return factorValuesToCheck;
    }

}

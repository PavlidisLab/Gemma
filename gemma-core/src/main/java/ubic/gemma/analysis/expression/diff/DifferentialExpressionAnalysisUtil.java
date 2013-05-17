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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A helper class for the differential expression analyzers. This class contains helper methods commonly needed when
 * performing an analysis.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisUtil {

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalysisUtil.class );

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise. When determining completeness, a biomaterial's factor values are only considered if they are
     * equivalent to one of the input experimental factors.
     * 
     * @param expressionExperiment
     * @param factors to consider completeness for.
     * @return boolean
     */
    public static boolean blockComplete( BioAssaySet expressionExperiment, Collection<ExperimentalFactor> factors ) {

        Collection<BioMaterial> biomaterials = getBioMaterials( expressionExperiment );

        /*
         * Get biomaterials with only those factor values equal to the factor values in the input factors. Only these
         * factor values in each biomaterial will be used to determine completeness.
         */
        Collection<BioMaterial> biomaterialsWithGivenFactorValues = filterFactorValuesFromBiomaterials( factors,
                biomaterials );

        boolean completeBlock = checkBlockDesign( biomaterialsWithGivenFactorValues, factors );

        boolean hasAllReps = checkBiologicalReplicates( expressionExperiment, factors );

        return completeBlock && hasAllReps;
    }

    /**
     * See if there are at least two samples for each factor value combination.
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    protected static boolean checkBiologicalReplicates( BioAssaySet expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        Collection<BioMaterial> biomaterials = getBioMaterials( expressionExperiment );

        for ( BioMaterial firstBm : biomaterials ) {

            Collection<FactorValue> factorValuesToCheck = getRelevantFactorValues( factors, firstBm );

            boolean match = false;
            for ( BioMaterial secondBm : biomaterials ) {

                if ( firstBm.equals( secondBm ) ) continue;

                Collection<FactorValue> factorValuesToCompareTo = getRelevantFactorValues( factors, secondBm );

                if ( factorValuesToCheck.size() == factorValuesToCompareTo.size()
                        && factorValuesToCheck.containsAll( factorValuesToCompareTo ) ) {
                    log.debug( "Replicate found for biomaterial " + firstBm + "." );
                    match = true;
                    break;
                }
            }
            if ( !match ) {
                log.warn( "No replicate found for biomaterial " + firstBm + "." );
                return false;
            }
        }

        return true;

    }

    /**
     * Check that <em>at least one</em> of the given factors is valid: the factorvalues are measurements, or that there
     * are at least two assays for at least one factor value.
     * 
     * @param expressionExperiment
     * @param experimentalFactors
     * @return
     */
    public static boolean checkValidForLm( BioAssaySet expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {
        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            boolean ok = checkValidForLm( expressionExperiment, experimentalFactor );
            if ( ok ) return true;
        }
        return false;
    }

    /**
     * Check that the factorvalues are measurements, or that there are at least two assays for at least one factor
     * value. Otherwise the model fit will be perfect and pvalues will not be returned.
     * 
     * @param expressionExperiment
     * @param experimentalFactor
     * @return true if it's okay, false otherwise.
     */
    public static boolean checkValidForLm( BioAssaySet expressionExperiment, ExperimentalFactor experimentalFactor ) {

        if ( experimentalFactor.getFactorValues().size() < 2 ) {
            return false;
        }

        if ( experimentalFactor.getType().equals( FactorType.CONTINUOUS ) ) {
            return true;
        }

        /*
         * Check to make sure more than one factor value is actually used.
         */
        boolean replicatesok = false;
        Map<FactorValue, Integer> counts = new HashMap<FactorValue, Integer>();
        for ( BioAssay ba : ( Collection<BioAssay> ) expressionExperiment.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            for ( FactorValue fv : bm.getFactorValues() ) {
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
            log.warn( experimentalFactor + " has only " + counts.size()
                    + " levels used in the current set, it cannot be analyzed" );
            return false;
        }

        return replicatesok;
    }

    /**
     * Returns a List of all the different types of biomaterials across all bioassays in the experiment. If there is
     * more than one biomaterial per bioassay, a {@link RuntimeException} is thrown.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    public static List<BioMaterial> getBioMaterialsForBioAssays( ExpressionDataMatrix<?> matrix ) {

        List<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        Collection<BioAssay> assays = new ArrayList<BioAssay>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bioassays = matrix.getBioAssaysForColumn( i );
            /*
             * Note: we could use addAll here. The mapping of bioassays to biomaterials is many-to-one (e.g., when the
             * HGU133A and B arrays were both used on the same set of samples).
             */
            assays.add( bioassays.iterator().next() );
        }

        for ( BioAssay assay : assays ) {
            BioMaterial material = assay.getSampleUsed();

            biomaterials.add( material );

        }

        return biomaterials;
    }

    /**
     * Returns the factors that can be used by R for a one way anova or t-test. There requirement here is that there is
     * only one factor value per factor per biomaterial (of course), and all factor values are from the same
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

        for ( BioMaterial biomaterial : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = biomaterial.getFactorValues();

            /*
             * Actually, it's fine if there are multiple factorValues, so long as they are from different factors.
             */

            if ( factorValuesFromBioMaterial.size() != 1 ) {

                /*
                 * Check to see if they are from different factors.
                 */
                ExperimentalFactor seen = null;
                for ( FactorValue fv : factorValuesFromBioMaterial ) {
                    if ( seen != null && fv.getExperimentalFactor().equals( seen ) ) {
                        throw new RuntimeException( "There should only be one factorvalue per factor per biomaterial, "
                                + biomaterial + " had multiple values for " + seen );
                    }
                    seen = fv.getExperimentalFactor();
                }
            }

            boolean found = false;
            for ( FactorValue candidate : factorValuesFromBioMaterial ) {
                if ( factorValues.contains( candidate ) ) {
                    rFactors.add( candidate.getId() + "_f" );
                    found = true;
                    break;
                }
            }

            if ( !found ) {
                throw new IllegalStateException( "No match for factorvalue on " + biomaterial
                        + " among possible factorValues: " + StringUtils.join( factorValues, "\n" ) );
            }
        }
        return rFactors;
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
     * Returns true if all of the following conditions hold true: each biomaterial has more than 2 factor values, each
     * biomaterial has a factor value from one of the input factors paired with a factor value from the other input
     * factors, and all factor values from 1 factor have been paired with all factor values from the other factors,
     * across all biomaterials.
     * 
     * @param biomaterials
     * @param factorValues
     * @return false if not a complete block design.
     */
    protected static boolean checkBlockDesign( Collection<BioMaterial> biomaterials,
            Collection<ExperimentalFactor> experimentalFactors ) {

        Collection<Set<FactorValue>> factorValuePairings = generateFactorValuePairings( experimentalFactors );

        /* check to see if the biomaterial's factor value pairing is one of the possible combinations */
        Map<Collection<FactorValue>, BioMaterial> seenPairings = new HashMap<Collection<FactorValue>, BioMaterial>();
        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            if ( factorValuesFromBioMaterial.size() < experimentalFactors.size() ) {
                log.warn( "Biomaterial must have at least " + experimentalFactors.size()
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
            log.warn( "Biomaterial not paired with all factor values for each of " + experimentalFactors.size()
                    + " experimental factors.  Found " + seenPairings.size() + " pairings but should have "
                    + factorValuePairings.size() + ".  Incomplete block design." );
            return false;
        }
        return true;

    }

    /**
     * Generates all possible factor value pairings for the given experimental factors.
     * 
     * @param experimentalFactors
     * @return A collection of hashsets, where each hashset is a pairing.
     */
    protected static Collection<Set<FactorValue>> generateFactorValuePairings(
            Collection<ExperimentalFactor> experimentalFactors ) {
        /* set up the possible pairings */
        Collection<FactorValue> allFactorValues = new HashSet<FactorValue>();
        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            allFactorValues.addAll( experimentalFactor.getFactorValues() );
        }

        Collection<FactorValue> allFactorValuesCopy = allFactorValues;

        Collection<Set<FactorValue>> factorValuePairings = new HashSet<Set<FactorValue>>();

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
     * Returns biomaterials with 'filtered' factor values. That is, each biomaterial will only contain those factor
     * values equivalent to a factor value from one of the input experimental factors.
     * <p>
     * FIXME I don't like the way this code modifies the factorvalues associated with the biomaterial.
     * 
     * @param factors
     * @param biomaterials
     * @return Collection<BioMaterial>
     */
    private static Collection<BioMaterial> filterFactorValuesFromBiomaterials( Collection<ExperimentalFactor> factors,
            Collection<BioMaterial> biomaterials ) {

        assert !biomaterials.isEmpty();
        assert !factors.isEmpty();

        Collection<FactorValue> allFactorValuesFromGivenFactors = new HashSet<FactorValue>();
        for ( ExperimentalFactor ef : factors ) {
            allFactorValuesFromGivenFactors.addAll( ef.getFactorValues() );
        }

        Collection<BioMaterial> biomaterialsWithGivenFactorValues = new HashSet<BioMaterial>();
        int numHaveAny = 0;
        for ( BioMaterial b : biomaterials ) {
            Collection<FactorValue> biomaterialFactorValues = b.getFactorValues();
            Collection<FactorValue> factorValuesToConsider = new HashSet<FactorValue>();
            factorValuesToConsider.addAll( biomaterialFactorValues );
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
     * Returns a collection of all the different types of biomaterials across all bioassays in the experiment.
     * 
     * @param expressionExperiment
     * @return
     */
    private static List<BioMaterial> getBioMaterials( BioAssaySet ee ) {

        List<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        for ( BioAssay assay : ( Collection<BioAssay> ) ee.getBioAssays() ) {
            BioMaterial material = assay.getSampleUsed();
            biomaterials.add( material );
        }

        return biomaterials;
    }

    /**
     * Isolate a biomaterial's factor values for a specific factor(s).
     * 
     * @param factors
     * @param biomaterial
     * @return the factor values the biomaterial has for the given factors.
     */
    private static Collection<FactorValue> getRelevantFactorValues( Collection<ExperimentalFactor> factors,
            BioMaterial biomaterial ) {
        Collection<FactorValue> factorValues = biomaterial.getFactorValues();

        Collection<FactorValue> factorValuesToCheck = new HashSet<FactorValue>();
        for ( FactorValue factorValue : factorValues ) {
            if ( factors.contains( factorValue.getExperimentalFactor() ) ) {
                factorValuesToCheck.add( factorValue );
            }
        }
        return factorValuesToCheck;
    }

}

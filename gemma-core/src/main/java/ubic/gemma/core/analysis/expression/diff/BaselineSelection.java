/*
 * The gemma project
 *
 * Copyright (c) 2015 University of British Columbia
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

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.MeasurementUtils;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import javax.annotation.Nullable;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

/**
 * Utilities for deciding if a factor value is a baseline condition.
 *
 * @author paul
 */
@CommonsLog
public class BaselineSelection {

    // see bug 4316. This term is "control"
    private static final String FORCED_BASELINE_VALUE_URI = "http://www.ebi.ac.uk/efo/EFO_0001461";

    /**
     * Values we treat as baseline.
     */
    private static final Set<String> controlGroupTerms = createTermSet(
            "baseline participant role",
            "baseline",
            "control diet",
            "control group",
            "control",
            "initial time point",
            "normal",
            "placebo",
            "reference subject role",
            "reference substance role",
            "to be treated with placebo role",
            "untreated",
            "wild type control",
            "wild type genotype",
            "wild type",
            "female" // alphabetically before male.
    );
    /**
     * Ontology terms we treat as baseline. Checked: 2024-08-12
     */
    private static final Set<String> controlGroupUris = createTermSet(
            "http://purl.obolibrary.org/obo/OBI_0000025", // reference substance
            "http://purl.obolibrary.org/obo/OBI_0000143", // baseline participant role
            "http://purl.obolibrary.org/obo/OBI_0000220",  // reference subject role
            "http://purl.obolibrary.org/obo/OBI_0000825", // to be treated with placebo role
            "http://purl.obolibrary.org/obo/OBI_0100046", // phosphate buffered saline
            "http://www.ebi.ac.uk/efo/EFO_0001461", // control
            "http://www.ebi.ac.uk/efo/EFO_0001674", // placebo
            "http://www.ebi.ac.uk/efo/EFO_0004425",// initial time point
            "http://www.ebi.ac.uk/efo/EFO_0005168", // wild type genotype
            "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201", // control group, old
            "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#wild_type", // retired
            "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2001", // normal control_group (retired)
            "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2201", // control_group, new version (retired)
            "http://purl.obolibrary.org/obo/PATO_0000383" // female
    );

    /**
     * Create an immutable, case-insensitive set.
     */
    private static Set<String> createTermSet( String... terms ) {
        Set<String> c = new TreeSet<>( Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) );
        c.addAll( Arrays.asList( terms ) );
        return Collections.unmodifiableSet( c );
    }

    /**
     * Check if a given factor value indicates a baseline condition.
     */
    public static boolean isBaselineCondition( FactorValue factorValue ) {
        if ( factorValue.getMeasurement() != null ) {
            return false;
        }
        if ( factorValue.getIsBaseline() != null ) {
            return factorValue.getIsBaseline();
        }
        //noinspection deprecation
        return factorValue.getCharacteristics().stream().anyMatch( BaselineSelection::isBaselineCondition )
                // for backwards compatibility we check anyway
                || BaselineSelection.controlGroupTerms.contains( normalizeTerm( factorValue.getValue() ) );
    }

    /**
     * Check if a given statement indicates a baseline condition.
     */
    public static boolean isBaselineCondition( Statement c ) {
        return BaselineSelection.controlGroupUris.contains( c.getSubjectUri() )
                || BaselineSelection.controlGroupUris.contains( c.getObjectUri() )
                || BaselineSelection.controlGroupUris.contains( c.getSecondObjectUri() )
                // free text checks
                || ( c.getSubjectUri() == null && BaselineSelection.controlGroupTerms.contains( normalizeTerm( c.getSubject() ) ) )
                || ( c.getObjectUri() == null && BaselineSelection.controlGroupTerms.contains( normalizeTerm( c.getObject() ) ) )
                || ( c.getSecondObjectUri() == null && BaselineSelection.controlGroupTerms.contains( normalizeTerm( c.getSecondObject() ) ) );
    }

    /**
     * Check if a given characteristic indicate a baseline condition.
     */
    public static boolean isBaselineCondition( Characteristic c ) {
        return BaselineSelection.controlGroupUris.contains( c.getValueUri() )
                || ( c.getValueUri() == null && BaselineSelection.controlGroupTerms.contains( normalizeTerm( c.getValue() ) ) );
    }

    private static String normalizeTerm( String term ) {
        if ( term == null ) {
            return null;
        }
        return normalizeSpace( term.replace( '_', ' ' ) );
    }

    /**
     * Check if this factor value is the baseline, overriding other possible baselines.
     * <p>
     * A baseline can be *forced* in two ways: either by setting {@link FactorValue#setIsBaseline(Boolean)} to true or
     * by adding a characteristic with the {@code FORCED_BASELINE_VALUE_URI} URI. In practice, this is not much
     * different from {@link #isBaselineCondition(Statement)}, but there might be cases where you would want to indicate
     * that the baseline was explicitly forced.
     */
    public static boolean isForcedBaseline( FactorValue fv ) {
        if ( fv.getMeasurement() != null ) {
            return false;
        }
        if ( fv.getIsBaseline() != null ) {
            return fv.getIsBaseline();
        }
        return fv.getCharacteristics().stream().anyMatch( BaselineSelection::isForcedBaseline );
    }

    private static boolean isForcedBaseline( Statement stmt ) {
        return BaselineSelection.FORCED_BASELINE_VALUE_URI.equalsIgnoreCase( stmt.getSubjectUri() )
                || BaselineSelection.FORCED_BASELINE_VALUE_URI.equalsIgnoreCase( stmt.getObjectUri() )
                || BaselineSelection.FORCED_BASELINE_VALUE_URI.equalsIgnoreCase( stmt.getSecondObjectUri() );
    }

    /**
     * Identify the FactorValue that should be treated as 'Baseline' for each of the given factors. This is done
     * heuristically, and if all else fails we choose arbitrarily. For continuous factors, the minimum value is treated
     * as baseline.
     *
     * @param factors     factors
     * @param samplesUsed These are used to make sure we don't bother using factor values as baselines if they are not
     *                    used by any of the samples. This is important for subsets. If null, this is ignored.
     * @return map of factors to the baseline factorvalue for that factor.
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( Collection<ExperimentalFactor> factors, @Nullable Collection<BioMaterial> samplesUsed ) {

        Map<ExperimentalFactor, FactorValue> result = new HashMap<>();

        for ( ExperimentalFactor factor : factors ) {

            if ( factor.getFactorValues().isEmpty() ) {
                throw new IllegalStateException( "Factor has no factor values: " + factor );
            }

            if ( factor.getType().equals( FactorType.CONTINUOUS ) ) {
                // then there is no baseline, but we'll take the minimum value.
                TreeMap<Double, FactorValue> sortedVals = new TreeMap<>();
                for ( FactorValue fv : factor.getFactorValues() ) {

                    /*
                     * Check that this factor value is used by at least one of the given samples. Only matters if this
                     * is a subset of the full data set.
                     */
                    if ( samplesUsed != null && !BaselineSelection.used( fv, samplesUsed ) ) {
                        // this factorValue cannot be a candidate baseline for this subset.
                        continue;
                    }

                    if ( fv.getMeasurement() == null || fv.getMeasurement().getValue() == null ) {
                        // throw new IllegalStateException( "Continuous factors should have Measurements as values" );
                        // This can happen if a value is missing, as nothing would be added to the BioMaterial.
                        BaselineSelection.log.warn( "No value for continuous factor " + factor + " for a sample, will treat as NaN" );
                        sortedVals.put( Double.NaN, fv );
                        continue;
                    }

                    if ( fv.getMeasurement().getValue().isEmpty() ) {
                        BaselineSelection.log.warn( "No value for continuous factor " + factor + " for a sample, will treat as NaN" );
                        sortedVals.put( Double.NaN, fv );
                        continue;
                    }

                    double v = MeasurementUtils.measurement2double( fv.getMeasurement() );
                    sortedVals.put( v, fv );
                }

                if ( sortedVals.isEmpty() ) {
                    BaselineSelection.log.warn( "No values for continuous factor " + factor );
                    continue;
                }
                result.put( factor, sortedVals.firstEntry().getValue() );

            } else {

                for ( FactorValue fv : factor.getFactorValues() ) {

                    /*
                     * Check that this factor value is used by at least one of the given samples. Only matters if this
                     * is a subset of the full data set.
                     */
                    if ( samplesUsed != null && !BaselineSelection.used( fv, samplesUsed ) ) {
                        // this factorValue cannot be a candidate baseline for this subset.
                        continue;
                    }

                    if ( isForcedBaseline( fv ) ) {
                        BaselineSelection.log.debug( "Baseline chosen: " + fv );
                        result.put( factor, fv );
                        break;
                    }

                    if ( isBaselineCondition( fv ) ) {
                        if ( result.containsKey( factor ) ) {
                            BaselineSelection.log.warn( "A second potential baseline was found for " + factor + ": " + fv );
                            continue;
                        }
                        BaselineSelection.log.debug( "Baseline chosen: " + fv );
                        result.put( factor, fv );
                    }
                }

                if ( !result.containsKey( factor ) ) { // fallback
                    FactorValue arbitraryBaselineFV = null;

                    if ( samplesUsed != null ) {
                        // make sure we choose a fv that is actually used (see above for non-arbitrary case)
                        for ( FactorValue fv : factor.getFactorValues() ) {
                            for ( BioMaterial bm : samplesUsed ) {
                                for ( FactorValue bfv : bm.getAllFactorValues() ) {
                                    if ( fv.equals( bfv ) ) {
                                        arbitraryBaselineFV = fv;
                                        break;
                                    }
                                }
                                if ( arbitraryBaselineFV != null )
                                    break;
                            }
                            if ( arbitraryBaselineFV != null )
                                break;
                        }

                        if ( arbitraryBaselineFV == null ) {
                            // If we get here, we had passed in the samples in consideration but none had a value assigned.
                            throw new IllegalStateException(
                                    "None of the samplesUsed have a value for factor:  " + factor + " (" + factor
                                            .getFactorValues().size() + " factor values) - ensure samples are assigned this factor" );
                        }

                    } else {
                        // I'm not sure the use case of this line but it would only be used if we didn't pass in any samples to consider.
                        arbitraryBaselineFV = factor.getFactorValues().iterator().next();
                    }

                    // There's no need to log this for batch factors, they are inherently arbitrary and only used
                    // during batch correction.
                    if ( !ExperimentFactorUtils.isBatchFactor( factor ) ) {
                        BaselineSelection.log.info( "Falling back on choosing baseline arbitrarily: " + arbitraryBaselineFV );
                    }
                    result.put( factor, arbitraryBaselineFV );
                }
            }
        }

        return result;
    }

    /**
     * Identify the FactorValue that should be treated as 'Baseline' for each of the given factors. This is done
     * heuristically, and if all else fails we choose arbitrarily.
     *
     * @param factors factors
     * @return map
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( Collection<ExperimentalFactor> factors ) {
        return getBaselineLevels( factors, null );
    }

    public static Map<ExperimentalFactor, FactorValue> getBaselineConditions( Collection<BioMaterial> samplesUsed,
            Collection<ExperimentalFactor> factors ) {
        Map<ExperimentalFactor, FactorValue> baselineConditions = getBaselineLevels( factors, samplesUsed );

        /*
         * For factors that don't have an obvious baseline, use the first factorvalue.
         */
        Collection<FactorValue> factorValuesOfFirstSample = samplesUsed.iterator().next().getAllFactorValues();
        for ( ExperimentalFactor factor : factors ) {
            if ( !baselineConditions.containsKey( factor ) ) {

                for ( FactorValue biomf : factorValuesOfFirstSample ) {
                    /*
                     * the first biomaterial has the values used as baseline
                     */
                    if ( biomf.getExperimentalFactor().equals( factor ) ) {
                        baselineConditions.put( factor, biomf );
                    }
                }
            }
        }

        /*
         * TODO: for OrganismPart (etc) we should allow there to be no baseline but use the global mean as the reference
         * point.
         */

        return baselineConditions;
    }

    /**
     * @return true if the factorvalue is used by at least one of the samples.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    private static boolean used( FactorValue fv, Collection<BioMaterial> samplesUsed ) {
        for ( BioMaterial bm : samplesUsed ) {
            for ( FactorValue bfv : bm.getAllFactorValues() ) {
                if ( fv.equals( bfv ) ) {
                    return true;
                }
            }
        }
        return false;
    }
}

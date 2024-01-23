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

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

/**
 * Utilities for deciding if a factor value is a baseline condition.
 *
 * @author paul
 */
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
            "wild type"
    );
    /**
     * Ontology terms we treat as baseline.
     */
    private static final Set<String> controlGroupUris = createTermSet(
            "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#wild_type",
            "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2001", // normal control_group (retired)
            "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2201", // control_group, new version (retired)
            "http://purl.obolibrary.org/obo/OBI_0000025", // reference substance
            "http://purl.obolibrary.org/obo/OBI_0000143", // baseline participant role
            "http://purl.obolibrary.org/obo/OBI_0000220",  // reference subject role
            "http://purl.obolibrary.org/obo/OBI_0000825", // to be treated with placebo
            "http://purl.obolibrary.org/obo/OBI_0100046", // phosphate buffered saline
            "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201", // control group, old
            "http://www.ebi.ac.uk/efo/EFO_0001461", // control
            "http://www.ebi.ac.uk/efo/EFO_0001674", // placebo
            "http://www.ebi.ac.uk/efo/EFO_0004425",// initial time point
            "http://www.ebi.ac.uk/efo/EFO_0005168" // wild type genotype
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

}

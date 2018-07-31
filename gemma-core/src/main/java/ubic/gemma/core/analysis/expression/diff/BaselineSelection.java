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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for deciding if a factor value is a baseline condition.
 *
 * @author paul
 */
public class BaselineSelection {
    private static final Set<String> controlGroupTerms = new HashSet<>();
    // see bug 4316. This term is "control"
    private static final String FORCED_BASELINE_VALUE_URI = "http://www.ebi.ac.uk/efo/EFO_0001461".toLowerCase();

    static {
        /*
         * Values or ontology terms we treat as 'baseline'.
         */
        BaselineSelection.controlGroupTerms
                .add( "http://purl.obolibrary.org/obo/OBI_0000025".toLowerCase() ); // - reference substance
        // role

        BaselineSelection.controlGroupTerms
                .add( "http://purl.obolibrary.org/obo/OBI_0000220".toLowerCase() );// - reference subject role
        BaselineSelection.controlGroupTerms
                .add( "http://purl.obolibrary.org/obo/OBI_0000143".toLowerCase() );// - baseline participant
        // role

        BaselineSelection.controlGroupTerms.add( "reference_substance_role" );
        BaselineSelection.controlGroupTerms.add( "reference_subject_role" );
        BaselineSelection.controlGroupTerms.add( "baseline_participant_role" );

        BaselineSelection.controlGroupTerms.add( "control group" );
        BaselineSelection.controlGroupTerms.add( "control" );
        BaselineSelection.controlGroupTerms.add( "normal" );
        BaselineSelection.controlGroupTerms.add( "untreated" );
        BaselineSelection.controlGroupTerms.add( "baseline" );
        BaselineSelection.controlGroupTerms.add( "control_group" );
        BaselineSelection.controlGroupTerms.add( "wild_type" );
        BaselineSelection.controlGroupTerms.add( "wild type" );
        BaselineSelection.controlGroupTerms.add( "wild type genotype" );
        BaselineSelection.controlGroupTerms.add( "initial time point" );

        BaselineSelection.controlGroupTerms.add( "to_be_treated_with_placebo_role" );

        BaselineSelection.controlGroupTerms
                .add( "http://purl.obolibrary.org/obo/OBI_0100046".toLowerCase() ); // phosphate buffered
        // saline.
        BaselineSelection.controlGroupTerms
                .add( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#wild_type".toLowerCase() );

        BaselineSelection.controlGroupTerms
                .add( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201"
                        .toLowerCase() ); // control_group, old.

        BaselineSelection.controlGroupTerms
                .add( "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2201"
                        .toLowerCase() ); // control_group, new version.(retired)

        BaselineSelection.controlGroupTerms
                .add( "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2001"
                        .toLowerCase() ); // " normal control_group", (retired)

        BaselineSelection.controlGroupTerms
                .add( "http://purl.obolibrary.org/obo/OBI_0000825".toLowerCase() ); // - to be treated with
        // placebo

        BaselineSelection.controlGroupTerms
                .add( "http://www.ebi.ac.uk/efo/EFO_0005168".toLowerCase() ); // wild type genotype

        BaselineSelection.controlGroupTerms
                .add( "http://www.ebi.ac.uk/efo/EFO_0004425".toLowerCase() ); // initial time point
    }

    /**
     * @param factorValue
     * @return
     */
    public static boolean isBaselineCondition( FactorValue factorValue ) {

        if ( factorValue.getIsBaseline() != null )
            return factorValue.getIsBaseline();

        // for backwards compatibility we check anyway

        if ( factorValue.getMeasurement() != null ) {
            return false;
        } else if ( factorValue.getCharacteristics().isEmpty() ) {
            /*
             * Just use the value.
             */
            return StringUtils.isNotBlank( factorValue.getValue() ) && BaselineSelection.controlGroupTerms
                    .contains( factorValue.getValue().toLowerCase() );
        } else {
            for ( Characteristic c : factorValue.getCharacteristics() ) {
                if ( isBaselineCondition( c ) )
                    return true;
            }
        }
        return false;
    }

    /**
     * @param c
     * @return true if this looks like a baseline condition
     */
    public static boolean isBaselineCondition( Characteristic c ) {
        String valueUri = c.getValueUri();

        if ( StringUtils.isNotBlank( valueUri ) && BaselineSelection.controlGroupTerms
                .contains( valueUri.toLowerCase() ) ) {
            return true;
        }

        return StringUtils.isNotBlank( c.getValue() ) && BaselineSelection.controlGroupTerms
                .contains( c.getValue().toLowerCase() );
    }

    /**
     * Check if this factor value is the baseline, overriding other possible baselines.
     *
     * @param fv factor value
     * @return true if given fv is forced baseline
     */
    public static boolean isForcedBaseline( FactorValue fv ) {
        if ( fv.getMeasurement() != null || fv.getCharacteristics().isEmpty() ) {
            return false;
        }
        for ( Characteristic c : fv.getCharacteristics() ) {
            String valueUri = c.getValueUri();
            if ( StringUtils.isNotBlank( valueUri ) && valueUri.toLowerCase()
                    .equals( BaselineSelection.FORCED_BASELINE_VALUE_URI ) ) {
                return true;
            }

        }
        return false;
    }

}

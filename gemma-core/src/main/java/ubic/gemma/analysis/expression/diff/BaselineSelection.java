/*
 * The gemma-core project
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

package ubic.gemma.analysis.expression.diff;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

/**
 * Utilities for deciding if a factor value is a baseline condition.
 * 
 * @author paul
 * @version $Id$
 */
public class BaselineSelection {
    public static Set<String> controlGroupTerms = new HashSet<String>();

    // see bug 4316. This term is "control"
    public static String FORCED_BASELINE_VALUE_URI = "http://www.ebi.ac.uk/efo/EFO_0001461".toLowerCase();

    static {
        /*
         * Values or ontology terms we treat as 'baseline'. See also {@link
         * http://www.chibi.ubc.ca/faculty/pavlidis/wiki
         * /display/PavLab/Gemma+Curation+Guidelines#GemmaCurationGuidelines-BaselineFactor%2FControlGroup}
         */
        controlGroupTerms.add( "http://purl.obolibrary.org/obo/OBI_0000025".toLowerCase() ); // - reference substance
        // role

        controlGroupTerms.add( "http://purl.obolibrary.org/obo/OBI_0000220".toLowerCase() );// - reference subject role
        controlGroupTerms.add( "http://purl.obolibrary.org/obo/OBI_0000143".toLowerCase() );// - baseline participant
        // role

        controlGroupTerms.add( "reference_substance_role" );
        controlGroupTerms.add( "reference_subject_role" );
        controlGroupTerms.add( "baseline_participant_role" );

        controlGroupTerms.add( "control group" );
        controlGroupTerms.add( "control" );
        controlGroupTerms.add( "normal" );
        controlGroupTerms.add( "untreated" );
        controlGroupTerms.add( "baseline" );
        controlGroupTerms.add( "control_group" );
        controlGroupTerms.add( "wild_type" );
        controlGroupTerms.add( "wild type" );
        controlGroupTerms.add( "wild type genotype" );
        controlGroupTerms.add( "initial time point" );

        controlGroupTerms.add( "to_be_treated_with_placebo_role" );

        controlGroupTerms.add( "http://purl.obolibrary.org/obo/OBI_0100046".toLowerCase() ); // phosphate buffered
                                                                                             // saline.
        controlGroupTerms.add( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#wild_type".toLowerCase() );

        controlGroupTerms.add( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201"
                .toLowerCase() ); // control_group, old.

        controlGroupTerms.add( "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2201"
                .toLowerCase() ); // control_group, new version.(retired)

        controlGroupTerms.add( "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2001"
                .toLowerCase() ); // " normal control_group", (retired)

        controlGroupTerms.add( "http://purl.obolibrary.org/obo/OBI_0000825".toLowerCase() ); // - to be treated with
                                                                                             // placebo

        controlGroupTerms.add( "http://www.ebi.ac.uk/efo/EFO_0005168".toLowerCase() ); // wild type genotype

        controlGroupTerms.add( "http://www.ebi.ac.uk/efo/EFO_0004425".toLowerCase() ); // initial time point
    }

    /**
     * @param factorValue
     * @return
     */
    public static boolean isBaselineCondition( FactorValue factorValue ) {

        if ( factorValue.getIsBaseline() != null ) return factorValue.getIsBaseline();

        // for backwards compatibility we check anyway

        if ( factorValue.getMeasurement() != null ) {
            return false;
        } else if ( factorValue.getCharacteristics().isEmpty() ) {
            /*
             * Just use the value.
             */
            if ( StringUtils.isNotBlank( factorValue.getValue() )
                    && controlGroupTerms.contains( factorValue.getValue().toLowerCase() ) ) {
                return true;
            }
        } else {
            for ( Characteristic c : factorValue.getCharacteristics() ) {
                if ( c instanceof VocabCharacteristic ) {
                    String valueUri = ( ( VocabCharacteristic ) c ).getValueUri();
                    if ( StringUtils.isNotBlank( valueUri ) && controlGroupTerms.contains( valueUri.toLowerCase() ) ) {
                        return true;
                    }
                    if ( StringUtils.isNotBlank( c.getValue() )
                            && controlGroupTerms.contains( c.getValue().toLowerCase() ) ) {
                        return true;
                    }
                } else if ( StringUtils.isNotBlank( c.getValue() )
                        && controlGroupTerms.contains( c.getValue().toLowerCase() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param factorValue
     * @return
     */
    public static boolean isBaselineConditionVO( FactorValueValueObject factorValue ) {
        if ( factorValue.getIsBaseline() != null ) return factorValue.getIsBaseline();

        // for backwards compatibility we check anyway

        if ( factorValue.isMeasurement() ) {
            return false;
        } else if ( StringUtils.isNotBlank( factorValue.getValue() )
                && BaselineSelection.controlGroupTerms.contains( factorValue.getValue().toLowerCase() ) ) {
            return true;
        }
        return false;
    }

    /**
     * Check if this factor value is the baseline, overriding other possible baselines.
     * 
     * @param fv
     * @return
     */
    public static boolean isForcedBaseline( FactorValue fv ) {
        if ( fv.getMeasurement() != null || fv.getCharacteristics().isEmpty() ) {
            return false;
        }
        for ( Characteristic c : fv.getCharacteristics() ) {
            if ( c instanceof VocabCharacteristic ) {
                String valueUri = ( ( VocabCharacteristic ) c ).getValueUri();
                if ( StringUtils.isNotBlank( valueUri )
                        && valueUri.toLowerCase().equals( BaselineSelection.FORCED_BASELINE_VALUE_URI ) ) {
                    return true;
                }
            } else {
                continue;
            }
        }
        return false;
    }

}

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
package ubic.gemma.web.propertyeditor;

import ubic.gemma.model.genome.biosequence.SequenceType;

import java.beans.PropertyEditorSupport;

/**
 * @author pavlidis
 *
 */
public class SequenceTypePropertyEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
        if ( this.getValue() == null ) {
            return "[Not available]";
        }

        SequenceType type = ( SequenceType ) this.getValue();

        if ( type.equals( SequenceType.EST ) ) {
            return "EST";
        } else if ( type.equals( SequenceType.mRNA ) ) {
            return "mRNA";
        } else if ( type.equals( SequenceType.WHOLE_CHROMOSOME ) ) {
            return "Whole Chromosome";
        } else if ( type.equals( SequenceType.DNA ) ) {
            return "DNA";
        } else if ( type.equals( SequenceType.AFFY_COLLAPSED ) ) {
            return "Collapsed Affymetrix Probes";
        } else if ( type.equals( SequenceType.OLIGO ) ) {
            return "Oligo";
        } else if ( type.equals( SequenceType.AFFY_TARGET ) ) {
            return "Affymetrix Target";
        } else if ( type.equals( SequenceType.AFFY_PROBE ) ) {
            return "Affymetrix Probe";
        } else if ( type.equals( SequenceType.REFSEQ ) ) {
            return "RefSeq";
        } else if ( type.equals( SequenceType.BAC ) ) {
            return "BAC";
        } else if ( type.equals( SequenceType.WHOLE_GENOME ) ) {
            return "Whole Genome";
        } else if ( type.equals( SequenceType.OTHER ) ) {
            return "Other";
        } else if ( type.equals( SequenceType.ORF ) ) {
            return "ORF";
        } else if ( type.equals( SequenceType.DUMMY ) ) {
            return "DUMMY";
        } else {
            return "[Unknown]";
        }

    }

    @Override
    public void setAsText( String text ) throws IllegalArgumentException {
        this.setValue( SequenceType.valueOf( text ) );
    }

}

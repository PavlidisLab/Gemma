/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

/**
 * Stores information about the validation status of an attempted expression experiment load.
 * 
 * @author Paul
 * @version $Id$
 */
public class SimpleExpressionExperimentCommandValidation {

    private boolean quantitationTypeIsValid = true;

    private boolean shortNameIsUnique = true;

    private boolean dataFileIsValidFormat = true;

    private boolean arrayDesignMatchesDataFile = true;

    private int numberOfNonMatchingProbes = 0;

    private Collection<String> nonMatchingProbeNameExamples = new HashSet<String>();

    private String quantitationTypeProblemMessage;

    private String dataFileFormatProblemMessage;

    private String arrayDesignMismatchProblemMessage;

    protected boolean isValid() {
        return quantitationTypeIsValid && shortNameIsUnique && dataFileIsValidFormat && arrayDesignMatchesDataFile;
    }

    protected boolean isQuantitationTypeIsValid() {
        return quantitationTypeIsValid;
    }

    protected void setQuantitationTypeIsValid( boolean quantitationTypeIsValid ) {
        this.quantitationTypeIsValid = quantitationTypeIsValid;
    }

    protected boolean isShortNameIsUnique() {
        return shortNameIsUnique;
    }

    protected void setShortNameIsUnique( boolean shortNameIsUnique ) {
        this.shortNameIsUnique = shortNameIsUnique;
    }

    protected boolean isDataFileIsValidFormat() {
        return dataFileIsValidFormat;
    }

    protected void setDataFileIsValidFormat( boolean dataFileIsValidFormat ) {
        this.dataFileIsValidFormat = dataFileIsValidFormat;
    }

    protected boolean isArrayDesignMatchesDataFile() {
        return arrayDesignMatchesDataFile;
    }

    protected void setArrayDesignMatchesDataFile( boolean arrayDesignMatchesDataFile ) {
        this.arrayDesignMatchesDataFile = arrayDesignMatchesDataFile;
    }

    protected int getNumberOfNonMatchingProbes() {
        return numberOfNonMatchingProbes;
    }

    protected void setNumberOfNonMatchingProbes( int numberOfNonMatchingProbes ) {
        this.numberOfNonMatchingProbes = numberOfNonMatchingProbes;
    }

    protected Collection<String> getNonMatchingProbeNameExamples() {
        return nonMatchingProbeNameExamples;
    }

    protected void setNonMatchingProbeNameExamples( Collection<String> nonMatchingProbeNameExamples ) {
        this.nonMatchingProbeNameExamples = nonMatchingProbeNameExamples;
    }

    protected String getQuantitationTypeProblemMessage() {
        return quantitationTypeProblemMessage;
    }

    protected void setQuantitationTypeProblemMessage( String quantitationTypeProblemMessage ) {
        this.quantitationTypeProblemMessage = quantitationTypeProblemMessage;
    }

    protected String getDataFileFormatProblemMessage() {
        return dataFileFormatProblemMessage;
    }

    protected void setDataFileFormatProblemMessage( String dataFileFormatProblemMessage ) {
        this.dataFileFormatProblemMessage = dataFileFormatProblemMessage;
    }

    protected String getArrayDesignMismatchProblemMessage() {
        return arrayDesignMismatchProblemMessage;
    }

    protected void setArrayDesignMismatchProblemMessage( String arrayDesignMismatchProblemMessage ) {
        this.arrayDesignMismatchProblemMessage = arrayDesignMismatchProblemMessage;
    }

}

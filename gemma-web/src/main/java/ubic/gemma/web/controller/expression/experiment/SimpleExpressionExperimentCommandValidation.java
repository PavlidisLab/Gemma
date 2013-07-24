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

    private Collection<String> nonMatchingProbeNameExamples;

    private String quantitationTypeProblemMessage;

    private String dataFileFormatProblemMessage;

    private String arrayDesignMismatchProblemMessage;

    private int numberMatchingProbes = 0;

    private int numRows = 0;
    private int numColumns = 0;

    private boolean isValid = true;

    public SimpleExpressionExperimentCommandValidation() {
        this.nonMatchingProbeNameExamples = new HashSet<String>();
    }

    public String getArrayDesignMismatchProblemMessage() {
        return arrayDesignMismatchProblemMessage;
    }

    public String getDataFileFormatProblemMessage() {
        return dataFileFormatProblemMessage;
    }

    public Collection<String> getNonMatchingProbeNameExamples() {
        return nonMatchingProbeNameExamples;
    }

    public int getNumberMatchingProbes() {
        return numberMatchingProbes;
    }

    public int getNumberOfNonMatchingProbes() {
        return numberOfNonMatchingProbes;
    }

    public int getNumColumns() {
        return numColumns;
    }

    public int getNumRows() {
        return numRows;
    }

    public String getQuantitationTypeProblemMessage() {
        return quantitationTypeProblemMessage;
    }

    public boolean isArrayDesignMatchesDataFile() {
        return arrayDesignMatchesDataFile;
    }

    public boolean isDataFileIsValidFormat() {
        return dataFileIsValidFormat;
    }

    public boolean isQuantitationTypeIsValid() {
        return quantitationTypeIsValid;
    }

    public boolean isShortNameIsUnique() {
        return shortNameIsUnique;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setArrayDesignMatchesDataFile( boolean arrayDesignMatchesDataFile ) {
        this.arrayDesignMatchesDataFile = arrayDesignMatchesDataFile;
        this.isValid = this.isValid && arrayDesignMatchesDataFile;
    }

    public void setArrayDesignMismatchProblemMessage( String arrayDesignMismatchProblemMessage ) {
        this.arrayDesignMismatchProblemMessage = arrayDesignMismatchProblemMessage;
    }

    public void setDataFileFormatProblemMessage( String dataFileFormatProblemMessage ) {
        this.dataFileFormatProblemMessage = dataFileFormatProblemMessage;
    }

    public void setDataFileIsValidFormat( boolean dataFileIsValidFormat ) {
        this.dataFileIsValidFormat = dataFileIsValidFormat;
        this.isValid = this.isValid && dataFileIsValidFormat;
    }

    public void setNonMatchingProbeNameExamples( Collection<String> nonMatchingProbeNameExamples ) {
        this.nonMatchingProbeNameExamples = nonMatchingProbeNameExamples;
    }

    public void setNumberMatchingProbes( int numberMatchingProbes ) {
        this.numberMatchingProbes = numberMatchingProbes;
    }

    public void setNumberOfNonMatchingProbes( int numberOfNonMatchingProbes ) {
        this.numberOfNonMatchingProbes = numberOfNonMatchingProbes;
    }

    public void setNumColumns( int numColumns ) {
        this.numColumns = numColumns;
    }

    public void setNumRows( int numRows ) {
        this.numRows = numRows;
    }

    public void setQuantitationTypeIsValid( boolean quantitationTypeIsValid ) {
        this.quantitationTypeIsValid = quantitationTypeIsValid;
        this.isValid = this.isValid && quantitationTypeIsValid;
    }

    public void setQuantitationTypeProblemMessage( String quantitationTypeProblemMessage ) {
        this.quantitationTypeProblemMessage = quantitationTypeProblemMessage;
    }

    public void setShortNameIsUnique( boolean shortNameIsUnique ) {
        this.shortNameIsUnique = shortNameIsUnique;
        this.isValid = this.isValid && shortNameIsUnique;
    }

}

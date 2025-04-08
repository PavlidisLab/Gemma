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

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Stores information about the validation status of an attempted expression experiment load.
 *
 * @author Paul
 *
 */
@Data
public class SimpleExpressionExperimentCommandValidation implements Serializable {

    private boolean isValid = true;

    private boolean quantitationTypeIsValid = true;
    private String quantitationTypeProblemMessage;

    private boolean shortNameIsUnique = true;

    private boolean dataFileIsValidFormat = true;
    private String dataFileFormatProblemMessage;

    private boolean arrayDesignMatchesDataFile = true;
    private String arrayDesignMismatchProblemMessage;
    private int numberMatchingProbes = 0;
    private int numberOfNonMatchingProbes = 0;
    private Collection<String> nonMatchingProbeNameExamples = new HashSet<>();
    private int numRows = 0;
    private int numColumns = 0;

    private String otherProblemsMessage;
}

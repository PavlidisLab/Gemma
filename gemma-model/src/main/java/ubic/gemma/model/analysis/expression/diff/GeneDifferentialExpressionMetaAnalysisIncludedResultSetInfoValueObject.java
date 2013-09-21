/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.analysis.expression.diff;

import java.io.Serializable;

/**
 * FIXME name is too long!
 * 
 * @author frances
 * @version $Id: GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject.java,v 1.1 2012/11/28 01:44:28
 *          frances Exp $
 */
public class GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4660741839991839187L;

    private Long experimentId;
    private Long analysisId;
    private Long resultSetId;

    public Long getAnalysisId() {
        return this.analysisId;
    }

    public Long getExperimentId() {
        return this.experimentId;
    }

    public Long getResultSetId() {
        return this.resultSetId;
    }

    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public void setExperimentId( Long experimentId ) {
        this.experimentId = experimentId;
    }

    public void setResultSetId( Long resultSetId ) {
        this.resultSetId = resultSetId;
    }
}

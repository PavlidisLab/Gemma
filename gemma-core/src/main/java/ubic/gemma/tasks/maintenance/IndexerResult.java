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

package ubic.gemma.tasks.maintenance;

import ubic.gemma.job.TaskResult;

/**
 * @author klc
 * @version $Id$
 */
public class IndexerResult extends TaskResult {

    private static final long serialVersionUID = -150285942553712429L;

    private String pathToArrayIndex = null;

    private String pathToBibliographicIndex = null;

    private String pathToBiosequenceIndex = null;

    private String pathToExperimentSetIndex;

    private String pathToExpressionIndex = null;

    private String pathToGeneIndex = null;

    private String pathToGeneSetIndex;

    private String pathToProbeIndex = null;

    public IndexerResult( IndexerTaskCommand command ) {
        super( command, null );
    }

    public String getPathToArrayIndex() {
        return pathToArrayIndex;
    }

    public String getPathToBibliographicIndex() {
        return pathToBibliographicIndex;
    }

    public String getPathToBiosequenceIndex() {
        return pathToBiosequenceIndex;
    }

    /**
     * @return the pathToExperimentSetIndex
     */
    public String getPathToExperimentSetIndex() {
        return pathToExperimentSetIndex;
    }

    public String getPathToExpressionIndex() {
        return pathToExpressionIndex;
    }

    public String getPathToGeneIndex() {
        return pathToGeneIndex;
    }

    /**
     * @return the pathToGeneSetIndex
     */
    public String getPathToGeneSetIndex() {
        return pathToGeneSetIndex;
    }

    public String getPathToProbeIndex() {
        return pathToProbeIndex;
    }

    public void setPathToArrayIndex( String pathToArrayIndex ) {
        this.pathToArrayIndex = pathToArrayIndex;
    }

    public void setPathToBibliographicIndex( String pathToBibliographicIndex ) {
        this.pathToBibliographicIndex = pathToBibliographicIndex;
    }

    public void setPathToBiosequenceIndex( String pathToBiosequenceIndex ) {
        this.pathToBiosequenceIndex = pathToBiosequenceIndex;
    }

    /**
     * @param pathToExperimentSetIndex the pathToExperimentSetIndex to set
     */
    public void setPathToExperimentSetIndex( String pathToExperimentSetIndex ) {
        this.pathToExperimentSetIndex = pathToExperimentSetIndex;
    }

    public void setPathToExpressionIndex( String pathToExpressionIndex ) {
        this.pathToExpressionIndex = pathToExpressionIndex;
    }

    public void setPathToGeneIndex( String pathToGeneIndex ) {
        this.pathToGeneIndex = pathToGeneIndex;
    }

    /**
     * @param pathToGeneSetIndex the pathToGeneSetIndex to set
     */
    public void setPathToGeneSetIndex( String pathToGeneSetIndex ) {
        this.pathToGeneSetIndex = pathToGeneSetIndex;
    }

    public void setPathToProbeIndex( String pathToProbeIndex ) {
        this.pathToProbeIndex = pathToProbeIndex;
    }

    @Override
    public String toString() {

        return "Probe path: " + pathToProbeIndex + " Gene path: " + pathToGeneIndex + " EE path: "
                + pathToExpressionIndex + " AD path " + pathToArrayIndex + " GS path=" + pathToGeneSetIndex
                + " ES path=" + pathToExperimentSetIndex;
    }

}

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
package ubic.gemma.grid.javaspaces.analysis.preprocess;

import ubic.gemma.grid.javaspaces.SpacesCommand;

/**
 * Command object for processing data vectors. Used by spaces.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesProcessedExpressionDataVectorCreateCommand extends SpacesCommand {

    private static final long serialVersionUID = 1L;

    private String accession = null;

    /**
     * @return
     */
    public String getAccession() {
        return accession;
    }

    /**
     * @param accession
     */
    public void setAccession( String accession ) {
        this.accession = accession;
    }

    /**
     * @param taskId
     */
    public SpacesProcessedExpressionDataVectorCreateCommand( String taskId, String accession ) {
        super( taskId );

        this.accession = accession;
    }

}

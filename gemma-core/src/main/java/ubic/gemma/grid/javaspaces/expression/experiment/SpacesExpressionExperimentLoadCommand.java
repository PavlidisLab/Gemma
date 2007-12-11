/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.grid.javaspaces.expression.experiment;

import java.io.Serializable;

import ubic.gemma.grid.javaspaces.SpacesCommand;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand;

/**
 * @author keshav
 * @version $Id$
 */
public class SpacesExpressionExperimentLoadCommand extends SpacesCommand implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private boolean loadPlatformOnly;

    /**
     * Used to turn off 'bioassay to biomaterial' matching.
     */
    private boolean suppressMatching = false;

    private String accession;

    private String arrayDesignName = null;

    private boolean isArrayExpress = false;

    /**
     * Set to true to attempt to remove all unneeded quantitation types during parsing.
     */
    private boolean aggressiveQtRemoval;

    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public boolean isAggressiveQtRemoval() {
        return aggressiveQtRemoval;
    }

    public void setAggressiveQtRemoval( boolean aggressiveQtRemoval ) {
        this.aggressiveQtRemoval = aggressiveQtRemoval;
    }

    public boolean isLoadPlatformOnly() {
        return loadPlatformOnly;
    }

    public void setLoadPlatformOnly( boolean loadPlatformOnly ) {
        this.loadPlatformOnly = loadPlatformOnly;
    }

    public boolean isSuppressMatching() {
        return suppressMatching;
    }

    public void setSuppressMatching( boolean suppressMatching ) {
        this.suppressMatching = suppressMatching;
    }

    /**
     * @param taskId
     * @param command
     */
    public SpacesExpressionExperimentLoadCommand( String taskId, ExpressionExperimentLoadCommand command ) {
        super( taskId );
        this.loadPlatformOnly = command.isLoadPlatformOnly();
        this.suppressMatching = command.isSuppressMatching();
        this.accession = command.getAccession();
        this.aggressiveQtRemoval = command.isAggressiveQtRemoval();
        this.isArrayExpress = command.isArrayExpress();
        this.arrayDesignName = command.getArrayDesignName();
    }

    public boolean isArrayExpress() {
        return isArrayExpress;
    }

    public void setArrayExpress( boolean isArrayExpress ) {
        this.isArrayExpress = isArrayExpress;
    }

    public String getArrayDesignName() {
        return arrayDesignName;
    }

    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

}

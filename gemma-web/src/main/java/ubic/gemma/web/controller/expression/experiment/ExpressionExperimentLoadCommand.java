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
package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;
import java.util.Collection;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Command class for expression experiment loading.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentLoadCommand implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8311146574225719807L;

    private String datasourceName;

    private boolean loadPlatformOnly;

    private String arrayDesignName; // for AE only

    private boolean arrayExpress = false; // default is GEO

    /**
     * Used to turn off 'bioassay to biomaterial' matching.
     */
    private boolean suppressMatching;

    private String accession;

    /**
     * Set to true to attempt to remove all unneeded quantitation types during parsing.
     */
    private boolean aggressiveQtRemoval;

    Collection<ArrayDesign> arrayDesigns;

    /**
     * @return Returns the accession.
     */
    public String getAccession() {
        return this.accession;
    }

    /**
     * @param accession The accession to set.
     * @spring.validator type="required"
     * @spring.validator-args arg0resource="expressionExperiment.accession"
     */
    public void setAccession( String accession ) {
        this.accession = accession;
    }

    /**
     * @return Returns the datasourceName.
     */
    public String getDatasourceName() {
        return this.datasourceName;
    }

    /**
     * @param datasourceName The datasourceName to set.
     */
    public void setDatasourceName( String datasourceName ) {
        this.datasourceName = datasourceName;
    }

    /**
     * @return Returns the loadPlatformOnly.
     */
    public boolean isLoadPlatformOnly() {
        return this.loadPlatformOnly;
    }

    /**
     * @param loadPlatformOnly The loadPlatformOnly to set.
     */
    public void setLoadPlatformOnly( boolean loadPlatformOnly ) {
        this.loadPlatformOnly = loadPlatformOnly;
    }

    public boolean isSuppressMatching() {
        return suppressMatching;
    }

    public void setSuppressMatching( boolean suppressMatching ) {
        this.suppressMatching = suppressMatching;
    }

    public boolean isAggressiveQtRemoval() {
        return aggressiveQtRemoval;
    }

    public void setAggressiveQtRemoval( boolean aggressiveQtRemoval ) {
        this.aggressiveQtRemoval = aggressiveQtRemoval;
    }

    /**
     * @return the arrayDesignName
     */
    public String getArrayDesignName() {
        return arrayDesignName;
    }

    /**
     * @param arrayDesignName the arrayDesignName to set
     */
    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

    /**
     * @return the arrayDesigns
     */
    public Collection<ArrayDesign> getArrayDesigns() {
        return arrayDesigns;
    }

    /**
     * @param arrayDesigns the arrayDesigns to set
     */
    public void setArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    /**
     * @return the arrayExpress
     */
    public boolean isArrayExpress() {
        return arrayExpress;
    }

    /**
     * @param arrayExpress the arrayExpress to set
     */
    public void setArrayExpress( boolean arrayExpress ) {
        this.arrayExpress = arrayExpress;
    }

}

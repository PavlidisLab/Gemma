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
package ubic.gemma.loader.expression;

import java.io.Serializable;
import java.util.Collection;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Command class for expression experiment loading.
 * <p>
 * Note that this is in the core module so that non-web applications can use it for loading data.
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
     * 
     */
    public ExpressionExperimentLoadCommand() {
    }

    /**
     * @param accession
     * @param loadPlatformOnly
     * @param suppressMatching
     * @param aggressiveQtRemoval
     * @param arrayExpress
     */
    public ExpressionExperimentLoadCommand( String accession, boolean loadPlatformOnly, boolean suppressMatching,
            boolean aggressiveQtRemoval, boolean arrayExpress ) {
        super();
        this.accession = accession;
        this.loadPlatformOnly = loadPlatformOnly;
        this.suppressMatching = suppressMatching;
        this.aggressiveQtRemoval = aggressiveQtRemoval;
        this.arrayExpress = arrayExpress;
    }

    /**
     * @return Returns the accession.
     */
    public String getAccession() {
        return this.accession;
    }

    /**
     * @return the arrayDesignName
     */
    public String getArrayDesignName() {
        return arrayDesignName;
    }

    /**
     * @return the arrayDesigns
     */
    public Collection<ArrayDesign> getArrayDesigns() {
        return arrayDesigns;
    }

    /**
     * @return Returns the datasourceName.
     */
    public String getDatasourceName() {
        return this.datasourceName;
    }

    public boolean isAggressiveQtRemoval() {
        return aggressiveQtRemoval;
    }

    /**
     * @return the arrayExpress
     */
    public boolean isArrayExpress() {
        return arrayExpress;
    }

    /**
     * @return Returns the loadPlatformOnly.
     */
    public boolean isLoadPlatformOnly() {
        return this.loadPlatformOnly;
    }

    public boolean isSuppressMatching() {
        return suppressMatching;
    }

    /**
     * @param accession The accession to set.
     * @spring.validator type="required"
     * @spring.validator-args arg0resource="expressionExperiment.accession"
     */
    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setAggressiveQtRemoval( boolean aggressiveQtRemoval ) {
        this.aggressiveQtRemoval = aggressiveQtRemoval;
    }

    /**
     * @param arrayDesignName the arrayDesignName to set
     */
    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

    /**
     * @param arrayDesigns the arrayDesigns to set
     */
    public void setArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    /**
     * @param arrayExpress the arrayExpress to set
     */
    public void setArrayExpress( boolean arrayExpress ) {
        this.arrayExpress = arrayExpress;
    }

    /**
     * @param datasourceName The datasourceName to set.
     */
    public void setDatasourceName( String datasourceName ) {
        this.datasourceName = datasourceName;
    }

    /**
     * @param loadPlatformOnly The loadPlatformOnly to set.
     */
    public void setLoadPlatformOnly( boolean loadPlatformOnly ) {
        this.loadPlatformOnly = loadPlatformOnly;
    }

    public void setSuppressMatching( boolean suppressMatching ) {
        this.suppressMatching = suppressMatching;
    }

}

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
package ubic.gemma.tasks.analysis.expression;

import ubic.gemma.job.TaskCommand;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentLoadTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private String accession;

    /**
     * Set to true to attempt to remove all unneeded quantitation types during parsing.
     */
    private boolean aggressiveQtRemoval;

    private boolean allowArrayExpressDesign = false;

    private boolean allowSuperSeriesLoad = true;

    private boolean allowSubSeriesLoad = true;

    private String arrayDesignName = null;

    private boolean isArrayExpress = false;

    private boolean isSplitByPlatform = false;

    private boolean loadPlatformOnly;

    /**
     * Used to turn off 'bioassay to biomaterial' matching.
     */
    private boolean suppressMatching = false;

    public ExpressionExperimentLoadTaskCommand() {
        super();
    }

    /**
     * @param taskId
     * @param command
     */
    public ExpressionExperimentLoadTaskCommand( boolean loadPlatformOnly, boolean suppressMatching, String accession,
            boolean aggressiveQtRemoval, boolean isArrayExpress, String arrayDesignName ) {
        super();
        this.loadPlatformOnly = loadPlatformOnly;
        this.suppressMatching = suppressMatching;
        this.accession = accession;
        this.aggressiveQtRemoval = aggressiveQtRemoval;
        this.isArrayExpress = isArrayExpress;
        this.arrayDesignName = arrayDesignName;
    }

    public String getAccession() {
        return accession;
    }

    public String getArrayDesignName() {
        return arrayDesignName;
    }

    public boolean isAggressiveQtRemoval() {
        return aggressiveQtRemoval;
    }

    /**
     * @return the allowArrayExpressDesign
     */
    public boolean isAllowArrayExpressDesign() {
        return allowArrayExpressDesign;
    }

    public boolean isAllowSuperSeriesLoad() {
        return this.allowSuperSeriesLoad;
    }

    public boolean isArrayExpress() {
        return isArrayExpress;
    }

    public boolean isLoadPlatformOnly() {
        return loadPlatformOnly;
    }

    public boolean isSplitByPlatform() {
        return isSplitByPlatform;
    }

    public boolean isSuppressMatching() {
        return suppressMatching;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setAggressiveQtRemoval( boolean aggressiveQtRemoval ) {
        this.aggressiveQtRemoval = aggressiveQtRemoval;
    }

    /**
     * @param allowArrayExpressDesign the allowArrayExpressDesign to set
     */
    public void setAllowArrayExpressDesign( boolean allowArrayExpressDesign ) {
        this.allowArrayExpressDesign = allowArrayExpressDesign;
    }

    /**
     * @param allowSuperSeriesLoad the allowSuperSeriesLoad to set
     */
    public void setAllowSuperSeriesLoad( boolean allowSuperSeriesLoad ) {
        this.allowSuperSeriesLoad = allowSuperSeriesLoad;
    }

    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

    public void setArrayExpress( boolean isArrayExpress ) {
        this.isArrayExpress = isArrayExpress;
    }

    public void setLoadPlatformOnly( boolean loadPlatformOnly ) {
        this.loadPlatformOnly = loadPlatformOnly;
    }

    public void setSplitByPlatform( boolean isSplitByPlatform ) {
        this.isSplitByPlatform = isSplitByPlatform;
    }

    public void setSuppressMatching( boolean suppressMatching ) {
        this.suppressMatching = suppressMatching;
    }

    protected boolean isAllowSubSeriesLoad() {
        return allowSubSeriesLoad;
    }

    protected void setAllowSubSeriesLoad( boolean allowSubSeriesLoad ) {
        this.allowSubSeriesLoad = allowSubSeriesLoad;
    }

}

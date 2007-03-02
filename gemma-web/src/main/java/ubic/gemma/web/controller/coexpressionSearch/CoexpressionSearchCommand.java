/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.io.Serializable;

import ubic.gemma.model.genome.Taxon;

/**
 * Expression experiment command object that wraps expression experiment search preferences.
 * 
 * @author joseph
 * @version $Id$
 */
public class CoexpressionSearchCommand implements Serializable {

    private static final long serialVersionUID = 2166768356457316142L;

    private int stringency;
    
    private String searchString = null;
    
    private String eeSearchString = null;

    private boolean suppressVisualizations;
    
    private Taxon taxon;
    
    private String exactSearch = "on";
    
    private String geneIdSearch = "false";
    
    public CoexpressionSearchCommand() {
        this.setTaxon( Taxon.Factory.newInstance() );
    }

    /**
     * @return boolean
     */
    public boolean isSuppressVisualizations() {
        return suppressVisualizations;
    }

    /**
     * @param suppressVisualizations
     */
    public void setSuppressVisualizations( boolean suppressVisualizations ) {
        this.suppressVisualizations = suppressVisualizations;
    }

    /**
     * @return String
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * @param searchString
     */
    public void setSearchString( String searchString ) {
        this.searchString = searchString;
    }

    /**
     * @return int
     */
    public int getStringency() {
        return stringency;
    }

    /**
     * @param stringency
     */
    public void setStringency( int stringency ) {
        this.stringency = stringency;
    }

    /**
     * @return the eeSearchString
     */
    public String getEeSearchString() {
        return eeSearchString;
    }

    /**
     * @param eeSearchString the eeSearchString to set
     */
    public void setEeSearchString( String eeSearchString ) {
        this.eeSearchString = eeSearchString;
    }

    /**
     * @return the taxon
     */
    public Taxon getTaxon() {
        return taxon;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    /**
     * @return the exactSearch
     */
    public String getExactSearch() {
        return exactSearch;
    }

    /**
     * @param exactSearch the exactSearch to set
     */
    public void setExactSearch( String exactSearch ) {
        this.exactSearch = exactSearch;
    }

    /**
     * @return the geneIdSearch
     */
    public String getGeneIdSearch() {
        return geneIdSearch;
    }

    /**
     * @param geneIdSearch the geneIdSearch to set
     */
    public void setGeneIdSearch( String geneIdSearch ) {
        this.geneIdSearch = geneIdSearch;
    }
}
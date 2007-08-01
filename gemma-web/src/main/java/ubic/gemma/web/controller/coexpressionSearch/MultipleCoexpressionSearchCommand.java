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
import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * Expression experiment command object that wraps expression experiment search preferences.
 * 
 * @author luke
 * @version $Id$
 */
public class MultipleCoexpressionSearchCommand implements Serializable {

    private static final long serialVersionUID = 2166768356457316142L;

    private int stringency;
    
    private String geneListString = null;
    
    private String eeSearchString = null;

    private String id = null;
    
    private boolean suppressVisualizations;
    
    private Taxon taxon;
    
    private String exactSearch = "on";
    
    private String geneIdSearch = "false";
    
    private Collection<ExpressionExperiment> toUseEE;
    
    private Collection<Gene> sourceGenes;
    
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId( String id ) {
        this.id = id;
    }

    /**
     * @return the sourceGenes
     */
    public Collection<Gene> getSourceGenes() {
        return sourceGenes;
    }

    /**
     * @param sourceGenes the sourceGenes to set
     */
    public void setSourceGenes( Collection<Gene> sourceGenes ) {
        this.sourceGenes = sourceGenes;
    }

    /**
     * @return the toUseEE
     */
    public Collection<ExpressionExperiment> getToUseEE() {
        return toUseEE;
    }

    /**
     * @param toUseEE the toUseEE to set
     */
    public void setToUseEE( Collection<ExpressionExperiment> toUseEE ) {
        this.toUseEE = toUseEE;
    }

    public MultipleCoexpressionSearchCommand() {
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
    public String getGeneListString() {
        return geneListString;
    }

    /**
     * @param searchString
     */
    public void setGeneListString( String searchString ) {
        this.geneListString = searchString;
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
}
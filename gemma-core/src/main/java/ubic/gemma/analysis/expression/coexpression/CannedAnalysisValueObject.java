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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.Taxon;

/**
 * Holds information about a GeneLinkAnalysis.
 * 
 * @author luke
 * @version $Id$
 */
public class CannedAnalysisValueObject {

    private Long id;

    private String name;

    private String description;

    private Taxon taxon;

    // for when sending to server, to avoid upstream marshallingproblems with taxon.
    private Long taxonId;

    private Integer numDatasets;

    private boolean isVirtual = false;

    private int stringency;

    private Long viewedAnalysisId;

    /*
     * Optional - only for 'heavyweight' version.
     */
    private Collection<Long> datasets;

    public CannedAnalysisValueObject() {
        super();
        this.datasets = new HashSet<Long>();
    }

    public Collection<Long> getDatasets() {
        return datasets;
    }

    public void setDatasets( Collection<Long> datasets ) {
        this.datasets = datasets;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual( boolean isVirtual ) {
        this.isVirtual = isVirtual;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public Integer getNumDatasets() {
        return numDatasets;
    }

    public void setNumDatasets( Integer numDatasets ) {
        this.numDatasets = numDatasets;
    }

    public Long getViewedAnalysisId() {
        return viewedAnalysisId;
    }

    public void setViewedAnalysisId( Long viewedAnalysisId ) {
        this.viewedAnalysisId = viewedAnalysisId;
    }

    public int getStringency() {
        return stringency;
    }

    public void setStringency( int stringency ) {
        this.stringency = stringency;
    }

    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }
}

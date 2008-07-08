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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSetValueObject {

    private Long id;
    private String name;
    private String description;
    private Collection<Long> expressionExperimentIds;
    private Integer numExperiments;
    private boolean modifiable;
    private String taxonName;
    private Long taxonId;

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable( boolean modifiable ) {
        this.modifiable = modifiable;
    }

    public Integer getNumExperiments() {
        return numExperiments;
    }

    public void setNumExperiments( Integer numExperiments ) {
        this.numExperiments = numExperiments;
    }

    public ExpressionExperimentSetValueObject() {
        this.expressionExperimentIds = new HashSet<Long>();
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

    public Collection<Long> getExpressionExperimentIds() {
        return expressionExperimentIds;
    }

    public void setExpressionExperimentIds( Collection<Long> expressionExperimentIds ) {
        this.expressionExperimentIds = expressionExperimentIds;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getTaxonName() {
        return taxonName;
    }

    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

}

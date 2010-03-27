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

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSetValueObject implements Comparable<ExpressionExperimentSetValueObject> {

    private Long id;
    private String name;
    private String description;
    private Collection<Long> expressionExperimentIds;
    private Integer numExperiments;

    /**
     * If modifying the set is contrained by existing analyses.
     */
    private boolean modifiable;

    private String taxonName;
    private Long taxonId;

    private boolean currentUserHasWritePermission = false;

    public ExpressionExperimentSetValueObject() {
        this.expressionExperimentIds = new HashSet<Long>();
    }

    public int compareTo( ExpressionExperimentSetValueObject arg0 ) {
        if ( this.getName() == null || arg0.getName() == null ) return 0;
        return this.getName().compareTo( arg0.getName() );
    }

    public String getDescription() {
        return description;
    }

    public Collection<Long> getExpressionExperimentIds() {
        return expressionExperimentIds;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getNumExperiments() {
        return numExperiments;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public String getTaxonName() {
        return taxonName;
    }

    public boolean isCurrentUserHasWritePermission() {
        return currentUserHasWritePermission;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setCurrentUserHasWritePermission( boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setExpressionExperimentIds( Collection<Long> expressionExperimentIds ) {
        this.expressionExperimentIds = expressionExperimentIds;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setModifiable( boolean modifiable ) {
        this.modifiable = modifiable;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNumExperiments( Integer numExperiments ) {
        this.numExperiments = numExperiments;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
    }

}

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
package ubic.gemma.model.expression.experiment;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.util.EntityUtils;


/**
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentSetValueObject implements Serializable, Comparable<ExpressionExperimentSetValueObject> {

    private static final long serialVersionUID = -6852364688337216390L;
        
    private boolean currentUserHasWritePermission = false;

    private boolean publik;

    private boolean shared;
        
    private String description;
    
    private Collection<Long> expressionExperimentIds;
    
    private Long id;
    
    /**
     * If modifying the set is constrained by existing analyses.
     */
    private boolean modifiable;

    private String name;

    private Integer numExperiments;
    private Long taxonId;

    private String taxonName;
    

    /**
     * @return the publik
     */
    public boolean isPublik() {
        return publik;
    }

    /**
     * @param publik the publik to set
     */
    public void setPublik( boolean publik ) {
        this.publik = publik;
    }

    /**
     * @return the shared
     */
    public boolean isShared() {
        return shared;
    }

    /**
     * @param shared the shared to set
     */
    public void setShared( boolean shared ) {
        this.shared = shared;
    }

    public ExpressionExperimentSetValueObject( ExpressionExperimentSet set ) {
        this.id = set.getId();
        this.name = set.getName();
        this.description = set.getDescription();
        this.taxonId = set.getTaxon().getId();
        this.numExperiments = set.getExperiments().size();
        this.expressionExperimentIds = EntityUtils.getIds( set.getExperiments() );
    }

    public ExpressionExperimentSetValueObject() {
        this.expressionExperimentIds = new HashSet<Long>();
    }

    public int compareTo( ExpressionExperimentSetValueObject arg0 ) {
        if ( this.getName() == null || arg0.getName() == null ) return 0;
        return this.getName().compareTo( arg0.getName() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExpressionExperimentSetValueObject other = ( ExpressionExperimentSetValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
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

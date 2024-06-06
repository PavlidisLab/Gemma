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

import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author tvrossum
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class ExpressionExperimentSetValueObject extends IdentifiableValueObject<ExpressionExperimentSet>
        implements SecureValueObject, Comparable<ExpressionExperimentSetValueObject> {

    private static final long serialVersionUID = -6852364688337216390L;

    private String description = "";
    private Collection<Long> expressionExperimentIds = new HashSet<>();
    private boolean isPublic = false;
    /**
     * If modifying the set is constrained by existing analyses.
     */
    private boolean modifiable = true;
    private String name = "";
    private Integer size = 0;
    private Integer numWithCoexpressionAnalysis = 0;
    private Integer numWithDifferentialExpressionAnalysis = 0;
    private boolean shared = false;
    private Long taxonId;
    private String taxonName;
    private boolean userCanWrite = false;
    private boolean userOwned = false;

    /**
     * Required when using the class as a spring bean.
     */
    public ExpressionExperimentSetValueObject() {
        super();
    }

    public ExpressionExperimentSetValueObject( Long id ) {
        super( id );
        this.expressionExperimentIds = new HashSet<>();
    }

    @Override
    public int compareTo( ExpressionExperimentSetValueObject arg0 ) {
        if ( this.getName() == null || arg0.getName() == null )
            return 0;
        return this.getName().compareTo( arg0.getName() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        ExpressionExperimentSetValueObject other = ( ExpressionExperimentSetValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
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

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    @Override
    public boolean getIsShared() {
        return shared;
    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.shared = isShared;
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        return ExpressionExperimentSet.class;
    }

    @Override
    public boolean getUserCanWrite() {
        return this.userCanWrite;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.userCanWrite = userCanWrite;
    }

    @Override
    public boolean getUserOwned() {
        return this.userOwned;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.userOwned = isUserOwned;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize( Integer numExperiments ) {
        this.size = numExperiments;
    }

    public Integer getNumWithCoexpressionAnalysis() {
        return numWithCoexpressionAnalysis;
    }

    public void setNumWithCoexpressionAnalysis( Integer numWithCoexpressionAnalysis ) {
        this.numWithCoexpressionAnalysis = numWithCoexpressionAnalysis;
    }

    public Integer getNumWithDifferentialExpressionAnalysis() {
        return numWithDifferentialExpressionAnalysis;
    }

    public void setNumWithDifferentialExpressionAnalysis( Integer numWithDifferentialExpressionAnalysis ) {
        this.numWithDifferentialExpressionAnalysis = numWithDifferentialExpressionAnalysis;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public String getTaxonName() {
        return taxonName;
    }

    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable( boolean modifiable ) {
        this.modifiable = modifiable;
    }

}

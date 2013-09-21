/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.analysis.expression.diff;

import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.DiffExpressionEvidenceValueObject;

/**
 * @author frances
 * @version $Id$
 */
public class GeneDifferentialExpressionMetaAnalysisSummaryValueObject implements SecureValueObject {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1856182824742323129L;

    private String description;
    private DiffExpressionEvidenceValueObject diffExpressionEvidence;
    private Long id;
    private boolean isEditable;
    private boolean isOwnedByCurrentUser;
    private boolean isPublic;

    private boolean isShared;
    private String name;
    private Integer numGenesAnalyzed;
    private Integer numResults;

    private Integer numResultSetsIncluded;

    public String getDescription() {
        return this.description;
    }

    public DiffExpressionEvidenceValueObject getDiffExpressionEvidence() {
        return this.diffExpressionEvidence;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public boolean getIsShared() {
        return this.isShared;
    }

    public String getName() {
        return this.name;
    }

    public Integer getNumGenesAnalyzed() {
        return this.numGenesAnalyzed;
    }

    public Integer getNumResults() {
        return this.numResults;
    }

    public Integer getNumResultSetsIncluded() {
        return this.numResultSetsIncluded;
    }

    @Override
    public Class<? extends Securable> getSecurableClass() {
        return GeneDifferentialExpressionMetaAnalysisImpl.class;
    }

    @Override
    public boolean getUserCanWrite() {
        return this.isEditable;
    }

    @Override
    public boolean getUserOwned() {
        return this.isOwnedByCurrentUser;
    }

    @Deprecated
    public boolean isEditable() {
        return this.isEditable;
    }

    @Deprecated
    public boolean isOwnedByCurrentUser() {
        return this.isOwnedByCurrentUser;
    }

    @Deprecated
    public boolean isPublic() {
        return this.isPublic;
    }

    @Deprecated
    public boolean isShared() {
        return this.isShared;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setDiffExpressionEvidence( DiffExpressionEvidenceValueObject diffExpressionEvidence ) {
        this.diffExpressionEvidence = diffExpressionEvidence;
        if ( diffExpressionEvidence != null ) {
            this.diffExpressionEvidence.setGeneDifferentialExpressionMetaAnalysisSummaryValueObject( this );
        }

    }

    @Deprecated
    public void setEditable( boolean isEditable ) {
        this.isEditable = isEditable;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.isShared = isShared;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNumGenesAnalyzed( Integer numGenesAnalyzed ) {
        this.numGenesAnalyzed = numGenesAnalyzed;
    }

    public void setNumResults( Integer numResults ) {
        this.numResults = numResults;
    }

    public void setNumResultSetsIncluded( Integer numResultSetsIncluded ) {
        this.numResultSetsIncluded = numResultSetsIncluded;
    }

    @Deprecated
    public void setOwnedByCurrentUser( boolean isOwnedByCurrentUser ) {
        this.isOwnedByCurrentUser = isOwnedByCurrentUser;
    }

    @Deprecated
    public void setPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    @Deprecated
    public void setShared( boolean isShared ) {
        this.isShared = isShared;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.isEditable = userCanWrite;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.isOwnedByCurrentUser = isUserOwned;
    }
}

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

import java.io.Serializable;

/**
 * @author frances
 * @version $Id$
 */
public class GeneDifferentialExpressionMetaAnalysisSummaryValueObject implements Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1856182824742323129L;

    private String description;
    private Long id;
    private String name;
    private Integer numGenesAnalyzed;
    private Integer numResults;
    private Integer numResultSetsIncluded;
    
    private boolean isOwnedByCurrentUser;
    private boolean isPublic;
    private boolean isShared;

    public String getDescription() {
        return this.description;
    }

    public Long getId() {
        return this.id;
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

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setId( Long id ) {
        this.id = id;
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

	public boolean isOwnedByCurrentUser() {
		return this.isOwnedByCurrentUser;
	}

	public void setOwnedByCurrentUser(boolean isOwnedByCurrentUser) {
		this.isOwnedByCurrentUser = isOwnedByCurrentUser;
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isShared() {
		return this.isShared;
	}

	public void setShared(boolean isShared) {
		this.isShared = isShared;
	}
}

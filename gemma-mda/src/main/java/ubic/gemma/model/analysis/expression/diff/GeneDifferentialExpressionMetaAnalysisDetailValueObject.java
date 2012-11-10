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
import java.util.Collection;

/**
 * @author frances
 * @version $Id$
 */
public class GeneDifferentialExpressionMetaAnalysisDetailValueObject implements Serializable {

	/**
	 * The serial version UID of this class. Needed for serialization.
	 */
	private static final long serialVersionUID = 3868004995989355452L;
	
	private Long id;
	private String name;
	private String description;
	private Integer numGenesAnalyzed;
	private Double qvalueThresholdForStorage;
	private Collection<IncludedResultSetDetail> includedResultSetDetails;
	private Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> results;
	
	public class IncludedResultSetDetail {
		private Long experimentId;
		private Long analysisId;
		private Long resultSetId;
		
		public Long getExperimentId() {
			return this.experimentId;
		}
		public void setExperimentId(Long experimentId) {
			this.experimentId = experimentId;
		}
		public Long getAnalysisId() {
			return this.analysisId;
		}
		public void setAnalysisId(Long analysisId) {
			this.analysisId = analysisId;
		}
		public Long getResultSetId() {
			return this.resultSetId;
		}
		public void setResultSetId(Long resultSetId) {
			this.resultSetId = resultSetId;
		}
	}
	
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getNumGenesAnalyzed() {
		return this.numGenesAnalyzed;
	}
	
	public void setNumGenesAnalyzed(Integer numGenesAnalyzed) {
		this.numGenesAnalyzed = numGenesAnalyzed;
	}
	
	public Double getQvalueThresholdForStorage() {
		return this.qvalueThresholdForStorage;
	}
	
	public void setQvalueThresholdForStorage(Double qvalueThresholdForStorage) {
		this.qvalueThresholdForStorage = qvalueThresholdForStorage;
	}

	public Collection<IncludedResultSetDetail> getIncludedResultSetDetails() {
		return this.includedResultSetDetails;
	}

	public void setIncludedResultSetDetails(
			Collection<IncludedResultSetDetail> includedResultSetDetails) {
		this.includedResultSetDetails = includedResultSetDetails;
	}

	public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> getResults() {
		return this.results;
	}

	public void setResults(	Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> results) {
		this.results = results;
	}
}

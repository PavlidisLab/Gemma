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

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author frances
 * @version $Id$
 */
public class GeneDifferentialExpressionMetaAnalysisResultValueObject implements	Serializable {

	/**
	 * The serial version UID of this class. Needed for serialization.
	 */
	private static final long serialVersionUID = 6099286095885830140L;
	
	private Long id;
	private GeneValueObject gene;
	private Double meanLogFoldChange;
	private Double metaPvalue;
	private Double metaPvalueRank;
	private Double metaQvalue;
	private Integer resultsUsedCount;
	private Boolean upperTail;
	
	public GeneDifferentialExpressionMetaAnalysisResultValueObject(GeneDifferentialExpressionMetaAnalysisResult result) {
		this.id = result.getId();
		this.gene = new GeneValueObject(result.getGene());
		this.meanLogFoldChange = result.getMeanLogFoldChange();
		this.metaPvalue = result.getMetaPvalue();
		this.metaPvalueRank = result.getMetaPvalueRank();
		this.metaQvalue = result.getMetaQvalue();
		this.resultsUsedCount = result.getResultsUsed().size();
		this.upperTail = result.getUpperTail();
	}
	
	public Long getId() {
		return this.id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public GeneValueObject getGene() {
		return this.gene;
	}
	public void setGene(GeneValueObject gene) {
		this.gene = gene;
	}
	public Double getMeanLogFoldChange() {
		return this.meanLogFoldChange;
	}
	public void setMeanLogFoldChange(Double meanLogFoldChange) {
		this.meanLogFoldChange = meanLogFoldChange;
	}
	public Double getMetaPvalue() {
		return this.metaPvalue;
	}
	public void setMetaPvalue(Double metaPvalue) {
		this.metaPvalue = metaPvalue;
	}
	public Double getMetaPvalueRank() {
		return this.metaPvalueRank;
	}
	public void setMetaPvalueRank(Double metaPvalueRank) {
		this.metaPvalueRank = metaPvalueRank;
	}
	public Double getMetaQvalue() {
		return this.metaQvalue;
	}
	public void setMetaQvalue(Double metaQvalue) {
		this.metaQvalue = metaQvalue;
	}
	public Integer getResultsUsedCount() {
		return this.resultsUsedCount;
	}
	public void setResultsUsedCount(Integer resultsUsedCount) {
		this.resultsUsedCount = resultsUsedCount;
	}
	public Boolean getUpperTail() {
		return this.upperTail;
	}
	public void setUpperTail(Boolean upperTail) {
		this.upperTail = upperTail;
	}
}

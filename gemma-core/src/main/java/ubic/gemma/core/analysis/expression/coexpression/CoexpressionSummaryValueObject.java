/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.analysis.expression.coexpression;

import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;

/**
 * @author luke
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class CoexpressionSummaryValueObject {

    private final long geneId;
    // node degree info for this gene, genome wide.
    private GeneCoexpressionNodeDegreeValueObject coexpNodeDegree = null;
    private int datasetsAvailable;
    private int datasetsTested;
    private int linksFound;

    public CoexpressionSummaryValueObject( Long geneId ) {
        this.geneId = geneId;
    }

    /**
     * @return node degree info for this gene, genome wide.
     */
    public GeneCoexpressionNodeDegreeValueObject getCoexpNodeDegree() {
        return coexpNodeDegree;
    }

    /**
     * @param coexpNodeDegree node degree info for this gene, genome wide.
     */
    public void setCoexpNodeDegree( GeneCoexpressionNodeDegreeValueObject coexpNodeDegree ) {
        this.coexpNodeDegree = coexpNodeDegree;
    }

    public int getDatasetsAvailable() {
        return datasetsAvailable;
    }

    public void setDatasetsAvailable( int datasetsAvailable ) {
        this.datasetsAvailable = datasetsAvailable;
    }

    public int getDatasetsTested() {
        return datasetsTested;
    }

    public void setDatasetsTested( int datasetsTested ) {
        this.datasetsTested = datasetsTested;
    }

    public long getGeneId() {
        return geneId;
    }

    public int getLinksFound() {
        return linksFound;
    }

    public void setLinksFound( int linksFound ) {
        this.linksFound = linksFound;
    }

}

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
package ubic.gemma.analysis.expression.coexpression;

import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;

/**
 * @author luke
 * @version $Id$
 */
public class CoexpressionSummaryValueObject {

    // node degree info for this gene, genomewide.
    private GeneCoexpressionNodeDegreeValueObject coexpNodeDegree = null;
    private int datasetsAvailable;
    private int datasetsTested;
    private long geneId;

    // useless
    // private int linksMetNegativeStringency;

    // useless
    // private int linksMetPositiveStringency;

    private int linksFound;

    public CoexpressionSummaryValueObject( Long geneId ) {
        this.geneId = geneId;
    }

    /**
     * @returnnode degree info for this gene, genomewide.
     */
    public GeneCoexpressionNodeDegreeValueObject getCoexpNodeDegree() {
        return coexpNodeDegree;
    }

    public int getDatasetsAvailable() {
        return datasetsAvailable;
    }

    public int getDatasetsTested() {
        return datasetsTested;
    }

    public long getGeneId() {
        return geneId;
    }

    public int getLinksFound() {
        return linksFound;
    }

    // public int getLinksMetNegativeStringency() {
    // return linksMetNegativeStringency;
    // }
    //
    // public int getLinksMetPositiveStringency() {
    // return linksMetPositiveStringency;
    // }

    /**
     * @param coexpNodeDegree node degree info for this gene, genomewide.
     */
    public void setCoexpNodeDegree( GeneCoexpressionNodeDegreeValueObject coexpNodeDegree ) {
        /*
         * FIXME this value object is a bit complex for use in the client...
         */
        this.coexpNodeDegree = coexpNodeDegree;
    }

    public void setDatasetsAvailable( int datasetsAvailable ) {
        this.datasetsAvailable = datasetsAvailable;
    }

    public void setDatasetsTested( int datasetsTested ) {
        this.datasetsTested = datasetsTested;
    }

    // public void setLinksMetNegativeStringency( int linksMetNegativeStringency ) {
    // this.linksMetNegativeStringency = linksMetNegativeStringency;
    // }
    //
    // public void setLinksMetPositiveStringency( int linksMetPositiveStringency ) {
    // this.linksMetPositiveStringency = linksMetPositiveStringency;
    // }

    public void setLinksFound( int linksFound ) {
        this.linksFound = linksFound;
    }

}

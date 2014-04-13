/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.association.coexpression;

import java.io.Serializable;

/**
 * Used to cache results; these objects are unmodifiable, and contains the coexpression data for one query gene and one
 * result gene, in all experiments.
 * 
 * @author paul
 * @version $Id$
 */
public class CoexpressionCacheValueObject implements Serializable {

    private static final long serialVersionUID = 184287422449009209L;

    /*
     * FIXME takes up too much memory? see bug 4034
     */

    private String coexGeneSymbol; // possibly don't store here?
    private String queryGeneSymbol; // possibly don't store here?

    private final Long queryGene;
    private final Long coexpGene;
    private final int support;
    private final boolean positiveCorrelation;

    private Long supportDetailsId;
    private Long[] supportingDatasets = null;
    private CompressedLongSet testedInDatasets = null;

    /**
     * @param vo
     */
    public CoexpressionCacheValueObject( CoexpressionValueObject vo ) {
        if ( vo.isEeConstraint() || vo.getMaxResults() > 0
                || vo.getQueryStringency() > CoexpressionCache.CACHE_QUERY_STRINGENCY ) {
            throw new IllegalArgumentException( "Cannot cache a result that had constraints" );
        }

        this.coexpGene = vo.getCoexGeneId();
        this.queryGene = vo.getQueryGeneId();
        this.support = vo.getNumDatasetsSupporting();
        this.supportDetailsId = vo.getSupportDetailsId();
        this.supportingDatasets = vo.getSupportingDatasets().toArray( new Long[] {} );
        this.testedInDatasets = new CompressedLongSet( vo.getTestedInDatasets() );

        this.queryGeneSymbol = vo.getQueryGeneSymbol();
        this.coexGeneSymbol = vo.getCoexGeneSymbol();
        this.positiveCorrelation = vo.isPositiveCorrelation();

    }

    public CoexpressionValueObject toModifiable() {
        return new CoexpressionValueObject( coexpGene, coexGeneSymbol, positiveCorrelation, queryGene, queryGeneSymbol,
                support, supportDetailsId, this.supportingDatasets, this.testedInDatasets.getValues() );
    }

}

/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Cache of processed data vectors by {@link Gene}.
 * @author Paul
 */
interface ProcessedDataVectorByGeneCache {

    @Nullable
    Collection<DoubleVectorValueObject> get( ExpressionExperiment ee, Long geneId );

    void put( ExpressionExperiment ee, Long geneId, Collection<DoubleVectorValueObject> vectors );

    void putById( Long eeId, Long geneId, Collection<DoubleVectorValueObject> vectors );

    /**
     * Evict all the vectors attached to the given experiment.
     */
    void evict( ExpressionExperiment ee );

    /**
     * Evict all the vectors stored in the cache.
     */
    void clear();
}
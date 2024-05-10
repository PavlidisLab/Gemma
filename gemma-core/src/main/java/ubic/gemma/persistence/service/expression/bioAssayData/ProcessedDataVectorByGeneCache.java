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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Cache of data vectors by {@link Gene}.
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface ProcessedDataVectorByGeneCache {

    @Nullable
    Collection<DoubleVectorValueObject> get( BioAssaySet bas, Gene g );

    @Nullable
    Collection<DoubleVectorValueObject> getById( Long basId, Long gId );

    void put( BioAssaySet bas, Gene gene, Collection<DoubleVectorValueObject> collection );

    void putById( Long basId, Long geneId, Collection<DoubleVectorValueObject> collection );

    /**
     * Evict all the vectors attached to the given experiment.
     */
    void evict( BioAssaySet bas );

    void evictById( Long basId );

    /**
     * Evict all the vectors stored in the cache.
     */
    void clear();
}
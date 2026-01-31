/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.persistence.service.association.coexpression;

import ubic.gemma.model.genome.Gene;

import java.util.Collection;

/**
 * For internal use. A queue of genes lined up for querying so the cache is warmed up. Genes are added to the queue if
 * they were not queried originally in a suitable "unrestricted" way usable across any query for that gene.
 *
 * @author Paul
 */
interface CoexpressionQueryQueue {

    void addToFullQueryQueue( Collection<Gene> genes );

    void addToFullQueryQueue( Gene gene );

    /**
     * Remove genes from the queue; for example if we know their data is about to become stale.
     */
    void removeFromQueue( Collection<Gene> genes );

}

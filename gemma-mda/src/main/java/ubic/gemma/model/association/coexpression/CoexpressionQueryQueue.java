/*
 * The gemma-mda project
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

package ubic.gemma.model.association.coexpression;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;

/**
 * For internal use. A queue of genes lined up for querying so the cache is warmed up. Genes are added to the queue if
 * they were not queried originally in a suitable "unrestricted" way usable across any query for that gene.
 * 
 * @author Paul
 * @version $Id$
 */
public interface CoexpressionQueryQueue {

    /**
     * @param geneIds
     * @param className
     */
    void addToFullQueryQueue( Collection<Long> geneIds, String className );

    /**
     * @param gene
     */
    void addToFullQueryQueue( Gene gene );

    /**
     * @param geneId
     * @param className
     */
    void addToFullQueryQueue( Long geneId, String className );

    /**
     * 
     */
    void queryForCache( QueuedGene gene );

    /**
     * Remove genes from the queue; for example if we know their data is about to become stale.
     * 
     * @param geneIds
     * @param className
     */
    void removeFromQueue( Collection<Long> geneIds, String className );

}

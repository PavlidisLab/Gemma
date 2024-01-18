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
package ubic.gemma.core.search;

import ubic.gemma.core.genome.gene.GOGroupValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author paul
 */
public interface GeneSetSearch {

    /**
     * @param gene gene
     * @return gene sets
     * @see ubic.gemma.core.genome.gene.service.GeneSetService#findByGene(ubic.gemma.model.genome.Gene)
     */
    Collection<GeneSet> findByGene( Gene gene );

    /**
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is not persistent.
     *
     * @param goId    go id
     * @param taxonId taxon id
     * @return a GeneSet or null if nothing is found
     */
    @Nullable
    GOGroupValueObject findGeneSetValueObjectByGoId( String goId, @Nullable Long taxonId );

    /**
     * Finds gene sets by exact match to goTermId eg: GO:0000002 Note: the gene set returned is not persistent.
     *
     * @param goId  go id
     * @param taxon taxon
     * @return a GeneSet or null if nothing is found
     */
    @Nullable
    GeneSet findByGoId( String goId, @Nullable Taxon taxon );

    /**
     * finds gene sets by go term name eg: "trans-hexaPrenylTransTransferase activity" Note: the gene sets returned are
     * not persistent
     *
     * @param goTermName go term name
     * @param taxon      taxon
     * @return a collection with the hits
     */
    Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon ) throws SearchException;

    /**
     * finds genesets by go term name eg: "trans-hexaPrenylTransTransferase activity" Note: the gene sets returned are
     * not persistent
     *
     * @param goTermName          go term name
     * @param taxon               taxon
     * @param maxGoTermsProcessed max go terms
     * @param maxGeneSetSize      max gene set size
     * @return a collection with the hits
     */
    Collection<GeneSet> findByGoTermName( String goTermName, @Nullable Taxon taxon, @Nullable Integer maxGoTermsProcessed,
            @Nullable Integer maxGeneSetSize ) throws SearchException;

    /**
     * @param name name
     * @return gene sets
     * @see ubic.gemma.core.genome.gene.service.GeneSetService#findByName(java.lang.String)
     */
    Collection<GeneSet> findByName( String name );

    /**
     * @param name  name
     * @param taxon taxon
     * @return gene sets
     */
    Collection<GeneSet> findByName( String name, Taxon taxon );

    /**
     * Similar to method of same name in GeneSetController.java but here: - no taxon needed - GO groups always searched
     * - GeneSet objects returned instead of GeneSetValueObjects
     *
     * @param query   string to match to a gene set.
     * @param taxonId taxon id
     * @return collection of GeneSet
     */
    @SuppressWarnings("unused")
    // Possible external use
    Collection<GeneSet> findGeneSetsByName( String query, Long taxonId ) throws SearchException;


}
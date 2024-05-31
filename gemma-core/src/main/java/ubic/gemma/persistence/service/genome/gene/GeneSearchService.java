/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package ubic.gemma.persistence.service.genome.gene;

import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResultDisplayObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Service for searching genes (and gene sets)
 *
 * @author tvrossum
 */
public interface GeneSearchService {

    /**
     * get all genes in the given taxon that are annotated with the given go id, including its child terms in the
     * hierarchy
     *
     * @param goId    GO id that must be in the format "GO_#######"
     * @param taxonId must not be null and must correspond to a taxon
     * @return empty if goId was blank or taxonId didn't correspond to a taxon
     */
    Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId );

    Collection<Gene> getGOGroupGenes( String goQuery, Taxon taxon ) throws SearchException;

    Collection<SearchResultDisplayObject> searchGenesAndGeneGroups( String query, Long taxonId ) throws SearchException;

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     *
     * @param query   A list of gene names (symbols), one per line.
     * @param taxonId taxon id
     * @return collection of gene value objects
     * @throws IOException when there are IO problems
     */
    Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException, SearchException;

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     *
     * @param query   gene names (symbols)
     * @param taxonId taxon id
     * @return query with match. Null values means nothing was found for that key (query)
     */
    Map<String, GeneValueObject> searchMultipleGenesGetMap( Collection<String> query, Long taxonId ) throws SearchException;

}

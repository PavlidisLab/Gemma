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
package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.Future;

/**
 * @author paul
 */
public interface HomologeneService {

    /**
     * @param taxon desired taxon to find homologue in
     * @param gene  gene
     * @return Finds the homologue of the given gene for the taxon specified, or null if there is no homologue
     */
    Gene getHomologue( Gene gene, Taxon taxon );

    /**
     * @param gene gene
     * @return Collection of genes found in Gemma that are homologous with the given gene. Empty if no homologues or
     * gene lacks homologue information, or null if not ready.
     */
    Collection<Gene> getHomologues( Gene gene );

    /**
     * @param ncbiId gene ncbi id
     * @return A collection of NCBI gene ids that are homologous with the given NCBI Gene Id
     */
    Collection<Long> getHomologues( Long ncbiId );

    /**
     * @param taxonCommonName desired taxon to find homologue in
     * @param geneId          gene id
     * @return Finds the homologue of the given gene for the taxon specified, or null if there is no homologue
     */
    GeneValueObject getHomologueValueObject( Long geneId, String taxonCommonName );

    Collection<Long> getNCBIGeneIdsInGroup( long homologeneGroupId );

    /**
     * Refresh homologene data.
     */
    void refresh() throws IOException;
}
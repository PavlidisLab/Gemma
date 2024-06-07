/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

/**
 * The interface for managing groupings of genes.
 *
 * @author kelsey
 */
@ParametersAreNonnullByDefault
public interface GeneSetDao extends BaseVoEnabledDao<GeneSet, DatabaseBackedGeneSetValueObject> {

    /**
     * This method does not do any permissions filtering. It assumes that id the user can see the set, they can see all
     * the members.
     *
     * @param id gene set id
     * @return integer count of genes in set
     */
    int getGeneCount( Long id );

    /**
     * Returns the taxon of an arbitrary member of the set.
     * @return the taxon, or null if the gene set does not have any member
     */
    @Nullable
    Taxon getTaxon( GeneSet geneSet );

    /**
     * Return all the taxa of the gene set members.
     */
    List<Taxon> getTaxa( GeneSet geneSet );

    DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id );

    List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> geneSetIds );

    Collection<GeneSet> findByGene( Gene gene );

    /**
     * @param name uses the given name to do a name* search in the db
     * @return a collection of geneSets that match the given search term.
     */
    Collection<GeneSet> findByName( String name );

    Collection<GeneSet> findByName( String name, @Nullable Taxon taxon );

    Collection<GeneSet> loadAll( @Nullable Taxon tax );

    /**
     * @param geneSet gene set
     */
    void thaw( GeneSet geneSet );

    int removeAll();
}

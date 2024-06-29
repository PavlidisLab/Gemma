/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GOGroupValueObject;
import ubic.gemma.model.genome.gene.GeneSet;

import java.util.Collection;
import java.util.List;

/**
 * * @author tvrossum
 */
public interface GeneSetValueObjectHelper {

    /**
     * Method to create a GO group object from an ad hoc entity
     *
     * @param goId       gene ontology id
     * @param gs         gene set
     * @param searchTerm search term
     * @return Go group VO
     */
    GOGroupValueObject convertToGOValueObject( GeneSet gs, @Nullable String goId, String searchTerm );

    /**
     * Constructor to build value object from GeneSet. This is a light version and *does not include member ids*! (But
     * the size is set.) No security filtering is done here, assuming that if the user could load the experimentSet
     * entity, they have access to it.
     *
     * @param gs the expressionExperimentSet entity to create a value object for
     * @return a gene set value object with all fields filled except for gene members
     */
    DatabaseBackedGeneSetValueObject convertToLightValueObject( GeneSet gs );

    /**
     * results will be sorted by size
     *
     * @param genesets                gene sets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be returned.
     * @return list of gene set value objects
     */
    List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes );

    /**
     * Constructor to build value object from GeneSet. No security filtering is done here, assuming that if the user
     * could load the experimentSet entity, they have access to it.
     *
     * @param gs an expressionExperimentSet entity to create a value object for
     * @return a new DatabaseBackedGeneSetValueObject
     */
    DatabaseBackedGeneSetValueObject convertToValueObject( GeneSet gs );

    /**
     * @param sets gene sets
     * @return results will be sorted by size gene sets that lack genes will be excluded
     */
    List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> sets );

    /**
     * @param geneSets                gene sets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be returned.
     * @return results will be sorted by size
     */
    List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> geneSets,
            boolean includeOnesWithoutGenes );

}
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
package ubic.gemma.genome.gene;

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.genome.gene.GeneSet;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public interface GeneSetValueObjectHelper {

    /**
     * Constructor to build value object from GeneSet. No security filtering is done here, assuming that if the user
     * could load the experimentSet entity, they have access to it.
     * 
     * @param gs an expressionExperimentSet entity to create a value object for
     */
    public abstract DatabaseBackedGeneSetValueObject convertToValueObject( GeneSet gs );

    /**
     * Constructor to build value object from GeneSet. This is a light version and *does not include member ids*! (But
     * the size is set.) No security filtering is done here, assuming that if the user could load the experimentSet
     * entity, they have access to it.
     * 
     * @param gs an expressionExperimentSet entity to create a value object for
     * @return a gene set value object with all fields filled except for gene members
     */
    public abstract DatabaseBackedGeneSetValueObject convertToLightValueObject( GeneSet gs );

    /**
     * results will be sorted by size gene sets that lack genes will be excluded
     * 
     * @see ubic.gemma.genome.gene.service.GeneSetServiceImpl.convertToValueObjects(Collection<GeneSet>, boolean) if you
     *      want empty sets returned
     * @param sets
     * @return
     */
    public abstract List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> sets );

    /**
     * results will be sorted by size
     * 
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be returned.
     * @return
     */
    public abstract List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes );

    /**
     * results will be sorted by size
     * 
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be returned.
     * @return
     */
    public abstract List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes );

    /**
     * Method to create a GO group object from an ad hoc entity
     */
    public abstract GOGroupValueObject convertToGOValueObject( GeneSet gs, String goId, String searchTerm );

    

}
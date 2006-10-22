/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.association;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociationService
 */
public class Gene2GOAssociationServiceImpl extends ubic.gemma.model.association.Gene2GOAssociationServiceBase {

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#find(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected ubic.gemma.model.association.Gene2GOAssociation handleFind(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().find( gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#create(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected ubic.gemma.model.association.Gene2GOAssociation handleCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return ( Gene2GOAssociation ) this.getGene2GOAssociationDao().create( gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findOrCreate(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected ubic.gemma.model.association.Gene2GOAssociation handleFindOrCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().findOrCreate( gene2GOAssociation );
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected java.util.Collection handleFindByGene( ubic.gemma.model.genome.Gene gene ) throws java.lang.Exception {
        // @todo implement protected java.util.Collection handleFindByGene(ubic.gemma.model.genome.Gene gene)
        return null;
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findByGOTerm(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected java.util.Collection handleFindByGOTerm( ubic.gemma.model.common.description.OntologyEntry goTerm )
            throws java.lang.Exception {
        // @todo implement protected java.util.Collection
        // handleFindByGOTerm(ubic.gemma.model.common.description.OntologyEntry goTerm)
        return null;
    }

}
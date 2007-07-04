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

package ubic.gemma.model.association;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociationService
 * @author klc
 * @version $Id$
 */
public class Gene2GOAssociationServiceImpl extends ubic.gemma.model.association.Gene2GOAssociationServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationServiceBase#handleFindByGOTerm(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByGOTerm( String goID, Taxon taxon ) throws Exception {
        return this.getGene2GOAssociationDao().findByGoTerm( goID, taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationServiceBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        return this.getGene2GOAssociationDao().findByGene( gene );

    }

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

}
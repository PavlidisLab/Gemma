/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;

/**
 * @author kelsey
 * @version $Id$
 */
public interface Gene2GOAssociationService {

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public ubic.gemma.model.association.Gene2GOAssociation create(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation );

    /**
     * 
     */
    public ubic.gemma.model.association.Gene2GOAssociation find(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation );

    /**
     * Returns all the Gene2GoAssociation's for the given Gene
     */
    public java.util.Collection<Gene2GOAssociation> findAssociationByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public java.util.Collection<VocabCharacteristic> findByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * <p>
     * Returns all the genes that have the given GoTerms or any of the given goterms children.
     * </p>
     */
    public java.util.Collection<Gene> findByGOTerm( java.lang.String goID, ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public ubic.gemma.model.association.Gene2GOAssociation findOrCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation );

    /**
     * <p>
     * Delete all Gene2GO associations from the system (done prior to an update
     * </p>
     */
    @Secured( { "GROUP_ADMIN" })
    public void removeAll();

}

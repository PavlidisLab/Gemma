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

import java.util.Collection;
import java.util.Map;

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
    @Secured({ "GROUP_ADMIN" })
    public Gene2GOAssociation create( Gene2GOAssociation gene2GOAssociation );

    /**
     * 
     */
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation );

    /**
     * Returns all the Gene2GoAssociations for the given Gene
     */
    public Collection<Gene2GOAssociation> findAssociationByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public Collection<VocabCharacteristic> findByGene( ubic.gemma.model.genome.Gene gene );

    public Map<Gene, Collection<VocabCharacteristic>> findByGenes( Collection<Gene> genes );

    public Collection<Gene> findByGOTerm( java.lang.String goID );

    /**
     * Returns all the genes that have the given GoTerms or any of the given goterms children.
     */
    public Collection<Gene> findByGOTerm( java.lang.String goID, ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation );

    /**
     * Delete all Gene2GO associations from the system (done prior to an update)
     */
    @Secured({ "GROUP_ADMIN" })
    public void removeAll();

}

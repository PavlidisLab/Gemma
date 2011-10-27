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
package ubic.gemma.model.genome.gene;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.genome.Gene;

/**
 * @author kelsey
 * @version $Id$
 */
public interface GeneProductService {

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public ubic.gemma.model.genome.gene.GeneProduct create( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void delete( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * 
     */
    public ubic.gemma.model.genome.gene.GeneProduct find( ubic.gemma.model.genome.gene.GeneProduct gProduct );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * Returns all the genes that share the given gene product name
     * 
     * @param search
     * @return
     */
    public java.util.Collection<Gene> getGenesByName( java.lang.String search );

    /**
     * Returns all the genes that share the given gene product ncbi id
     * 
     * @param search
     * @return
     */
    public java.util.Collection<Gene> getGenesByNcbiId( java.lang.String search );

    /**
     * Returns all the genes that share the given gene product id
     * 
     * @param id
     * @return
     */
    public ubic.gemma.model.genome.gene.GeneProduct load( java.lang.Long id );

    /**
     * <p>
     * loads geneProducts specified by the given ids.
     * </p>
     */
    public java.util.Collection<GeneProduct> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void update( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    public GeneProduct thaw( GeneProduct existing );

    @Secured({ "GROUP_ADMIN" })
    public void remove( Collection<GeneProduct> toRemove );

}

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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public interface GeneProductDao extends BaseDao<GeneProduct> {

    public java.util.Collection<GeneProduct> findByNcbiId( String ncbiId );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public ubic.gemma.model.genome.gene.GeneProduct find( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * 
     */
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenesByName( java.lang.String search );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenesByNcbiId( java.lang.String search );

    @Override
    public Collection<GeneProduct> load( Collection<Long> ids );

    public GeneProduct thaw( GeneProduct existing );

}

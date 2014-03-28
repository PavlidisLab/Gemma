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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.BaseDao;

/**
 * @see GeneProduct
 */
public interface GeneProductDao extends BaseDao<GeneProduct> {

    public GeneProduct findByNcbiId( String ncbiGi );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public GeneProduct find( GeneProduct geneProduct );

    /**
     * 
     */
    public GeneProduct findOrCreate( GeneProduct geneProduct );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenesByName( String search );

    /**
     * TODO: this really should return a unique gene only.
     */
    public Collection<Gene> getGenesByNcbiId( String search );

    public GeneProduct thaw( GeneProduct existing );

    public Collection<GeneProduct> findByName( String name, Taxon taxon );

}

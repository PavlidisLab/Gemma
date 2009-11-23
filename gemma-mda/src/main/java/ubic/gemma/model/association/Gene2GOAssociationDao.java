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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociation
 */
public interface Gene2GOAssociationDao extends BaseDao<Gene2GOAssociation> {

    /**
     * 
     */
    public ubic.gemma.model.association.Gene2GOAssociation find(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation );

    /**
     * <p>
     * Returns the Gene2GoAssociation's associated with the given Gene
     * </p>
     */
    public java.util.Collection<Gene2GOAssociation> findAssociationByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public java.util.Collection<Gene2GOAssociation> findByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * <p>
     * Return all genes for the given taxon that have the given GO id associated.
     * </p>
     */
    public java.util.Collection<Gene> findByGoTerm( java.lang.String goId, ubic.gemma.model.genome.Taxon taxon );

    /**
     * <p>
     * Given a collection of GO Objects returns a colllection of genes that have any of the given goterms
     * </p>
     */
    public java.util.Collection<Gene> findByGOTerm( java.util.Collection goTerms, ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    public ubic.gemma.model.association.Gene2GOAssociation findOrCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation );

    public void removeAll();

}

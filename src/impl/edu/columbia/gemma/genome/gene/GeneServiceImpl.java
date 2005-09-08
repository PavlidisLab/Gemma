/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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

package edu.columbia.gemma.genome.gene;

import java.util.Collection;
import java.util.Iterator;
import edu.columbia.gemma.genome.Gene;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.genome.gene.GeneService
 */
public class GeneServiceImpl extends edu.columbia.gemma.genome.gene.GeneServiceBase {

    /**
     * This was created because calling saveGene with an existant gene actually causes a caching error in Spring.
     * 
     * @see edu.columbia.gemma.genome.gene.GeneService#updateGene(edu.columbia.gemma.genome.Gene)
     */
    protected edu.columbia.gemma.genome.Gene handleUpdateGene( edu.columbia.gemma.genome.Gene gene )
            throws java.lang.Exception {
        this.getGeneDao().update( gene );
        return gene;
    }

    /**
     * This was created because calling saveGene from Spring causes caching errors. I left saveGene in place on the
     * assumption that Kiran's loaders use it with success.
     * 
     * @see edu.columbia.gemma.genome.gene.GeneService#createGene(edu.columbia.gemma.genome.Gene)
     */
    protected edu.columbia.gemma.genome.Gene handleSaveGene( edu.columbia.gemma.genome.Gene gene )
            throws java.lang.Exception {
        this.getGeneDao().create( gene );
        return gene;
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#removeGene(java.lang.String)
     */
    protected void handleRemoveGene( java.lang.String officialName ) throws java.lang.Exception {
        java.util.Collection col = this.getGeneDao().findByOfficialName( officialName );
        Iterator iter = col.iterator();
        while ( iter.hasNext() ) {
            Gene g = ( Gene ) iter.next();
            this.getGeneDao().remove( g );
        }
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#findByOfficialName(java.lang.String)
     */
    protected java.util.Collection handleFindByOfficialName( java.lang.String officialName ) throws java.lang.Exception {
        return this.getGeneDao().findByOfficialName( officialName );
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#findByOfficialSymbol(java.lang.String)
     */
    protected java.util.Collection handleFindByOfficialSymbol( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficalSymbol( officialSymbol );
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#handleFindByOfficialSymbolInexact(java.lang.String)
     */
    protected java.util.Collection handleFindByOfficialSymbolInexact( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialSymbolInexact( officialSymbol );
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#findAllQtlsByPhysicalMapLocation(edu.columbia.gemma.genome.PhysicalLocation)
     */
    protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(
            edu.columbia.gemma.genome.PhysicalLocation physicalMapLocation ) throws java.lang.Exception {
        return this.getGeneDao().findByPhysicalLocation( physicalMapLocation );
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#handleFindByID(java.lang.long)
     */
    protected edu.columbia.gemma.genome.Gene handleFindByID( long id ) throws java.lang.Exception {
        return this.getGeneDao().findByID( id );
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneServiceBase#handleGetAllGenes()
     */
    @Override
    protected Collection handleGetAllGenes() throws Exception {
        return this.getGeneDao().loadAll();
    }

}
/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.dummy.sequence.gene;

/**
 * @see edu.columbia.gemma.sequence.gene.GeneService
 */
public class GeneServiceDummyImpl
    extends edu.columbia.gemma.sequence.gene.GeneServiceBase
{

    /**
     * @see edu.columbia.gemma.sequence.gene.GeneService#saveGene(edu.columbia.gemma.sequence.gene.Gene)
     */
    protected void handleSaveGene(edu.columbia.gemma.sequence.gene.Gene gene)
        throws java.lang.Exception
    {
        //@todo implement protected void handleSaveGene(edu.columbia.gemma.sequence.gene.Gene gene)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.sequence.gene.GeneService.handleSaveGene(edu.columbia.gemma.sequence.gene.Gene gene) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.sequence.gene.GeneService#getAllGenes()
     */
    protected java.util.Collection handleGetAllGenes()
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleGetAllGenes()
        return null;
    }

    /**
     * @see edu.columbia.gemma.sequence.gene.GeneService#removeGene(java.lang.String)
     */
    protected void handleRemoveGene(java.lang.String officialName)
        throws java.lang.Exception
    {
        //@todo implement protected void handleRemoveGene(java.lang.String officialName)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.sequence.gene.GeneService.handleRemoveGene(java.lang.String officialName) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.sequence.gene.GeneService#findByOfficialName(java.lang.String)
     */
    protected edu.columbia.gemma.sequence.gene.Gene handleFindByOfficialName(java.lang.String officialName)
        throws java.lang.Exception
    {
        //@todo implement protected edu.columbia.gemma.sequence.gene.Gene handleFindByOfficialName(java.lang.String officialName)
        return getGeneDao().findByOfficalName(officialName);
    }

    /**
     * @see edu.columbia.gemma.sequence.gene.GeneService#findAllQtlsByPhysicalMapLocation(java.lang.String)
     */
    protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(int physicalMapLocation)
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(java.lang.String physicalMapLocation)
//       if (getGenomicRoiDao() == null) throw new IllegalStateException("Null DAO");
//       if (physicalMapLocation == null) throw new IllegalStateException("physical map location is null");
        return getGenomicRoiDao().findAllQtlsByPhysicalMapLocation(physicalMapLocation);
    }

}
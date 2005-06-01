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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.genome.gene;

/**
 * @see edu.columbia.gemma.genome.gene.GeneService
 */
public class GeneServiceImpl
    extends edu.columbia.gemma.genome.gene.GeneServiceBase
{

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#saveGene(edu.columbia.gemma.genome.Gene)
     */
    protected void handleSaveGene(edu.columbia.gemma.genome.Gene gene)
        throws java.lang.Exception
    {
        //@todo implement protected void handleSaveGene(edu.columbia.gemma.genome.Gene gene)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.GeneService.handleSaveGene(edu.columbia.gemma.genome.Gene gene) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#getAllGenes()
     */
    protected java.util.Collection handleGetAllGenes()
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleGetAllGenes()
        return this.getGeneDao().findAllGenes();
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#removeGene(java.lang.String)
     */
    protected void handleRemoveGene(java.lang.String officialName)
        throws java.lang.Exception
    {
        //@todo implement protected void handleRemoveGene(java.lang.String officialName)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.genome.gene.GeneService.handleRemoveGene(java.lang.String officialName) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#findByOfficialName(java.lang.String)
     */
    protected java.util.Collection handleFindByOfficialName(java.lang.String officialName)
        throws java.lang.Exception
    {
    	return this.getGeneDao().findByOfficialName(officialName);
    }
    
    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#findByOfficialSymbol(java.lang.String)
     */
    protected java.util.Collection handleFindByOfficialSymbol(java.lang.String officialSymbol)
        throws java.lang.Exception
    {
    	return this.getGeneDao().findByOfficalSymbol(officialSymbol);
    }    

    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#handleFindByOfficialSymbolInexact(java.lang.String)
     */
    protected java.util.Collection handleFindByOfficialSymbolInexact(java.lang.String officialSymbol)
        throws java.lang.Exception
    {
    	return this.getGeneDao().findByOfficialSymbolInexact(officialSymbol);
    }   
    
    /**
     * @see edu.columbia.gemma.genome.gene.GeneService#findAllQtlsByPhysicalMapLocation(edu.columbia.gemma.genome.PhysicalLocation)
     */
    protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(edu.columbia.gemma.genome.PhysicalLocation physicalMapLocation)
        throws java.lang.Exception
    {
        //@todo implement protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(edu.columbia.gemma.genome.PhysicalLocation physicalMapLocation)
        return null;
    }

}
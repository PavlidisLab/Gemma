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
package ubic.gemma.model.expression.designElement;

import java.util.Collection;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.BaseDao;

/**
 * @see CompositeSequence
 */
public interface CompositeSequenceDao extends BaseDao<CompositeSequence> {
    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public CompositeSequence find( CompositeSequence compositeSequence );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByBioSequenceName( java.lang.String name );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByGene( Gene gene );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<CompositeSequence> findByName( java.lang.String name );

    /**
     * 
     */
    public CompositeSequence findByName( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            java.lang.String name );

    /**
     * 
     */
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence );

    /**
     * <p>
     * Given a collection of composite sequences returns a map of the given composite sequences to a collection of genes
     * </p>
     */
    public java.util.Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> compositeSequences );

    /**
     * given a composite sequence returns a collection of genes
     */
    public java.util.Collection<Gene> getGenes( CompositeSequence compositeSequence );

    /**
     * Returns a map of CompositeSequences to BlatAssociations .
     */
    public java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * 
     */
    public Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences, Integer numResults );

    /**
     * 
     */
    public Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, java.lang.Integer numResults );

    /**
     * <p>
     * See ArrayDesignDao.getRawCompositeSequenceSummary.
     * </p>
     */
    public Collection<Object[]> getRawSummary( CompositeSequence compositeSequence, java.lang.Integer numResults );

    /**
     * 
     */
    public void thaw( java.util.Collection<CompositeSequence> compositeSequences );

    public CompositeSequence thaw( CompositeSequence compositeSequence );

}

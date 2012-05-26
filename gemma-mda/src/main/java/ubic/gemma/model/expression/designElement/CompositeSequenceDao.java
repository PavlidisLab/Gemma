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

import ubic.gemma.model.genome.Gene;
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
    public java.util.Collection<CompositeSequence> findByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByBioSequenceName( java.lang.String name );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByGene( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> findByName( java.lang.String name );

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
    public java.util.Map getGenes( java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * <p>
     * given a composite sequence returns a collection of genes
     * </p>
     */
    public java.util.Collection<Gene> getGenes( CompositeSequence compositeSequence );

    /**
     * <p>
     * Returns a map of CompositeSequences to PhysicalLocation to BlatAssociations at each location.
     * </p>
     */
    public java.util.Map getGenesWithSpecificity( java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * 
     */
    public java.util.Collection getRawSummary( java.util.Collection<CompositeSequence> compositeSequences,
            java.lang.Integer numResults );

    /**
     * 
     */
    public java.util.Collection getRawSummary( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            java.lang.Integer numResults );

    /**
     * <p>
     * See ArrayDesignDao.getRawCompositeSequenceSummary.
     * </p>
     */
    public java.util.Collection getRawSummary( CompositeSequence compositeSequence, java.lang.Integer numResults );

    /**
     * 
     */
    public void thaw( java.util.Collection<CompositeSequence> compositeSequences );

    public CompositeSequence thaw( CompositeSequence compositeSequence );

}

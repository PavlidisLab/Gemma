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
package ubic.gemma.persistence.service.expression.designElement;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.Slice;

import java.util.Collection;
import java.util.Map;

/**
 * @see CompositeSequence
 */
public interface CompositeSequenceDao extends FilteringVoEnabledDao<CompositeSequence, CompositeSequenceValueObject> {

    String OBJECT_ALIAS = "probe";

    Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence );

    Collection<CompositeSequence> findByBioSequenceName( String name );

    Collection<CompositeSequence> findByGene( Gene gene );

    Slice<CompositeSequence> findByGene( Gene gene, int start, int limit );

    Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign );

    Collection<CompositeSequence> findByName( String name );

    @Nullable
    CompositeSequence findByName( ArrayDesign arrayDesign, String name );

    /**
     * Given a collection of composite sequences returns a map of the given composite sequences to a collection of genes
     *
     * @param compositeSequences composite sequences
     * @return map
     */
    Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> compositeSequences );

    /**
     * given a composite sequence returns a collection of genes
     *
     * @param compositeSequence sequence
     * @param offset            offset
     * @param limit             limit
     * @return collection of genes
     */
    Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit );

    /**
     * @param compositeSequences sequences
     * @return a map of CompositeSequences to BlatAssociations.
     */
    Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults );

    void thaw( Collection<CompositeSequence> compositeSequences );

    void thaw( CompositeSequence compositeSequence );

}

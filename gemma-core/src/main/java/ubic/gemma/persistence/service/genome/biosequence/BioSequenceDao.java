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
package ubic.gemma.persistence.service.genome.biosequence;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;
import java.util.Map;

/**
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public interface BioSequenceDao extends BaseVoEnabledDao<BioSequence, BioSequenceValueObject> {

    @Nullable
    BioSequence findByAccession( DatabaseEntry accession );

    /**
     * <p>
     * Returns matching biosequences for the given genes in a Map (gene to biosequences). Genes which had no associated
     * sequences are not included in the result.
     * </p>
     *
     * @param genes genes
     * @return map to biosequences
     */
    Map<Gene, Collection<BioSequence>> findByGenes( Collection<Gene> genes );

    Collection<BioSequence> findByName( String name );

    Collection<Gene> getGenesByAccession( String search );

    /**
     * For a biosequence name, get the genes
     *
     * @param search name
     * @return genes
     */
    Collection<Gene> getGenesByName( String search );

    Collection<BioSequence> thaw( Collection<BioSequence> bioSequences );

    @Nullable
    BioSequence thaw( BioSequence bioSequence );

    @Nullable
    BioSequence findByCompositeSequence( CompositeSequence compositeSequence );
}

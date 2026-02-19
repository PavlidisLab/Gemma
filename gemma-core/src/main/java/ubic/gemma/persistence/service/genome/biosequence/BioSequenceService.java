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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AdminEditableBaseService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @author kelsey
 */
public interface BioSequenceService extends AdminEditableBaseService<BioSequence>, BaseVoEnabledService<BioSequence, BioSequenceValueObject> {

    @Nullable
    BioSequence findByAccession( DatabaseEntry accession );

    /**
     * @param genes genes
     * @return matching biosequences for the given genes in a Map (gene to a collection of biosequences). Genes which
     * had no associated sequences are not included in the result.
     */
    Map<Gene, Collection<BioSequence>> findByGenes( Collection<Gene> genes );

    /**
     * @param name name
     * @return all biosequences with names matching the given string. This matches only the name field, not the
     * accession.
     */
    Collection<BioSequence> findByName( String name );

    @Secured({ "GROUP_ADMIN" })
    Collection<BioSequence> findOrCreate( Collection<BioSequence> bioSequences );

    Collection<Gene> getGenesByAccession( String search );

    Collection<Gene> getGenesByName( String search );

    Collection<BioSequence> thaw( Collection<BioSequence> bioSequences );

    BioSequence thaw( BioSequence bs );

    @Nullable
    BioSequence findByCompositeSequence( CompositeSequence compositeSequence );
}

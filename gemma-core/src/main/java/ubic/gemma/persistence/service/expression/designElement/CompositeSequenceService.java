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

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.sequence.GeneMappingSummary;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public interface CompositeSequenceService
        extends BaseService<CompositeSequence>, FilteringVoEnabledService<CompositeSequence, CompositeSequenceValueObject> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    CompositeSequence find( CompositeSequence compositeSequence );

    @Override
    @Secured({ "GROUP_USER" })
    CompositeSequence findOrCreate( CompositeSequence compositeSequence );

    @Override
    @Secured({ "GROUP_USER" })
    CompositeSequence create( CompositeSequence compositeSequence );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> load( Collection<Long> ids );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( CompositeSequence compositeSequence );

    @Override
    @Secured({ "GROUP_USER" })
    void update( CompositeSequence compositeSequence );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByBioSequenceName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByGene( Gene gene );

    /**
     * Include gene mapping summary in the {@link CompositeSequenceValueObject}.
     */
    @Nullable
    @Transactional(readOnly = true)
    CompositeSequenceValueObject loadValueObjectWithGeneMappingSummary( CompositeSequence cs );

    Slice<CompositeSequenceValueObject> loadValueObjectsForGene( Gene gene, int start, int limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_READ" })
    CompositeSequence findByName( ArrayDesign arrayDesign, String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByNamesInArrayDesigns( Collection<String> compositeSequenceNames,
            Collection<ArrayDesign> arrayDesigns );

    /**
     * Given a Collection of composite sequences returns of map of a composite sequence to a collection of genes
     *
     * @param sequences sequences
     * @return map
     */
    Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> sequences );

    Collection<Gene> getGenes( CompositeSequence compositeSequence );

    Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit );

    /**
     * @param compositeSequences sequences
     * @return a map of CompositeSequences to collection of BioSequence2GeneProducts at each location.
     */
    Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults );

    Collection<GeneMappingSummary> getGeneMappingSummary( BioSequence biologicalCharacteristic,
            @Nullable CompositeSequenceValueObject cs );

    @CheckReturnValue
    Collection<CompositeSequence> thaw( Collection<CompositeSequence> compositeSequences );

    @CheckReturnValue
    CompositeSequence thaw( CompositeSequence compositeSequence );

}

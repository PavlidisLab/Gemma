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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;

/**
 * @author Gemma
 * @see BlatResult
 */
public interface BlatResultDao extends BaseVoEnabledDao<BlatResult, BlatResultValueObject> {

    @Nullable
    BlatResult thaw( BlatResult blatResult );

    Collection<BlatResult> thaw( Collection<BlatResult> blatResults );

    /**
     * Find BLAT results for the given sequence
     *
     * @param bioSequence BA
     * @return matching blat results
     */
    Collection<BlatResult> findByBioSequence( BioSequence bioSequence );

}

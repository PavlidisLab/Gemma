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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * Spring Service base class for <code>BlatResultService</code>, provides access to all services and entities referenced
 * by this service.
 *
 * @see BlatResultService
 */
@Component
public class BlatResultServiceImpl extends AbstractVoEnabledService<BlatResult, BlatResultValueObject>
        implements BlatResultService {

    private final BlatResultDao blatResultDao;

    @Autowired
    public BlatResultServiceImpl( BlatResultDao blatResultDao ) {
        super( blatResultDao );
        this.blatResultDao = blatResultDao;
    }

    /**
     * @see BlatResultService#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> findByBioSequence(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.blatResultDao.findByBioSequence( bioSequence );
    }

    @Override
    @Transactional(readOnly = true)
    public BlatResult thaw( BlatResult blatResult ) {
        return this.blatResultDao.thaw( blatResult );
    }

    @Override
    @Transactional(readOnly = true)
    public BlatResult thawOrFail( BlatResult toView ) {
        return requireNonNull( thaw( toView ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> thaw( Collection<BlatResult> blatResults ) {
        return this.blatResultDao.thaw( blatResults );
    }
}
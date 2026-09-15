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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;

/**
 * Spring Service base class for <code>BlatAssociationService</code>, provides access to all services and entities
 * referenced by this service.
 *
 * @see BlatAssociationService
 */
@Component
public class BlatAssociationServiceImpl extends AbstractService<BlatAssociation> implements BlatAssociationService {

    @Autowired
    private BlatAssociationReadService readService;

    @Autowired
    public BlatAssociationServiceImpl( BlatAssociationDao blatAssociationDao ) {
        super( blatAssociationDao );
    }

    // =====================================================================
    // Read methods -- delegate to BlatAssociationReadService.
    // BlatAssociationService declares no @Secured / @PostAuthorize / @PostFilter
    // on these read methods (the AdminEditableBaseImmutableService
    // @Secured("GROUP_ADMIN") annotations cover the write side only).
    // =====================================================================

    @Override
    public Collection<BlatAssociation> find( BioSequence bioSequence ) {
        return readService.find( bioSequence );
    }

    @Override
    public Collection<BlatAssociation> findAndThaw( BioSequence bioSequence ) {
        return readService.findAndThaw( bioSequence );
    }

    @Override
    public Collection<BlatAssociation> find( Gene gene ) {
        return readService.find( gene );
    }
}

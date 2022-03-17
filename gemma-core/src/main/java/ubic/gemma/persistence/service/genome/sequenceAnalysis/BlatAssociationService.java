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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface BlatAssociationService extends BaseService<BlatAssociation> {

    @Override
    @Secured({ "GROUP_USER" })
    BlatAssociation create( BlatAssociation blatAssociation );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( BlatAssociation blatAssociation );

    @Override
    @Secured({ "GROUP_USER" })
    void update( BlatAssociation blatAssociation );

    Collection<BlatAssociation> find( BioSequence bioSequence );

    Collection<BlatAssociation> find( Gene gene );
}

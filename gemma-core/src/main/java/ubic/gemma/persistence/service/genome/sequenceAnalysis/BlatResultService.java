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

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AdminEditableBaseService;

import java.util.Collection;

/**
 * @author paul
 */
public interface BlatResultService extends AdminEditableBaseService<BlatResult>, BaseVoEnabledService<BlatResult, BlatResultValueObject> {

    Collection<BlatResult> findByBioSequence( BioSequence bioSequence );

    BlatResult thaw( BlatResult blatResult );

    Collection<BlatResult> thaw( Collection<BlatResult> blatResults );
}

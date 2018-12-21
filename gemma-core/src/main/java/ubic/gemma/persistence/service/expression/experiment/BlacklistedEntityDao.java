/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

/**
 * @author paul
 */
public interface BlacklistedEntityDao extends BaseVoEnabledDao<BlacklistedEntity, BlacklistedValueObject> {

    boolean isBlacklisted( String accession );

    /**
     * @param accession
     * @return null if not blacklisted, or a BlackListedPlatform or BlackListedExperiment.
     */
    public BlacklistedEntity findByAccession( String accession );

}

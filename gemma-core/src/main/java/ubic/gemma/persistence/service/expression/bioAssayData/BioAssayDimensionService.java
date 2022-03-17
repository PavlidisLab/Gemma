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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

/**
 * @author Paul
 */
public interface BioAssayDimensionService
        extends BaseVoEnabledService<BioAssayDimension, BioAssayDimensionValueObject> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    BioAssayDimension findOrCreate( BioAssayDimension bioAssayDimension );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    BioAssayDimension create( BioAssayDimension bioAssayDimension );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( BioAssayDimension bioAssayDimension );

    @Deprecated
    BioAssayDimension thawLite( BioAssayDimension bioAssayDimension );

    @Override
    @Deprecated
    BioAssayDimension thaw( BioAssayDimension bioAssayDimension );
}

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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.persistence.service.BaseImmutableService;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.util.Thaws;

import java.util.Collection;

/**
 * @author Paul
 */
public interface BioAssayDimensionService
        extends BaseImmutableService<BioAssayDimension>, BaseVoEnabledService<BioAssayDimension, BioAssayDimensionValueObject> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    BioAssayDimension findOrCreate( BioAssayDimension bioAssayDimension );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    BioAssayDimension create( BioAssayDimension bioAssayDimension );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Collection<BioAssayDimension> findByBioAssayContainsAll( Collection<BioAssay> bioAssays );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( BioAssayDimension bioAssayDimension );

    /**
     * Lightly thaw a dimension.
     * <p>
     * Only the collection of bioassays is thawed.
     */
    BioAssayDimension thawLite( BioAssayDimension bioAssayDimension );

    /**
     * Fully thaw a dimension.
     * <p>
     * Each assay is thawed with {@link Thaws#thawBioAssay}.
     */
    BioAssayDimension thaw( BioAssayDimension bioAssayDimension );
}

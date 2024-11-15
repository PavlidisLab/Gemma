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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimension
 */
public interface BioAssayDimensionDao extends BaseVoEnabledDao<BioAssayDimension, BioAssayDimensionValueObject> {

    /**
     * Find all the dimensions that contains all the given assays.
     */
    Collection<BioAssayDimension> findByBioAssayContainsAll( Collection<BioAssay> bioAssays );

    void thawLite( BioAssayDimension bioAssayDimension );

    void thaw( BioAssayDimension bioAssayDimension );
}

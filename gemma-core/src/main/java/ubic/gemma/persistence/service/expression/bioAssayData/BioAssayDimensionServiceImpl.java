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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

/**
 * <p>
 * Spring Service base class for <code>BioAssayDimensionService</code>, provides access to all services and entities
 * referenced by this service.
 * </p>
 *
 * @see BioAssayDimensionService
 */
@Service
public class BioAssayDimensionServiceImpl
        extends AbstractVoEnabledService<BioAssayDimension, BioAssayDimensionValueObject>
        implements BioAssayDimensionService {

    private final BioAssayDimensionDao bioAssayDimensionDao;

    @Autowired
    public BioAssayDimensionServiceImpl( BioAssayDimensionDao bioAssayDimensionDao ) {
        super( bioAssayDimensionDao );
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension thawLite( BioAssayDimension bioAssayDimension ) {
        bioAssayDimension = loadOrFail( bioAssayDimension.getId() );
        this.bioAssayDimensionDao.thawLite( bioAssayDimension );
        return bioAssayDimension;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension thaw( BioAssayDimension bioAssayDimension ) {
        bioAssayDimension = loadOrFail( bioAssayDimension.getId() );
        this.bioAssayDimensionDao.thaw( bioAssayDimension );
        return bioAssayDimension;
    }

}
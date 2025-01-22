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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.util.Thaws;

import java.util.Collection;

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
    public Collection<BioAssayDimension> findByBioAssayContainsAll( Collection<BioAssay> bioAssays ) {
        return bioAssayDimensionDao.findByBioAssayContainsAll( bioAssays );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension thawLite( BioAssayDimension bioAssayDimension ) {
        bioAssayDimension = loadOrFail( bioAssayDimension.getId() );
        Hibernate.initialize( bioAssayDimension );
        Hibernate.initialize( bioAssayDimension.getBioAssays() );
        return bioAssayDimension;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension thaw( BioAssayDimension bioAssayDimension ) {
        bioAssayDimension = loadOrFail( bioAssayDimension.getId() );
        Hibernate.initialize( bioAssayDimension );
        bioAssayDimension.getBioAssays().forEach( Thaws::thawBioAssay );
        return bioAssayDimension;
    }

}
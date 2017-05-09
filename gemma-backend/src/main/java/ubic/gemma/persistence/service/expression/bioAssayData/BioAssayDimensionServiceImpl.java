/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

/**
 * @author pavlidis
 * @version $Id$
 * @see BioAssayDimensionService
 */
@Service
public class BioAssayDimensionServiceImpl extends BioAssayDimensionServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see
     * BioAssayDimensionService#thaw(ubic.gemma.model.expression.bioAssayData
     * .BioAssayDimension)
     */
    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension thaw( BioAssayDimension bioAssayDimension ) {
        return this.getBioAssayDimensionDao().thaw( bioAssayDimension );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension thawLite( BioAssayDimension bioAssayDimension ) {
        return this.getBioAssayDimensionDao().thawLite( bioAssayDimension );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * BioAssayDimensionServiceBase#handleCreate(ubic.gemma.model.expression
     * .bioAssayData.BioAssayDimension)
     */
    @Override
    protected BioAssayDimension handleCreate( BioAssayDimension bioAssayDimension ) {
        return this.getBioAssayDimensionDao().create( bioAssayDimension );
    }

    /**
     * @see BioAssayDimensionService#findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    protected BioAssayDimension handleFindOrCreate( BioAssayDimension bioAssayDimension ) {
        return this.getBioAssayDimensionDao().findOrCreate( bioAssayDimension );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BioAssayDimensionServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected BioAssayDimension handleLoad( Long id ) {
        return this.getBioAssayDimensionDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * BioAssayDimensionServiceBase#handleRemove(ubic.gemma.model.expression
     * .bioAssayData.BioAssayDimension)
     */
    @Override
    protected void handleRemove( BioAssayDimension bioAssayDimension ) {
        this.getBioAssayDimensionDao().remove( bioAssayDimension );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * BioAssayDimensionServiceBase#handleUpdate(ubic.gemma.model.expression
     * .bioAssayData.BioAssayDimension)
     */
    @Override
    protected void handleUpdate( BioAssayDimension bioAssayDimension ) {
        this.getBioAssayDimensionDao().update( bioAssayDimension );
    }

}
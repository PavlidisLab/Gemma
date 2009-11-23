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
package ubic.gemma.model.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.bioAssayData.DesignElementDimensionService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDimensionService
 */
public abstract class DesignElementDimensionServiceBase implements
        ubic.gemma.model.expression.bioAssayData.DesignElementDimensionService {

    @Autowired
    private ubic.gemma.model.expression.bioAssayData.DesignElementDimensionDao designElementDimensionDao;

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDimensionService#findOrCreate(ubic.gemma.model.expression.bioAssayData.DesignElementDimension)
     */
    public ubic.gemma.model.expression.bioAssayData.DesignElementDimension findOrCreate(
            final ubic.gemma.model.expression.bioAssayData.DesignElementDimension designElementDimension ) {
        try {
            return this.handleFindOrCreate( designElementDimension );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDimensionServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.DesignElementDimensionService.findOrCreate(ubic.gemma.model.expression.bioAssayData.DesignElementDimension designElementDimension)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>designElementDimension</code>'s DAO.
     */
    public void setDesignElementDimensionDao(
            ubic.gemma.model.expression.bioAssayData.DesignElementDimensionDao designElementDimensionDao ) {
        this.designElementDimensionDao = designElementDimensionDao;
    }

    /**
     * Gets the reference to <code>designElementDimension</code>'s DAO.
     */
    protected ubic.gemma.model.expression.bioAssayData.DesignElementDimensionDao getDesignElementDimensionDao() {
        return this.designElementDimensionDao;
    }

    /**
     * Performs the core logic for
     * {@link #findOrCreate(ubic.gemma.model.expression.bioAssayData.DesignElementDimension)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.DesignElementDimension handleFindOrCreate(
            ubic.gemma.model.expression.bioAssayData.DesignElementDimension designElementDimension )
            throws java.lang.Exception;

}
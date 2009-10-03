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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService
 */
public abstract class BioAssayDataVectorServiceBase implements
        ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService {

    private ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao bioAssayDataVectorDao;

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService#findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    public ubic.gemma.model.expression.bioAssayData.BioAssayDataVector findOrCreate(
            final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        try {
            return this.handleFindOrCreate( bioAssayDataVector );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService.findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService#remove(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    public void remove( final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        try {
            this.handleRemove( bioAssayDataVector );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService.remove(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>bioAssayDataVector</code>'s DAO.
     */
    public void setBioAssayDataVectorDao(
            ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao bioAssayDataVectorDao ) {
        this.bioAssayDataVectorDao = bioAssayDataVectorDao;
    }

    /**
     * Gets the reference to <code>bioAssayDataVector</code>'s DAO.
     */
    protected ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao getBioAssayDataVectorDao() {
        return this.bioAssayDataVectorDao;
    }

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.BioAssayDataVector handleFindOrCreate(
            ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector )
            throws java.lang.Exception;

}
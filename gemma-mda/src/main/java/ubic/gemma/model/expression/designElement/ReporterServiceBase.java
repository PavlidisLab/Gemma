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
package ubic.gemma.model.expression.designElement;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.designElement.ReporterService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.designElement.ReporterService
 */
public abstract class ReporterServiceBase implements ubic.gemma.model.expression.designElement.ReporterService {

    private ubic.gemma.model.expression.designElement.ReporterDao reporterDao;

    /**
     * @see ubic.gemma.model.expression.designElement.ReporterService#create(java.util.Collection)
     */
    public java.util.Collection create( final java.util.Collection reporters ) {
        try {
            return this.handleCreate( reporters );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.ReporterServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.ReporterService.create(java.util.Collection reporters)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.ReporterService#create(ubic.gemma.model.expression.designElement.Reporter)
     */
    public ubic.gemma.model.expression.designElement.Reporter create(
            final ubic.gemma.model.expression.designElement.Reporter reporter ) {
        try {
            return this.handleCreate( reporter );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.ReporterServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.ReporterService.create(ubic.gemma.model.expression.designElement.Reporter reporter)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.ReporterService#find(ubic.gemma.model.expression.designElement.Reporter)
     */
    public ubic.gemma.model.expression.designElement.Reporter find(
            final ubic.gemma.model.expression.designElement.Reporter reporter ) {
        try {
            return this.handleFind( reporter );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.ReporterServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.ReporterService.find(ubic.gemma.model.expression.designElement.Reporter reporter)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.ReporterService#findOrCreate(ubic.gemma.model.expression.designElement.Reporter)
     */
    public ubic.gemma.model.expression.designElement.Reporter findOrCreate(
            final ubic.gemma.model.expression.designElement.Reporter reporter ) {
        try {
            return this.handleFindOrCreate( reporter );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.ReporterServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.ReporterService.findOrCreate(ubic.gemma.model.expression.designElement.Reporter reporter)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.ReporterService#remove(ubic.gemma.model.expression.designElement.Reporter)
     */
    public void remove( final ubic.gemma.model.expression.designElement.Reporter reporter ) {
        try {
            this.handleRemove( reporter );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.ReporterServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.ReporterService.remove(ubic.gemma.model.expression.designElement.Reporter reporter)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>reporter</code>'s DAO.
     */
    public void setReporterDao( ubic.gemma.model.expression.designElement.ReporterDao reporterDao ) {
        this.reporterDao = reporterDao;
    }

    /**
     * Gets the reference to <code>reporter</code>'s DAO.
     */
    protected ubic.gemma.model.expression.designElement.ReporterDao getReporterDao() {
        return this.reporterDao;
    }

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection handleCreate( java.util.Collection reporters ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.designElement.Reporter)}
     */
    protected abstract ubic.gemma.model.expression.designElement.Reporter handleCreate(
            ubic.gemma.model.expression.designElement.Reporter reporter ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.designElement.Reporter)}
     */
    protected abstract ubic.gemma.model.expression.designElement.Reporter handleFind(
            ubic.gemma.model.expression.designElement.Reporter reporter ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.designElement.Reporter)}
     */
    protected abstract ubic.gemma.model.expression.designElement.Reporter handleFindOrCreate(
            ubic.gemma.model.expression.designElement.Reporter reporter ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.designElement.Reporter)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.designElement.Reporter reporter )
            throws java.lang.Exception;

}
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
package ubic.gemma.model.expression.biomaterial;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.biomaterial.TreatmentService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.biomaterial.TreatmentService
 */
public abstract class TreatmentServiceBase extends ubic.gemma.model.common.AuditableServiceImpl implements
        ubic.gemma.model.expression.biomaterial.TreatmentService {

    private ubic.gemma.model.expression.biomaterial.TreatmentDao treatmentDao;

    /**
     * @see ubic.gemma.model.expression.biomaterial.TreatmentService#getTreatments()
     */
    public java.util.Collection getTreatments() {
        try {
            return this.handleGetTreatments();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.TreatmentServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.TreatmentService.getTreatments()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.TreatmentService#saveTreatment(ubic.gemma.model.expression.biomaterial.Treatment)
     */
    public ubic.gemma.model.expression.biomaterial.Treatment saveTreatment(
            final ubic.gemma.model.expression.biomaterial.Treatment treatment ) {
        try {
            return this.handleSaveTreatment( treatment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.TreatmentServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.TreatmentService.saveTreatment(ubic.gemma.model.expression.biomaterial.Treatment treatment)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>treatment</code>'s DAO.
     */
    public void setTreatmentDao( ubic.gemma.model.expression.biomaterial.TreatmentDao treatmentDao ) {
        this.treatmentDao = treatmentDao;
    }

    /**
     * Gets the reference to <code>treatment</code>'s DAO.
     */
    protected ubic.gemma.model.expression.biomaterial.TreatmentDao getTreatmentDao() {
        return this.treatmentDao;
    }

    /**
     * Performs the core logic for {@link #getTreatments()}
     */
    protected abstract java.util.Collection handleGetTreatments() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #saveTreatment(ubic.gemma.model.expression.biomaterial.Treatment)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.Treatment handleSaveTreatment(
            ubic.gemma.model.expression.biomaterial.Treatment treatment ) throws java.lang.Exception;

}
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
package ubic.gemma.model.common.description;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.description.FileFormatService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.FileFormatService
 */
public abstract class FileFormatServiceBase implements ubic.gemma.model.common.description.FileFormatService {

    private ubic.gemma.model.common.description.FileFormatDao fileFormatDao;

    /**
     * @see ubic.gemma.model.common.description.FileFormatService#findByIdentifier(java.lang.String)
     */
    public ubic.gemma.model.common.description.FileFormat findByIdentifier( final java.lang.String identifier ) {
        try {
            return this.handleFindByIdentifier( identifier );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.FileFormatServiceException(
                    "Error performing 'ubic.gemma.model.common.description.FileFormatService.findByIdentifier(java.lang.String identifier)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>fileFormat</code>'s DAO.
     */
    public void setFileFormatDao( ubic.gemma.model.common.description.FileFormatDao fileFormatDao ) {
        this.fileFormatDao = fileFormatDao;
    }

    /**
     * Gets the reference to <code>fileFormat</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.FileFormatDao getFileFormatDao() {
        return this.fileFormatDao;
    }

    /**
     * Performs the core logic for {@link #findByIdentifier(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.description.FileFormat handleFindByIdentifier(
            java.lang.String identifier ) throws java.lang.Exception;

}
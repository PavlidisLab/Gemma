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

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.description.LocalFileService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.LocalFileService
 */
public abstract class LocalFileServiceBase implements ubic.gemma.model.common.description.LocalFileService {

    @Autowired
    private ubic.gemma.model.common.description.LocalFileDao localFileDao;

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#copyFile(ubic.gemma.model.common.description.LocalFile,
     *      ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile copyFile(
            final ubic.gemma.model.common.description.LocalFile sourceFile,
            final ubic.gemma.model.common.description.LocalFile targetFile ) {
        try {
            return this.handleCopyFile( sourceFile, targetFile );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.copyFile(ubic.gemma.model.common.description.LocalFile sourceFile, ubic.gemma.model.common.description.LocalFile targetFile)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#deleteFile(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public void deleteFile( final ubic.gemma.model.common.description.LocalFile localFile ) {
        try {
            this.handleDeleteFile( localFile );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.deleteFile(ubic.gemma.model.common.description.LocalFile localFile)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#find(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile find(
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        try {
            return this.handleFind( localFile );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.find(ubic.gemma.model.common.description.LocalFile localFile)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#findByPath(java.lang.String)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile findByPath( final java.lang.String path ) {
        try {
            return this.handleFindByPath( path );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.findByPath(java.lang.String path)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#findOrCreate(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile findOrCreate(
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        try {
            return this.handleFindOrCreate( localFile );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.findOrCreate(ubic.gemma.model.common.description.LocalFile localFile)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#save(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile save(
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        try {
            return this.handleSave( localFile );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.save(ubic.gemma.model.common.description.LocalFile localFile)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>localFile</code>'s DAO.
     */
    public void setLocalFileDao( ubic.gemma.model.common.description.LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileService#update(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public void update( final ubic.gemma.model.common.description.LocalFile localFile ) {
        try {
            this.handleUpdate( localFile );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.LocalFileServiceException(
                    "Error performing 'ubic.gemma.model.common.description.LocalFileService.update(ubic.gemma.model.common.description.LocalFile localFile)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>localFile</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.LocalFileDao getLocalFileDao() {
        return this.localFileDao;
    }

    /**
     * Performs the core logic for
     * {@link #copyFile(ubic.gemma.model.common.description.LocalFile, ubic.gemma.model.common.description.LocalFile)}
     */
    protected abstract ubic.gemma.model.common.description.LocalFile handleCopyFile(
            ubic.gemma.model.common.description.LocalFile sourceFile,
            ubic.gemma.model.common.description.LocalFile targetFile ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #deleteFile(ubic.gemma.model.common.description.LocalFile)}
     */
    protected abstract void handleDeleteFile( ubic.gemma.model.common.description.LocalFile localFile )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.description.LocalFile)}
     */
    protected abstract ubic.gemma.model.common.description.LocalFile handleFind(
            ubic.gemma.model.common.description.LocalFile localFile ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByPath(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.description.LocalFile handleFindByPath( java.lang.String path )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.description.LocalFile)}
     */
    protected abstract ubic.gemma.model.common.description.LocalFile handleFindOrCreate(
            ubic.gemma.model.common.description.LocalFile localFile ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #save(ubic.gemma.model.common.description.LocalFile)}
     */
    protected abstract ubic.gemma.model.common.description.LocalFile handleSave(
            ubic.gemma.model.common.description.LocalFile localFile ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.description.LocalFile)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.description.LocalFile localFile )
            throws java.lang.Exception;

}
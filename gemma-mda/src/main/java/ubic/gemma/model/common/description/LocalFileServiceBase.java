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

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Spring Service base class for <code>LocalFileService</code>, provides access to all services and entities referenced
 * by this service.
 * </p>
 * 
 * @see LocalFileService
 */
public abstract class LocalFileServiceBase implements LocalFileService {

    @Autowired
    private LocalFileDao localFileDao;

    /**
     * @see LocalFileService#copyFile(LocalFile, LocalFile)
     */
    @Override
    @Transactional
    public LocalFile copyFile( final LocalFile sourceFile, final LocalFile targetFile ) throws IOException {
        return this.handleCopyFile( sourceFile, targetFile );

    }

    /**
     * @see LocalFileService#deleteFile(LocalFile)
     */
    @Override
    @Transactional
    public void deleteFile( final LocalFile localFile ) throws IOException {
        this.handleDeleteFile( localFile );

    }

    /**
     * @see LocalFileService#find(LocalFile)
     */
    @Override
    @Transactional(readOnly = true)
    public LocalFile find( final LocalFile localFile ) {
        return this.handleFind( localFile );

    }

    /**
     * @see LocalFileService#findByPath(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public LocalFile findByPath( final java.lang.String path ) {
        return this.handleFindByPath( path );

    }

    /**
     * @see LocalFileService#findOrCreate(LocalFile)
     */
    @Override
    @Transactional
    public LocalFile findOrCreate( final LocalFile localFile ) {
        return this.handleFindOrCreate( localFile );

    }

    /**
     * @see LocalFileService#save(LocalFile)
     */
    @Override
    @Transactional
    public LocalFile save( final LocalFile localFile ) {
        return this.handleSave( localFile );

    }

    /**
     * Sets the reference to <code>localFile</code>'s DAO.
     */
    public void setLocalFileDao( LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

    /**
     * @see LocalFileService#update(LocalFile)
     */
    @Override
    @Transactional
    public void update( final LocalFile localFile ) {
        this.handleUpdate( localFile );
    }

    /**
     * Gets the reference to <code>localFile</code>'s DAO.
     */
    protected LocalFileDao getLocalFileDao() {
        return this.localFileDao;
    }

    /**
     * Performs the core logic for {@link #copyFile(LocalFile, LocalFile)}
     */
    protected abstract LocalFile handleCopyFile( LocalFile sourceFile, LocalFile targetFile ) throws IOException;

    /**
     * Performs the core logic for {@link #deleteFile(LocalFile)}
     */
    protected abstract void handleDeleteFile( LocalFile localFile ) throws IOException;

    /**
     * Performs the core logic for {@link #find(LocalFile)}
     */
    protected abstract LocalFile handleFind( LocalFile localFile );

    /**
     * Performs the core logic for {@link #findByPath(java.lang.String)}
     */
    protected abstract LocalFile handleFindByPath( java.lang.String path );

    /**
     * Performs the core logic for {@link #findOrCreate(LocalFile)}
     */
    protected abstract LocalFile handleFindOrCreate( LocalFile localFile );

    /**
     * Performs the core logic for {@link #save(LocalFile)}
     */
    protected abstract LocalFile handleSave( LocalFile localFile );

    /**
     * Performs the core logic for {@link #update(LocalFile)}
     */
    protected abstract void handleUpdate( LocalFile localFile );

}
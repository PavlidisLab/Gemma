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
package ubic.gemma.persistence.service.common.description;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.service.AbstractService;

import java.io.*;
import java.net.MalformedURLException;

/**
 * @author pavlidis
 */
@Service
public class LocalFileServiceImpl extends AbstractService<LocalFile> implements LocalFileService {

    private final LocalFileDao localFileDao;

    @Autowired
    public LocalFileServiceImpl( LocalFileDao mainDao ) {
        super( mainDao );
        this.localFileDao = mainDao;
    }

    /**
     * @see LocalFileService#copyFile(LocalFile, LocalFile)
     */
    @Override
    public LocalFile copyFile( LocalFile sourceFile, LocalFile targetFile ) throws IOException {
        File src = sourceFile.asFile();
        if ( src == null )
            throw new IOException( "Could not convert LocalFile into java.io.File" );

        File dst = targetFile.asFile();
        if ( dst == null )
            throw new IOException( "Could not convert LocalFile into java.io.File" );
        try (InputStream in = new FileInputStream( src ); OutputStream out = new FileOutputStream( dst )) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            long size = 0;
            while ( ( len = in.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
                size += len;
            }
            in.close();
            out.close();

            targetFile.setSize( size );

            this.localFileDao.create( targetFile );
            return targetFile;
        }
    }

    /**
     * @see LocalFileService#deleteFile(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public void deleteFile( LocalFile localFile ) throws IOException {

        if ( localFile == null )
            return;
        File file = localFile.asFile();
        if ( file == null )
            throw new IOException( "Could not convert LocalFile into java.io.File" );

        boolean success = false;
        if ( file.exists() ) {
            success = file.delete();
        }

        if ( file.exists() || !success ) {
            throw new IOException( "Cannot remove file" );
        }
        this.localFileDao.remove( localFile );
    }

    @Override
    public LocalFile findByPath( String path ) {
        File f = new File( path );
        LocalFile seek = LocalFile.Factory.newInstance();
        try {
            seek.setLocalURL( f.toURI().toURL() );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        return this.localFileDao.find( seek );
    }

    @Override
    public LocalFile save( LocalFile localFile ) {
        return this.localFileDao.create( localFile );
    }

}
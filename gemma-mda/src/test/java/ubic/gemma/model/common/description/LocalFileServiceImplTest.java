/*
 * The Gemma project
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
package ubic.gemma.model.common.description;

import static org.easymock.EasyMock.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 * @author pavlidis
 * @version $Id$
 */
@RunWith(EasyMockRunner.class)
public class LocalFileServiceImplTest {

    @Mock
    private LocalFileDao mockLFDao = null;

    @TestSubject
    private LocalFileServiceImpl svc = new LocalFileServiceImpl();

    /*
     * Test method for 'ubic.gemma.model.common.description.LocalFileServiceImpl.handleDeleteFile(LocalFile)'
     */
    @Test
    public void testHandleDeleteFile() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        try (OutputStream out = new FileOutputStream( f );) {
            out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        }
        lf.setLocalURL( f.toURI().toURL() );

        mockLFDao.remove( lf );
        expectLastCall().once();

        replay( mockLFDao );

        svc.deleteFile( lf );
        verify( mockLFDao );

        assertTrue( !f.exists() );
    }

    /*
     * Test method for 'ubic.gemma.model.common.description.LocalFileServiceImpl.handleCopyFile(LocalFile, LocalFile)'
     */
    @Test
    public void testHandleCopyFile() throws Exception {
        LocalFile src = LocalFile.Factory.newInstance();
        File srcf = File.createTempFile( "copyMe", ".txt" );
        src.setLocalURL( srcf.toURI().toURL() );

        try (OutputStream out = new FileOutputStream( srcf );) {
            out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );

        }

        LocalFile dst = LocalFile.Factory.newInstance();
        File dstf = File.createTempFile( "copyToMe", ".txt" );
        dst.setLocalURL( dstf.toURI().toURL() );

        mockLFDao.create( dst );
        expectLastCall().andReturn( dst );

        replay( mockLFDao );

        svc.copyFile( src, dst );
        verify( mockLFDao );
        assertTrue( dstf.canRead() );

        dstf.delete();
        srcf.delete();
    }

    @Test
    public void testFindByPath() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        try (OutputStream out = new FileOutputStream( f );) {
            out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        }
        lf.setLocalURL( f.toURI().toURL() );

        LocalFile seek = LocalFile.Factory.newInstance();
        try {
            seek.setLocalURL( f.toURI().toURL() );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }

        mockLFDao.find( lf );
        expectLastCall().andReturn( seek ).once();
        replay( mockLFDao );

        svc.findByPath( f.getAbsolutePath() );

        verify( mockLFDao );
        f.delete();
    }

    @Test
    public void testFind() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        try (OutputStream out = new FileOutputStream( f );) {
            out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        }
        lf.setLocalURL( f.toURI().toURL() );

        mockLFDao.find( lf );
        expectLastCall().andReturn( lf );
        replay( mockLFDao );

        svc.find( lf );
        verify( mockLFDao );
        f.delete();
    }

    @Test
    public void testFindOrCreate() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        try (OutputStream out = new FileOutputStream( f );) {
            out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        }
        lf.setLocalURL( f.toURI().toURL() );

        mockLFDao.findOrCreate( lf );
        expectLastCall().andReturn( lf );
        replay( mockLFDao );

        svc.findOrCreate( lf );
        verify( mockLFDao );
        f.delete();
    }

}

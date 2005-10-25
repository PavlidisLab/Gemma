/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common.description;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LocalFileServiceImplTest extends TestCase {

    private LocalFileDao mockLFDao = null;
    private LocalFileServiceImpl svc = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        svc = new LocalFileServiceImpl();
        mockLFDao = createMock( LocalFileDao.class );
        svc.setLocalFileDao( mockLFDao );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.common.description.LocalFileServiceImpl.handleDeleteFile(LocalFile)'
     */
    public void testHandleDeleteFile() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        OutputStream out = new FileOutputStream( f );
        out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        out.close();
        lf.setLocalURI( f.toURI().toString() );

        mockLFDao.remove( lf );
        expectLastCall().once();

        replay( mockLFDao );

        svc.deleteFile( lf );
        verify( mockLFDao );

        assertTrue( !f.exists() );
    }

    /*
     * Test method for 'edu.columbia.gemma.common.description.LocalFileServiceImpl.handleCopyFile(LocalFile, LocalFile)'
     */
    public void testHandleCopyFile() throws Exception {
        LocalFile src = LocalFile.Factory.newInstance();
        File srcf = File.createTempFile( "copyMe", ".txt" );
        src.setLocalURI( srcf.toURI().toString() );

        OutputStream out = new FileOutputStream( srcf );
        out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        out.close();

        LocalFile dst = LocalFile.Factory.newInstance();
        File dstf = File.createTempFile( "copyToMe", ".txt" );
        dst.setLocalURI( dstf.toURI().toString() );

        mockLFDao.create( dst );
        expectLastCall().andReturn( dst );

        replay( mockLFDao );

        svc.copyFile( src, dst );
        verify( mockLFDao );
        assertTrue( dstf.canRead() );

        dstf.delete();
        srcf.delete();
    }

    public void testFindByPath() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        OutputStream out = new FileOutputStream( f );
        out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        out.close();
        lf.setLocalURI( f.toURI().toString() );

        mockLFDao.find( lf );
        expectLastCall().andReturn( lf );
        replay( mockLFDao );

        svc.findByPath( f.getAbsolutePath() );

        verify( mockLFDao );
        f.delete();
    }

    public void testFind() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        OutputStream out = new FileOutputStream( f );
        out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        out.close();
        lf.setLocalURI( f.toURI().toString() );

        mockLFDao.find( lf );
        expectLastCall().andReturn( lf );
        replay( mockLFDao );

        svc.find( lf );
        verify( mockLFDao );
        f.delete();
    }

    public void testFindOrCreate() throws Exception {
        LocalFile lf = LocalFile.Factory.newInstance();
        File f = File.createTempFile( "deleteMe", ".txt" );
        OutputStream out = new FileOutputStream( f );
        out.write( "fiibly".getBytes(), 0, "fiibly".getBytes().length );
        out.close();
        lf.setLocalURI( f.toURI().toString() );

        mockLFDao.findOrCreate( lf );
        expectLastCall().andReturn( lf );
        replay( mockLFDao );

        svc.findOrCreate( lf );
        verify( mockLFDao );
        f.delete();
    }

}

/*
 * The gemma-web project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.web.util.upload;

import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import ubic.gemma.core.config.Settings;
import ubic.gemma.web.controller.util.upload.FileUploadUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class FileUploadUtilTest {

    private static final Path UPLOAD_DIR = Paths.get( Settings.getDownloadPath() ).resolve( "userUploads" );

    private Path copiedFile;

    @After
    public void tearDown() throws IOException {
        if ( copiedFile != null ) {
            Files.delete( copiedFile );
        }
    }

    @Test
    public void testGetUploadedFile() {
        assertEquals( UPLOAD_DIR.resolve( "foo.txt" ), FileUploadUtil.getUploadedFile( "bar/foo.txt", UPLOAD_DIR ) );
        assertEquals( UPLOAD_DIR.resolve( "foo.txt" ), FileUploadUtil.getUploadedFile( "../foo.txt", UPLOAD_DIR ) );
        assertThrows( IllegalArgumentException.class, () -> FileUploadUtil.getUploadedFile( "..", UPLOAD_DIR ) );
        assertThrows( IllegalArgumentException.class, () -> FileUploadUtil.getUploadedFile( ".", UPLOAD_DIR ) );
    }

    @Test
    public void testCopyUploadedFileStreamClosed() throws IOException {

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod( "POST" );

        String text_contents = "test";
        String key = "file1";
        String filename = "test_upload.txt";

        MockMultipartFile sourceFile = new MockMultipartFile( key, filename, "text/plain", text_contents.getBytes() );

        long expectedSize = sourceFile.getSize();
        request.setContent( text_contents.getBytes() );
        request.addFile( sourceFile );

        copiedFile = FileUploadUtil.copyUploadedFile( request, key, UPLOAD_DIR );
        assertTrue( Files.exists( copiedFile ) );
        assertTrue( copiedFile.getFileName().toString().endsWith( "__test_upload.txt" ) );
        assertEquals( expectedSize, Files.size( copiedFile ) );

        assertEquals( copiedFile, FileUploadUtil.getUploadedFile( copiedFile.toString(), UPLOAD_DIR ) );
        assertEquals( copiedFile, FileUploadUtil.getUploadedFile( copiedFile.getFileName().toString(), UPLOAD_DIR ) );
        assertEquals( copiedFile, FileUploadUtil.getUploadedFile( Paths.get( "foo", "bar" ).resolve( copiedFile.getFileName() ).toString(), UPLOAD_DIR ) );
        assertEquals( copiedFile, FileUploadUtil.getUploadedFile( Paths.get( "foo", ".." ).resolve( copiedFile.getFileName() ).toString(), UPLOAD_DIR ) );
    }

    @Test
    public void testCopyUploadFileWhenFileContainsDirectory() throws IOException {

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod( "POST" );

        String text_contents = "test";
        String key = "file1";
        String filename = "dir/../test_upload.txt";

        MockMultipartFile sourceFile = new MockMultipartFile( key, filename, "text/plain", text_contents.getBytes() );

        long expectedSize = sourceFile.getSize();
        request.setContent( text_contents.getBytes() );
        request.addFile( sourceFile );

        copiedFile = FileUploadUtil.copyUploadedFile( request, key, UPLOAD_DIR );
        assertTrue( Files.exists( copiedFile ) );
        assertTrue( copiedFile.getFileName().toString().endsWith( "__test_upload.txt" ) );
        assertEquals( expectedSize, Files.size( copiedFile ) );
    }

}

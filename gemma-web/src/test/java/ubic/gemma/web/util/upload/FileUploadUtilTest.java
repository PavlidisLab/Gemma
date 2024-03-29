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

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileUploadUtilTest {

    private File copiedFile;

    @After
    public void tearDown() {
        if ( copiedFile != null ) {
            assertTrue( copiedFile.delete() );
        }
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

        copiedFile = FileUploadUtil.copyUploadedFile( request, key );
        assertTrue( copiedFile.exists() );
        assertTrue( copiedFile.getName().endsWith( "__test_upload.txt" ) );
        assertEquals( expectedSize, copiedFile.length() );
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

        copiedFile = FileUploadUtil.copyUploadedFile( request, key );
        assertTrue( copiedFile.exists() );
        assertTrue( copiedFile.getName().endsWith( "__test_upload.txt" ) );
        assertEquals( expectedSize, copiedFile.length() );
    }

}

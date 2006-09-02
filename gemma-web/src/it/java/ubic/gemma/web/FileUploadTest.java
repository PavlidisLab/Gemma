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
package ubic.gemma.web;

import java.io.File;

import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class FileUploadTest extends BaseWebTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();

    }

    public final void testFileUpload() throws Exception {
        this.beginAt( "/uploadFile.jsp" );
        assertFormPresent();
        this.setTextField( "name", "just a test" );
        this.setTextField( "file", ConfigUtils.getString( "gemma.home" )
                + "/gemma-web/src/test/resources/data/pubmed-test.xml".replace( '/', File.separatorChar ) );
        this.submit();
    }

    public final void testNonExistentFileUpload() throws Exception {
        this.beginAt( "/uploadFile.jsp" );
        assertFormPresent();
        this.setTextField( "name", "just a test" );
        this.setTextField( "file", ConfigUtils.getString( "gemma.home" )
                + "/gemma-web/src/test/resources/data/does not exist".replace( '/', File.separatorChar ) );
        this.submit();
    }

    public final void testNoFile() throws Exception {
        this.beginAt( "/uploadFile.jsp" );
        assertFormPresent();
        this.setTextField( "name", "just a test" );
        this.setTextField( "file", "" );
        this.submit();
    }

}

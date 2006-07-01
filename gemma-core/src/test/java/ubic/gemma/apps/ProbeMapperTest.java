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
package ubic.gemma.apps;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperTest extends TestCase {

    File tempFile;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        tempFile = File.createTempFile( "cli", "txt" );

    }

    protected void tearDown() throws Exception {
        tempFile.delete();
    }

    public final void testMainBadPort() {
        ProbeMapper.main( new String[] { "-P", "b" } );
    }

    public final void testMainBadOptionB() {
        fail( "Not yet implemented" ); // TODO
    }

    public final void testMainBadOptionC() {
        fail( "Not yet implemented" ); // TODO
    }

    public final void testMainBadOptionD() {
        fail( "Not yet implemented" ); // TODO
    }

    public void testBlatHandling() throws Exception {
        ProbeMapper.main( new String[] { "-u", "pavlidis", "-p", "toast", "-o", tempFile.getAbsolutePath() } );
    }

    public void testGbHandling() throws Exception {
        ProbeMapper.main( new String[] { "-u", "pavlidis", "-p", "toast", "-o", tempFile.getAbsolutePath() } );
    }

    public void testSingleGb() throws Exception {
        ProbeMapper.main( new String[] { "-u", "pavlidis", "-p", "toast", "-o", tempFile.getAbsolutePath() } );
    }
}

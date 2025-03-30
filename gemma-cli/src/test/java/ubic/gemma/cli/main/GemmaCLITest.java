/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.cli.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author paul
 */
public class GemmaCLITest {

    @Test
    public void testMaskPwd() {
        String result = GemmaCLI.getOptStringForLogging( new String[] { "-u", "administrator", "-p", "password", "--array", "GPL14187" } );
        assertEquals( "-u administrator -p XXXXXX --array GPL14187", result );

        result = GemmaCLI.getOptStringForLogging( new String[] { "-u", "administrator", "--array", "GPL14187", "-p", "password" } );
        assertEquals( "-u administrator --array GPL14187 -p XXXXXX", result );

        result = GemmaCLI.getOptStringForLogging( new String[] { "-u", "administrator", "--password", "password", "--array", "GPL14187" } );
        assertEquals( "-u administrator --password XXXXXX --array GPL14187", result );

    }

}

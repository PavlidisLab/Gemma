/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.core.job;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;

/**
 * @author paul
 *
 */
public class TaskUtilsTest {

    /**
     * Test method for {@link ubic.gemma.core.job.TaskUtils#generateTaskId()}.
     */
    @Test
    public final void testGenerateTaskId() {
        Collection<String> seen = new HashSet<>();

        for ( int i = 0; i < 1000; i++ ) {
            String id = TaskUtils.generateTaskId();
            assertTrue( StringUtils.isNotBlank( id ) );
            assertTrue( !seen.contains( id ) );
            seen.add( id );
        }
    }

}

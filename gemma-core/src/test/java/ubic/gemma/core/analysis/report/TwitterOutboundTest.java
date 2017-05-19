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
package ubic.gemma.core.analysis.report;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.testing.BaseSpringContextTest;

/**
 * @author sshao
 * @version $Id$
 */
public class TwitterOutboundTest extends BaseSpringContextTest {
    @Autowired
    private TwitterOutbound twitterOutbound;

    @Before
    public void setup() {
        super.getTestPersistentExpressionExperiment();
    }

    @Test
    public void testTweetLength() {
        String status = twitterOutbound.generateDailyFeed();
        log.info( status );
        assertNotNull( status );
        assertTrue( ( status.length() <= 140 ) );
    }
}
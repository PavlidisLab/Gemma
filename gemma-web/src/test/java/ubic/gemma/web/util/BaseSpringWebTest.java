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
package ubic.gemma.web.util;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.util.logging.Logger;

/**
 * Class to extend for tests of controllers et al. that need a spring context. Provides convenience methods for dealing
 * with mock requests and responses. Also provides a safe port to send email on for testing (for example, using
 * dumbster)
 * <p>
 * This is meant for integration tests, if you want to perform unit tests, consider using {@link WebAppConfiguration}
 * and {@link ContextConfiguration} with a static inner class annotated with {@link org.springframework.context.annotation.Configuration}.
 * See {@link ubic.gemma.web.services.rest.SearchWebServiceTest} for a complete example.
 * @author pavlidis
 * @deprecated favour the simpler {@link BaseWebIntegrationTest} for new tests
 */
@Deprecated
@ActiveProfiles("web")
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath*:WEB-INF/gemma-servlet.xml" })
public abstract class BaseSpringWebTest extends BaseSpringContextTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mvc;

    /**
     * @see MockMvc#perform(RequestBuilder)
     */
    protected final ResultActions perform( RequestBuilder requestBuilder ) throws Exception {
        if ( mvc == null ) {
            mvc = MockMvcBuilders.webAppContextSetup( applicationContext ).build();
        }
        return mvc.perform( requestBuilder );
    }
}

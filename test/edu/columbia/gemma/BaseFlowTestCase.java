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
package edu.columbia.gemma;

import org.springframework.webflow.test.AbstractFlowExecutionTests;

import edu.columbia.gemma.util.SpringContextUtil;

public class BaseFlowTestCase extends AbstractFlowExecutionTests {

    protected String flowId() {
        return null;
    }

    protected String[] getConfigLocations() {
        return SpringContextUtil.getConfigLocations( true );
    }

}

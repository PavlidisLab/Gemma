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
package edu.columbia.gemma.web.flow;

import java.util.HashMap;
import java.util.Map;

import org.springframework.webflow.Event;
import org.springframework.webflow.ViewDescriptor;
import org.springframework.webflow.test.AbstractFlowExecutionTests;
import org.springframework.webflow.test.MockRequestContext;

import edu.columbia.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class FileUploadActionTest extends AbstractFlowExecutionTests {

    public void testProcessFile() throws Exception {
        this.startFlow();
        assertCurrentStateEquals( "selectFile" );
        MockRequestContext context = new MockRequestContext();
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put( "file", new FileUpload() );
        context.getFlowScope().setAttribute( "file", new FileUpload() );

        FileUploadAction fua = new FileUploadAction();
        Event result = fua.execute( context );
        assertEquals( "success", result.getId() );

        ViewDescriptor view = signalEvent( result );
        this.assertModelAttributeNotNull( "readFile", view );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.webflow.test.AbstractFlowExecutionTests#flowId()
     */
    @Override
    protected String flowId() {
        return "fileupload";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath:WEB-INF/action-servlet.xml",
                "classpath:WEB-INF/applicationContext-hibernate.xml",
                "classpath:WEB-INF/applicationContext-security.xml",
                "classpath:WEB-INF/applicationContext-validation.xml", "classpath:WEB-INF/localTestDataSource.xml" };
    }

}

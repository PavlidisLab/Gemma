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
package ubic.gemma.web.controller.expression.arrayDesign;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author keshav
 */
public class ArrayDesignControllerTest extends BaseSpringWebTest {

    private MockHttpServletRequest request;

    @Autowired
    private ArrayDesignController arrayDesignController;

    @Test
    public void testShowAllArrayDesigns() throws Exception {
        request = this.newPost( Settings.getRootContext() + "/arrayDesign/showAllArrayDesigns.html" );

        ModelAndView mav = arrayDesignController.showAllArrayDesigns( request, ( HttpServletResponse ) null );
        Collection<Object> c = mav.getModel().values();
        assertNotNull( c );
        assertEquals( "arrayDesigns", mav.getViewName() );
    }
}

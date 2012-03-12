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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.testing.BaseSpringWebTest;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignControllerTest extends BaseSpringWebTest {

    private MockHttpServletRequest request;

    @Autowired
    private ArrayDesignController arrayDesignController;

    /**
     * @throws Exception
     */
    @Test
    public void testShowAllArrayDesigns() throws Exception {
        request = newPost( "Gemma/arrayDesign/showAllArrayDesigns.html" );

        ModelAndView mav = arrayDesignController.showAllArrayDesigns( request, ( HttpServletResponse ) null );
        Collection<Object> c = mav.getModel().values();
        assertNotNull( c );
        assertEquals( "arrayDesigns", mav.getViewName() );
    }
}

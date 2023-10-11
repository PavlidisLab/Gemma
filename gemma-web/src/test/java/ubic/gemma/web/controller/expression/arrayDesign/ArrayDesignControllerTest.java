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
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author keshav
 */
public class ArrayDesignControllerTest extends BaseSpringWebTest {

    @Autowired
    private ArrayDesignController arrayDesignController;

    @Test
    public void testShowAllArrayDesigns() {
        ModelAndView mav = arrayDesignController.showAllArrayDesigns();
        Collection<Object> c = mav.getModel().values();
        assertNotNull( c );
        assertEquals( "arrayDesigns", mav.getViewName() );
    }
}

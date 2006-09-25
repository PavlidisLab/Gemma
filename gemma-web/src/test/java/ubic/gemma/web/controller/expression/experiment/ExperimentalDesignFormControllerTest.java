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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class ExperimentalDesignFormControllerTest extends BaseTransactionalSpringContextTest {
    // TODO finish implementing this test.
    private Log log = LogFactory.getLog( this.getClass() );

    private ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
    private MockHttpServletRequest request = null;

    /**
     * setUp
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( RandomStringUtils.randomAlphanumeric( 10 ) );
        ed.setDescription( "An experimental design created from the ExperimentalDesignFormControllerTest." );

        Collection<ExperimentalFactor> efCol = new HashSet();

        ExperimentalDesignService eds = ( ExperimentalDesignService ) getBean( "experimentalDesignService" );
        ed = eds.findOrCreate( ed );

        for ( int i = 0; i < TEST_ELEMENT_COLLECTION_SIZE; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setName( "Experimental Factor Bot" );
            ef.setDescription( "An experimental factor created from the ExperimentalDesignFormControllerTest" );
            efCol.add( ef );
            ef.setExperimentalDesign( ed );
        }
        ed.setExperimentalFactors( efCol );

        eds.update( ed );

        assert ed.getId() != null;
    }
//
//    /**
//     * @throws Exception
//     */
//    public void testSave() throws Exception {
//        log.debug( "testing save" );
//        // FIXME - implement this.
//    }

    /**
     * @throws Exception
     */
    public void testEdit() throws Exception {
        log.debug( "testing edit" );

        ExperimentalDesignFormController c = ( ExperimentalDesignFormController ) getBean( "experimentalDesignFormController" );

        request = new MockHttpServletRequest( "GET", "/experimentalDesign/editExperimentalDesign.html" );
        request.addParameter( "name", ed.getName() );

        ModelAndView mav = c.handleRequest( request, ( new MockHttpServletResponse() ) );

        assertEquals( "experimentalDesign.edit", mav.getViewName() );
    }

    // /**
    // * @throws Exception
    // */
    // public void testDelete() throws Exception {
    // log.debug( "testing delete" ); // FIXME - implement this.
    //    }

}

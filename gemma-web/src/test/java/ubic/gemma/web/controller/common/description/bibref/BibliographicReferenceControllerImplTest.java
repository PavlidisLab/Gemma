/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.web.controller.common.description.bibref;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.testing.BaseSpringWebTest;

/**
 * @author Paul
 * @version $Id$
 */
public class BibliographicReferenceControllerImplTest extends BaseSpringWebTest {

    @Autowired
    BibliographicReferenceController bibliographicReferenceController;

    /**
     * Test method for
     * {@link ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceControllerImpl#showAllForExperiments(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * .
     */
    @Test
    public void testShowAllForExperiments() {
        ModelAndView mv = bibliographicReferenceController.showAllForExperiments(
                this.newGet( "/bibRef/showAllEeBibRefs.html" ), ( HttpServletResponse ) null );
        Map<CitationValueObject, Collection<ExpressionExperimentValueObject>> citationToEEs = ( Map<CitationValueObject, Collection<ExpressionExperimentValueObject>> ) mv
                .getModel().get( "citationToEEs" );
        assertNotNull( citationToEEs );

    }
}

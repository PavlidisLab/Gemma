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
package ubic.gemma.web.controller.genome.gene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.CandidateGeneListService;
import ubic.gemma.model.genome.gene.GeneService;

/** 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="geneFinderController"  
 * @spring.property name="formView" value="geneFinder"
 * @spring.property name="successView" value="geneFinder"
 * @spring.property name="geneService" ref="geneService"
 */
public class GeneFinderController extends SimpleFormController {
    private GeneService geneService;

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String searchString = request.getParameter( "searchString" );

        // search by inexact symbol
        Collection<Gene> genes = this.getGeneService().findByOfficialSymbolInexact( searchString );
        ModelAndView mav = new ModelAndView("genes");
        mav.addObject( "genes", genes );
        return mav;

    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
}

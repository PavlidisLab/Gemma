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
package ubic.gemma.web.controller.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BackgroundProcessingFormController;
import ubic.gemma.web.controller.BaseFormController;

/** 
 * @author joseph
 * @version $Id$
 * @spring.bean id="mainMenuController"  
 * @spring.property name="formView" value="mainMenu"
 * @spring.property name="successView" value="mainMenu"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioAssayService" ref="bioAssayService" 
 * @spring.property name="arrayDesignService" ref="arrayDesignService" 
 */
public class MainMenuController extends BaseFormController {
    private static Log log = LogFactory.getLog( MainMenuController.class.getName() );
    
    private ExpressionExperimentService expressionExperimentService;
    private BioAssayService bioAssayService;
    private ArrayDesignService arrayDesignService;
    


    /**
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @return the bioAssayService
     */
    public BioAssayService getBioAssayService() {
        return bioAssayService;
    }

    /**
     * @param bioAssayService the bioAssayService to set
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    @SuppressWarnings({ "unused", "unchecked" })
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
      
        ModelAndView mav = new ModelAndView(getFormView());

        return mav;
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
   
    
    /* (non-Javadoc)
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors ) throws Exception {
        ModelAndView mav = new ModelAndView(getFormView());

        Map<String,Long> stats = new HashMap<String,Long>();
        long bioAssayCount = bioAssayService.countAll();
        stats.put( "bioAssayCount", bioAssayCount );
        long arrayDesignCount = arrayDesignService.countAll();
        stats.put( "arrayDesignCount", arrayDesignCount );
        Map<String,Long> taxonCount = expressionExperimentService.getPerTaxonCount();

        mav.addObject( "stats", stats );
        mav.addObject( "taxonCount",taxonCount );
        return mav;        
    }






}

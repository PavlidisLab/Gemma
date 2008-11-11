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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;

/**
 * Responsible for display of the Gemma home page.
 * 
 * @author joseph
 * @version $Id$
 * @spring.bean id="mainMenuController"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="whatsNewService" ref="whatsNewService"
 */
public class MainMenuController extends AbstractController {
    private static Log log = LogFactory.getLog( MainMenuController.class.getName() );

    private static final long MIN_TO_REPORT = 200;

    private ExpressionExperimentService expressionExperimentService;
    private BioAssayService bioAssayService;
    private ArrayDesignService arrayDesignService;
    private WhatsNewService whatsNewService;

    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        ModelAndView mav = new ModelAndView();
        Map<String, Long> stats = new HashMap<String, Long>();

        long bioAssayCount = bioAssayService.countAll();
        stats.put( "bioAssayCount", bioAssayCount );

        long arrayDesignCount = arrayDesignService.countAll();
        stats.put( "arrayDesignCount", arrayDesignCount );
        mav.addObject( "stats", stats );

        getTaxonEECounts( mav );

        WhatsNew wn = getWhatsNewReport();
        if ( wn != null && wn.getDate() != null ) {
            mav.addObject( "whatsNew", wn );
        }

        // I like to time things.
        timer.stop();
        if ( timer.getTime() > MIN_TO_REPORT ) {
            log.info( "Home page processing: " + timer.getTime() + "ms (only times over " + MIN_TO_REPORT + " given)" );
        }
        return mav;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bioAssayService the bioAssayService to set
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setWhatsNewService( WhatsNewService whatsNewService ) {
        this.whatsNewService = whatsNewService;
    }

    /**
     * @param mav
     */
    private void getTaxonEECounts( ModelAndView mav ) {
        Map<Taxon, Long> taxonCount = expressionExperimentService.getPerTaxonCount();
        long expressionExperimentCount = 0;
        Collection<Long> values = taxonCount.values();
        for ( Long count : values ) {
            expressionExperimentCount += count;
        }
        mav.addObject( "taxonCount", taxonCount );
        mav.addObject( "expressionExperimentCount", expressionExperimentCount );
    }

    /**
     * @return
     */
    private WhatsNew getWhatsNewReport() {
        WhatsNew wn = whatsNewService.retrieveReport();
        return wn;
    }

}

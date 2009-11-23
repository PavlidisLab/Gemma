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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.monitor.Monitored;

/**
 * Responsible for display of the Gemma home page.
 * 
 * @author joseph
 * @version $Id$
 */
@Controller
public class HomePageController {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private WhatsNewService whatsNewService;

    private ModelAndView mav = new ModelAndView();

    @RequestMapping("/mainMenu.html")
    @Monitored(minTimeToReport = 10)
    public ModelAndView showHomePage() throws Exception {

        /*
         * Note that this needs to be fast. The queries involved almost always result in a O(1) cache hit. Don't add new
         * functionality here without considering that.
         */
        updateCounts();
        return mav;
    }

    /**
     * FIXME : I wish we could use @Scheduled to update this kind of thing periodically.
     * 
     * @param
     */
    public void updateCounts() {
        Map<String, Long> stats = new HashMap<String, Long>();

        long bioAssayCount = bioAssayService.countAll();
        long arrayDesignCount = arrayDesignService.countAll();
        Map<Taxon, Long> eesPerTaxon = expressionExperimentService.getPerTaxonCount();
        long expressionExperimentCount = 0;
        Collection<Long> values = eesPerTaxon.values();
        for ( Long count : values ) {
            expressionExperimentCount += count;
        }
        WhatsNew wn = whatsNewService.retrieveReport();

        stats.put( "bioAssayCount", bioAssayCount );
        stats.put( "arrayDesignCount", arrayDesignCount );

        mav.addObject( "stats", stats );
        mav.addObject( "taxonCount", eesPerTaxon );
        mav.addObject( "expressionExperimentCount", expressionExperimentCount );
        if ( wn != null && wn.getDate() != null ) {
            mav.addObject( "whatsNew", wn );
        }

    }

}

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.report.WhatsNew;
import ubic.gemma.core.analysis.report.WhatsNewService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.WebConstants;

import java.util.*;

/**
 * Responsible for display of the Gemma home page.
 *
 * @author joseph
 * @deprecated
 */
@Controller
@Deprecated
public class GemmaClassicHomePageController {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private WhatsNewService whatsNewService;

    private ModelAndView mav = new ModelAndView();

    private Taxon otherTaxa;

    public GemmaClassicHomePageController() {
        otherTaxa = Taxon.Factory.newInstance();
        otherTaxa.setId( -1L );
        otherTaxa.setCommonName( "Other" );
        otherTaxa.setScientificName( "Other" );
        otherTaxa.setIsGenesUsable( false );
    }

    @RequestMapping(value = WebConstants.CLASSIC_HOME_PAGE, method = RequestMethod.GET)
    public ModelAndView showHomePage() {

        /*
         * Note that this needs to be fast. The queries involved almost always result in a O(1) cache hit. Don't add new
         * functionality here without considering that.
         */
        updateCounts();
        return mav;
    }

    public void updateCounts() {
        Map<String, Long> stats = new HashMap<String, Long>();

        long bioAssayCount = bioAssayService.countAll();
        long arrayDesignCount = arrayDesignService.countAll();

        /*
         * Sort taxa by name.
         */
        TreeMap<Taxon, Long> eesPerTaxon = new TreeMap<Taxon, Long>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                return o1.getScientificName().compareTo( o2.getScientificName() );
            }
        } );
        eesPerTaxon.putAll( expressionExperimentService.getPerTaxonCount() );

        long expressionExperimentCount = 0;
        long otherTaxaEECount = 0;
        for ( Iterator<Taxon> it = eesPerTaxon.keySet().iterator(); it.hasNext(); ) {
            Taxon t = it.next();
            Long c = eesPerTaxon.get( t );
            // TODO problem with this is we want to make a link to them.
            if ( c < 10 ) {
                // temporary, hide 'uncommon' taxa from this table. See bug 2052
                otherTaxaEECount += c;
                it.remove();
            }
            expressionExperimentCount += c;
        }
        if ( otherTaxaEECount > 0 ) {
            // eesPerTaxon.put( otherTaxa, otherTaxaEECount );
        }

        WhatsNew wn = whatsNewService.getLatestWeeklyReport();

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

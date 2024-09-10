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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.WebConstants;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.Map.Entry;

/**
 * Responsible for display of the Gemma 2.0 home page. Based on original HomePageController.java
 *
 * @author thea
 *
 */
@Controller
public class HomePageController {

    private static final TaxonComparator TAXON_COMPARATOR = new TaxonComparator();

    private static final class TaxonComparator implements Comparator<Map.Entry<Taxon, Long>> {
        @Override
        public int compare( Map.Entry<Taxon, Long> e1, Map.Entry<Taxon, Long> e2 ) {
            Long e1value = e1.getValue();
            Long e2value = e2.getValue();

            int cf = e1value.compareTo( e2value );
            if ( cf == 0 ) {
                try {
                    String e1commonName = e1.getKey().getCommonName();
                    String e2commonName = e2.getKey().getCommonName();

                    cf = e1commonName.compareTo( e2commonName );
                } catch ( Exception e ) {
                    cf = 1;
                }
            }
            return cf;
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BuildInfo buildInfo;

    @RequestMapping("/")
    public RedirectView redirectToHomePage( HttpServletRequest request ) {
        String uri = ServletUriComponentsBuilder.fromRequest( request )
                .scheme( null ).host( null ).port( -1 )
                .replacePath( WebConstants.HOME_PAGE )
                .build()
                .toUriString();
        return new RedirectView( uri );
    }

    @RequestMapping(WebConstants.HOME_PAGE)
    public ModelAndView showHomePage() {
        ModelAndView mav = new ModelAndView( "home" );
        mav.addObject( "buildInfo", buildInfo );
        /*
         * Note that this needs to be fast. The queries involved almost always result in a O(1) cache hit. Don't add new
         * functionality here without considering that.
         */
        addCountsForTaxonPieChart( mav );
        return mav;
    }

    /**
     * For the show-off graph that shows number of data sets per taxon.
     */
    public void addCountsForTaxonPieChart( ModelAndView mav ) {

        Map<Taxon, Long> unsortedEEsPerTaxon = expressionExperimentService.getPerTaxonCount();

        /*
         * Sort taxa by count.
         */
        TreeSet<Map.Entry<Taxon, Long>> eesPerTaxonValueSorted = new TreeSet<>( TAXON_COMPARATOR );

        eesPerTaxonValueSorted.addAll( unsortedEEsPerTaxon.entrySet() );

        long expressionExperimentCount = expressionExperimentService.countAll();

        double groupBelow = 0.1; // if a taxon has less then this percent of total count, group into 'other'
        String googleData = encodeDataForGoogle( eesPerTaxonValueSorted.descendingSet(), expressionExperimentCount,
                groupBelow );

        List<String> googleLabelsColls = new ArrayList<>();
        boolean grouped = false;
        List<String> others = new ArrayList<>();

        for ( Entry<Taxon, Long> entry : eesPerTaxonValueSorted.descendingSet() ) {
            String tname = entry.getKey().getCommonName();
            if ( StringUtils.isBlank( tname ) ) tname = entry.getKey().getScientificName();

            if ( entry.getValue() == 0 ) continue;

            if ( groupIntoOther( entry.getValue(), expressionExperimentCount, groupBelow ) ) {
                grouped = true;
                others.add( tname );
            } else {
                googleLabelsColls.add( tname );
            }
        }

        if ( grouped ) {
            googleLabelsColls.add( StringUtils.abbreviate( StringUtils.join( others, ", " ), 50 ) );
        }

        String googleLabels = StringUtils.join( googleLabelsColls, '|' );

        mav.addObject( "googleData", googleData );
        mav.addObject( "googleLabels", googleLabels );

    }

    private final static char[] simpleEncoding = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private String encodeDataForGoogle( Set<Entry<Taxon, Long>> eesPerTaxonValueSorted, long maxValue, double groupBelow ) {
        // This function scales the submitted values so that
        // maxVal becomes the highest value.
        StringBuilder chartData = new StringBuilder( "s:" );
        int otherSum = 0;
        for ( Entry<Taxon, Long> currentValue : eesPerTaxonValueSorted ) {
            if ( groupIntoOther( currentValue.getValue(), maxValue, groupBelow ) ) {
                otherSum += currentValue.getValue();
            } else {
                chartData.append( simpleEncoding[Math.round( ( float ) ( ( simpleEncoding.length - 1 ) * currentValue.getValue() ) / maxValue )] );
            }
        }
        if ( otherSum != 0 ) {
            chartData.append( simpleEncoding[Math.round( ( float ) ( ( simpleEncoding.length - 1 ) * otherSum ) / maxValue )] );
        }
        return chartData.toString();
    }

    private boolean groupIntoOther( long value, long maxValue, double threshold ) {
        double a = ( ( double ) value / ( double ) maxValue );
        return threshold > a;
    }
}

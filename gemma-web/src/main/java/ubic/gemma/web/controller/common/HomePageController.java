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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.web.controller.WebConstants;

/**
 * Responsible for display of the Gemma 2.0 home page. Based on original HomePageController.java
 * 
 * @author thea
 * @version $Id$
 */
@Controller
public class HomePageController {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private ModelAndView mav = new ModelAndView();

    private Taxon otherTaxa;

    public HomePageController() {
        otherTaxa = Taxon.Factory.newInstance();
        otherTaxa.setId( -1L );
        otherTaxa.setCommonName( "Other" );
        otherTaxa.setScientificName( "Other" );
        otherTaxa.setAbbreviation( "Other" );
        otherTaxa.setIsGenesUsable( false );
        otherTaxa.setIsSpecies( false );
    }

    @RequestMapping(WebConstants.HOME_PAGE)
    public ModelAndView showHomePage() {

        /*
         * Note that this needs to be fast. The queries involved almost always result in a O(1) cache hit. Don't add new
         * functionality here without considering that.
         */
        // updateCounts();
        getCountsForTaxonPieChart();
        return mav;
    }

    public void getCountsForTaxonPieChart() {

        Map<Taxon, Long> unsortedEEsPerTaxon = expressionExperimentService.getPerTaxonCount();

        /*
         * Sort taxa by count.
         */
        TreeSet<Map.Entry<Taxon, Long>> eesPerTaxonValueSorted = new TreeSet<Map.Entry<Taxon, Long>>(
                new Comparator<Map.Entry<Taxon, Long>>() {
                    @Override
                    public int compare( Map.Entry<Taxon, Long> e1, Map.Entry<Taxon, Long> e2 ) {
                        int cf = e1.getValue().compareTo( e2.getValue() );
                        if ( cf == 0 ) {
                            try {
                                cf = e1.getKey().getCommonName().compareTo( e2.getKey().getCommonName() );
                            } catch ( Exception e ) {
                                cf = 1;
                            }
                        }
                        return cf;
                    }
                } );
        eesPerTaxonValueSorted.addAll( unsortedEEsPerTaxon.entrySet() );

        long expressionExperimentCount = expressionExperimentService.countAll();

        double groupBelow = 0.1; // if a taxa has less then this percent of total count, group into 'other'
        String googleData = encodeDataForGoogle( eesPerTaxonValueSorted.descendingSet(), expressionExperimentCount,
                groupBelow );
        List<String> googleLabelsColls = new ArrayList<String>();
        boolean grouped = false;
        List<String> others = new ArrayList<String>();
        for ( Entry<Taxon, Long> entry : eesPerTaxonValueSorted.descendingSet() ) {
            if ( groupIntoOther( entry.getValue(), expressionExperimentCount, groupBelow ) ) {
                grouped = true;
                others.add( entry.getKey().getCommonName() );
            } else {
                googleLabelsColls.add( entry.getKey().getCommonName() );
            }
        }
        if ( grouped ) {
            googleLabelsColls.add( StringUtils.join( others, ", " ) );
        }
        String googleLabels = StringUtils.join( googleLabelsColls, '|' );

        mav.addObject( "googleData", googleData );
        mav.addObject( "googleLabels", googleLabels );

    }

    private String encodeDataForGoogle( Set<Entry<Taxon, Long>> eesPerTaxonValueSorted, long maxValue, double groupBelow ) {
        char[] simpleEncoding = new String( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" )
                .toCharArray();

        // This function scales the submitted values so that
        // maxVal becomes the highest value.
        String chartData = "s:";
        int otherSum = 0;
        for ( Entry<Taxon, Long> currentValue : eesPerTaxonValueSorted ) {
            if ( groupIntoOther( currentValue.getValue(), maxValue, groupBelow ) ) {
                otherSum += currentValue.getValue();
            } else {
                chartData += simpleEncoding[Math.round( ( simpleEncoding.length - 1 ) * currentValue.getValue()
                        / maxValue )];
            }
        }
        if ( otherSum != 0 ) {
            chartData += simpleEncoding[Math.round( ( simpleEncoding.length - 1 ) * otherSum / maxValue )];
        }
        return chartData;
    }

    private boolean groupIntoOther( long value, long maxValue, double threshold ) {
        double a = ( new Double( value ) / new Double( maxValue ) );
        return threshold > a;
    }
}

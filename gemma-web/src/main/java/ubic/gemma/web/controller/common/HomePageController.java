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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.web.controller.WebConstants;

/**
 * Responsible for display of the Gemma 2.0 home page.
 * 
 * Based on original HomePageController.java
 * 
 * @author thea
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
        Map<Taxon, Long> unsortedEEsPerTaxon = expressionExperimentService.getPerTaxonCount();
        
        /*
         * Sort taxa by name.
         */
        TreeMap<Taxon, Long> eesPerTaxon = new TreeMap<Taxon, Long>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                return o1.getScientificName().compareTo( o2.getScientificName() );
            }
        } );
        /*
         * Sort taxa by count.
         */
        TreeSet<Map.Entry<Taxon, Long>> eesPerTaxonValueSorted = new TreeSet<Map.Entry<Taxon, Long>>( new Comparator<Map.Entry<Taxon, Long>>() {  
             public int compare(Map.Entry<Taxon, Long> e1, Map.Entry<Taxon, Long> e2) {  
                int cf = e1.getValue().compareTo(e2.getValue());  
                if (cf == 0) {  
                   cf = ((Taxon)e1.getKey()).getCommonName().compareTo(((Taxon)e2.getKey()).getCommonName());  
                }  
                return cf;  
             }  
          }  );
        eesPerTaxonValueSorted.addAll( unsortedEEsPerTaxon.entrySet() );
        
        eesPerTaxon.putAll( unsortedEEsPerTaxon );
        
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

        WhatsNew wn = whatsNewService.retrieveReport();
                
        if(wn == null){
        	Calendar c = Calendar.getInstance();
        	Date date = c.getTime();
        	date = DateUtils.addWeeks( date, -1 );
        	wn = whatsNewService.getReport( date );
        }
        if(wn != null){
        	 //Get count for new assays
        	int newAssayCount = wn.getNewAssayCount();
                
        	int newExpressionExperimentCount = (wn.getNewExpressionExperiments()!=null)?
        	                                            wn.getNewExpressionExperiments().size():0;
        	int updatedExpressionExperimentCount = (wn.getUpdatedExpressionExperiments() != null)?
        	                                            wn.getUpdatedExpressionExperiments().size():0;
           
            /*Store counts for new and updated experiments by taxonId*/
            /* new front page design doesn't need these structures
        	Map<Taxon, Long> newEEsPerTaxon = wn.getNewEECountPerTaxon();
            Map<Taxon, Long> updatedEEsPerTaxon = wn.getUpdatedEECountPerTaxon();
            mav.addObject( "newPerTaxonCount", newEEsPerTaxon );
            mav.addObject( "updatedPerTaxonCount", updatedEEsPerTaxon );
            */
        	                                            
        	//Get count for new and updated array designs
            int newArrayCount = (wn.getNewArrayDesigns()!=null)? wn.getNewArrayDesigns().size():0;
            int updatedArrayCount = (wn.getUpdatedArrayDesigns()!=null)? wn.getUpdatedArrayDesigns().size():0;
        	
        	boolean drawNewColumn = (newExpressionExperimentCount > 0 || newArrayCount > 0 || newAssayCount > 0)? true:false;
        	boolean drawUpdatedColumn = (updatedExpressionExperimentCount > 0 || updatedArrayCount > 0 )? true:false;
        	String date = (wn.getDate() != null)?DateFormat.getDateInstance(DateFormat.LONG).format(wn.getDate()): "";
        	date = date.replace( '-', ' ' );

        	mav.addObject( "updateDate",  date);
        	mav.addObject( "drawNewColumn", drawNewColumn);
        	mav.addObject( "drawUpdatedColumn", drawUpdatedColumn);
        	if(newAssayCount != 0) stats.put( "newBioAssayCount", new Long(newAssayCount) );
        	if(newArrayCount != 0) stats.put( "newArrayDesignCount", new Long(newArrayCount));
        	if(updatedArrayCount != 0) stats.put( "updatedArrayDesignCount", new Long(updatedArrayCount));
            if(newExpressionExperimentCount != 0) mav.addObject( "newExpressionExperimentCount",  newExpressionExperimentCount);
            if(updatedExpressionExperimentCount != 0) mav.addObject( "updatedExpressionExperimentCount", updatedExpressionExperimentCount);

        }
               
        stats.put( "bioAssayCount", bioAssayCount );
        stats.put( "arrayDesignCount", arrayDesignCount );
        
        mav.addObject( "stats", stats );
        mav.addObject( "taxonCount", eesPerTaxon );
        mav.addObject( "expressionExperimentCount", expressionExperimentCount );
        if ( wn != null && wn.getDate() != null ) {
            mav.addObject( "whatsNew", wn );
        }
        
        
        double groupBelow = 0.1; // if a taxa has less then this percent of total count, group into 'other'
        String googleData = encodeDataForGoogle(eesPerTaxonValueSorted.descendingSet(), expressionExperimentCount, groupBelow);
        List<String> googleLabelsColls = new ArrayList<String>();
        boolean grouped = false;
        List<String> others = new ArrayList<String>();
        for(Entry<Taxon, Long> entry : eesPerTaxonValueSorted.descendingSet()){
            if(groupIntoOther(entry.getValue(), expressionExperimentCount, groupBelow)){
                grouped = true;
                others.add( entry.getKey().getCommonName() );
            }else{
                googleLabelsColls.add( entry.getKey().getCommonName() );    
            }
        }
        if(grouped){
            googleLabelsColls.add( StringUtils.join( others, ", " ) );
        }
        String googleLabels = StringUtils.join( googleLabelsColls, '|' );

        mav.addObject( "googleData", googleData );
        mav.addObject( "googleLabels", googleLabels );
        
    }
    private String encodeDataForGoogle(Set<Entry<Taxon, Long>> eesPerTaxonValueSorted, long maxValue, double groupBelow){
        char[] simpleEncoding = new String("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789").toCharArray();
        
        // This function scales the submitted values so that
        // maxVal becomes the highest value.
          String chartData = "s:";
          int otherSum = 0;
          for (Entry<Taxon, Long> currentValue : eesPerTaxonValueSorted) {
              if(groupIntoOther(currentValue.getValue(), maxValue, groupBelow)){
                  otherSum += currentValue.getValue(); 
              }else{
                  chartData += simpleEncoding[Math.round((simpleEncoding.length-1) * 
                              currentValue.getValue() / maxValue)];
              }
          }
          if(otherSum != 0){
              chartData += simpleEncoding[Math.round((simpleEncoding.length-1) * 
                      otherSum / maxValue)];
          }
          return chartData;
    }
    private boolean groupIntoOther(long value, long maxValue, double threshold){
        double a = (new Double(value) / new Double(maxValue));
        return threshold > a;
    }
}

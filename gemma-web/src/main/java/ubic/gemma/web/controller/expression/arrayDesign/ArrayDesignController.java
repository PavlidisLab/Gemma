/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.controller.expression.arrayDesign;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;

/**
 * Note: do not use parametrized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 *
 * @author paul
 */
@RequestMapping("/arrays")
public interface ArrayDesignController {

    String addAlternateName( Long arrayDesignId, String alternateName );

    /**
     * AJAX call for remote paging store security isn't incorporated in db query, so paging needs to occur at higher
     * level? Is there security for ArrayDesigns? ids can be null
     */
    JsonReaderResponse<ArrayDesignValueObject> browse( ListBatchCommand batch, Long[] ids, boolean showMerged,
            boolean showOrphans );

    /**
     * Delete an arrayDesign.
     */
    @RequestMapping("/deleteArrayDesign.html")
    ModelAndView delete( HttpServletRequest request, HttpServletResponse response );

    @RequestMapping("/downloadAnnotationFile.html")
    ModelAndView downloadAnnotationFile( HttpServletRequest request, HttpServletResponse response );

    /**
     * Show array designs that match search criteria.
     */
    @RequestMapping("/filterArrayDesigns.html")
    ModelAndView filter( HttpServletRequest request, HttpServletResponse response );

    /**
     * Build summary report for an array design
     */
    @RequestMapping("/generateArrayDesignSummary.html")
    ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response );

    Collection<ArrayDesignValueObject> getArrayDesigns( Long[] arrayDesignIds, boolean showMergees,
            boolean showOrphans );

    /**
     * Exposed for AJAX calls.
     */
    Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator ed );

    Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign );

    /**
     * @return the HTML to display.
     */
    Map<String, String> getReportHtml( EntityDelegator ed );

    String getSummaryForArrayDesign( Long id );

    Collection<ArrayDesignValueObject> loadArrayDesignsForShowAll( Long[] arrayDesignIds );

    ArrayDesignValueObject loadArrayDesignsSummary();

    String remove( EntityDelegator ed );

    /**
     * Show all array designs, or according to a list of IDs passed in.
     */
    @RequestMapping("/showAllArrayDesigns.html")
    ModelAndView showAllArrayDesigns( HttpServletRequest request, HttpServletResponse response );

    @RequestMapping({ "/showArrayDesign.html", "/" })
    ModelAndView showArrayDesign( HttpServletRequest request, HttpServletResponse response );

    /**
     * Show (some of) the probes from an array.
     */
    @RequestMapping("/showCompositeSequenceSummary.html")
    ModelAndView showCompositeSequences( HttpServletRequest request );

    /**
     * shows a list of BioAssays for an expression experiment subset
     *
     * @return ModelAndView
     */
    @RequestMapping("/showExpressionExperiments.html")
    ModelAndView showExpressionExperiments( HttpServletRequest request );

    String updateReport( EntityDelegator ed );

    String updateReportById( Long id );

    ArrayDesignValueObject getDetails( Long id );

}
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

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @author paul
 * @version $Id$
 */
@RequestMapping("/arrays")
public interface ArrayDesignController {

    public abstract String addAlternateName( Long arrayDesignId, String alternateName );

    /**
     * AJAX call for remote paging store security isn't incorporated in db query, so paging needs to occur at higher
     * level? Is there security for ArrayDesigns? ids can be null
     * 
     * @param batch
     * @return
     */
    public abstract JsonReaderResponse<ArrayDesignValueObject> browse( ListBatchCommand batch, Long[] ids,
            boolean showMerged, boolean showOrphans );

    /**
     * Delete an arrayDesign.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/deleteArrayDesign.html")
    public abstract ModelAndView delete( HttpServletRequest request, HttpServletResponse response );

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/downloadAnnotationFile.html")
    public abstract ModelAndView downloadAnnotationFile( HttpServletRequest request, HttpServletResponse response );

    /**
     * Show array designs that match search criteria.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/filterArrayDesigns.html")
    public abstract ModelAndView filter( HttpServletRequest request, HttpServletResponse response );

    /**
     * Build summary report for an array design
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/generateArrayDesignSummary.html")
    public abstract ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response );

    /**
     * AJAX
     * 
     * @param arrayDesignIds
     * @param showMergees
     * @param showOrphans
     * @return
     */
    public abstract Collection<ArrayDesignValueObject> getArrayDesigns( Long[] arrayDesignIds, boolean showMergees,
            boolean showOrphans );

    /**
     * Exposed for AJAX calls.
     * 
     * @param ed
     * @return
     */
    public abstract Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator ed );

    /**
     * @param arrayDesign
     * @return
     */
    public abstract Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign );

    /**
     * AJAX
     * 
     * @param ed
     * @return the HTML to display.
     */
    public abstract Map<String, String> getReportHtml( EntityDelegator ed );

    /**
     * AJAX
     * 
     * @param ArrayDesignValueObject
     * @return
     */
    public abstract String getSummaryForArrayDesign( Long id );

    /**
     * AJAX
     * 
     * @param arrayDesignIds
     * @param showMergees
     * @param showOrphans
     * @return
     */
    public abstract Collection<ArrayDesignValueObject> loadArrayDesignsForShowAll( Long[] arrayDesignIds );

    /**
     * AJAX
     * 
     * @param arrayDesignIds
     * @param showMergees
     * @param showOrphans
     * @return
     */
    public abstract ArrayDesignValueObject loadArrayDesignsSummary();

    /**
     * AJAX
     * 
     * @return the taskid
     */
    public abstract String remove( EntityDelegator ed );

    /**
     * Show all array designs, or according to a list of IDs passed in.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/showAllArrayDesigns.html")
    public abstract ModelAndView showAllArrayDesigns( HttpServletRequest request, HttpServletResponse response );

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @RequestMapping({ "/showArrayDesign.html", "/" })
    public abstract ModelAndView showArrayDesign( HttpServletRequest request, HttpServletResponse response );

    /**
     * Show (some of) the probes from an array.
     * 
     * @param request
     * @return
     */
    @RequestMapping("/showCompositeSequenceSummary.html")
    public abstract ModelAndView showCompositeSequences( HttpServletRequest request );

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showExpressionExperiments.html")
    public abstract ModelAndView showExpressionExperiments( HttpServletRequest request );

    /**
     * AJAX
     * 
     * @param ed
     * @return the taskid
     */
    public abstract String updateReport( EntityDelegator ed );

    /**
     * AJAX
     * 
     * @param ed
     * @return the taskid
     */
    public abstract String updateReportById( Long id );

}
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    ModelAndView delete( @RequestParam("id") Long id );

    @RequestMapping("/downloadAnnotationFile.html")
    void downloadAnnotationFile( @RequestParam("id") Long arrayDesignId, @RequestParam(value = "fileType", required = false) String fileType, HttpServletResponse httpServletResponse ) throws IOException;

    /**
     * Show array designs that match search criteria.
     */
    @RequestMapping("/filterArrayDesigns.html")
    ModelAndView filter( @RequestParam("filter") String filter );

    /**
     * Build summary report for an array design
     */
    @RequestMapping("/generateArrayDesignSummary.html")
    ModelAndView generateSummary( @RequestParam(value = "id", required = false) Long id );

    Collection<ArrayDesignValueObject> getArrayDesigns( Long[] arrayDesignIds, boolean showMergees,
            boolean showOrphans );

    /**
     * Exposed for AJAX calls.
     */
    Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator<ArrayDesign> ed );

    Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign );

    /**
     * @return the HTML to display.
     */
    Map<String, String> getReportHtml( EntityDelegator<ArrayDesign> ed );

    String getSummaryForArrayDesign( Long id );

    Collection<ArrayDesignValueObject> loadArrayDesignsForShowAll( Long[] arrayDesignIds );

    ArrayDesignValueObject loadArrayDesignsSummary();

    String remove( EntityDelegator<ArrayDesign> ed );

    /**
     * Show all array designs, or according to a list of IDs passed in.
     */
    @RequestMapping("/showAllArrayDesigns.html")
    ModelAndView showAllArrayDesigns();

    @RequestMapping(value = { "/showArrayDesign.html", "/" }, params = { "id" })
    ModelAndView showArrayDesign( @RequestParam("id") Long id );

    @RequestMapping(value = { "/showArrayDesign.html", "/" }, params = { "name" })
    ModelAndView showArrayDesignByName( @RequestParam("name") String name );

    /**
     * Show (some of) the probes from an array.
     */
    @RequestMapping("/showCompositeSequenceSummary.html")
    ModelAndView showCompositeSequences( @RequestParam("id") Long id );

    /**
     * shows a list of BioAssays for an expression experiment subset
     *
     * @return ModelAndView
     */
    @RequestMapping("/showExpressionExperiments.html")
    ModelAndView showExpressionExperiments( @RequestParam("id") Long id );

    String updateReport( EntityDelegator<ArrayDesign> ed );

    String updateReportById( Long id );

    ArrayDesignValueObject getDetails( Long id );

}
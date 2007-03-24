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
package ubic.gemma.web.controller.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.expression.arrayDesign.ArrayDesignReportService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignController" name="arrayDesignController"
 * @springproperty name="validator" ref="arrayDesignValidator"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "arrayDesignReportService" ref="arrayDesignReportService"
 * @spring.property name = "arrayDesignMapResultService" ref="arrayDesignMapResultService"
 * @spring.property name="methodNameResolver" ref="arrayDesignActions"
 * @spring.property name="searchService" ref="searchService"
 */
public class ArrayDesignController extends BackgroundProcessingMultiActionController {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog( ArrayDesignController.class.getName() );

    private SearchService searchService;
    private ArrayDesignService arrayDesignService = null;
    private ArrayDesignReportService arrayDesignReportService = null;
    private ArrayDesignMapResultService arrayDesignMapResultService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private final String messageName = "Array design with name";
    private final String identifierNotFound = "Must provide a valid Array Design identifier";

    /**
     * @param arrayDesignReportService the arrayDesignReportService to set
     */
    public void setArrayDesignReportService( ArrayDesignReportService arrayDesignReportService ) {
        this.arrayDesignReportService = arrayDesignReportService;
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {

        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name or Id" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( Long.parseLong( idStr ) );

        ModelAndView mav = new ModelAndView( "arrayDesign.compositeSequences" );

        Collection rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, 100 );
        Collection compositeSequenceSummary = arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );

        // Collection compositeSequenceSummary = arrayDesignMapResultService.getSummaryMapValueObjects( arrayDesign );

        if ( compositeSequenceSummary == null || compositeSequenceSummary.size() == 0 ) {
            // / FIXME, return error or do something else intelligent.
        }

        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "sequenceData", compositeSequenceSummary );
        mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    // @SuppressWarnings({ "unused", "unchecked" })
    @SuppressWarnings("unchecked")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );
        String idStr = request.getParameter( "id" );

        if ( ( name == null ) && ( idStr == null ) ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name or Id" );
        }
        ArrayDesign arrayDesign = null;
        if ( idStr != null ) {
            arrayDesign = arrayDesignService.load( Long.parseLong( idStr ) );
            request.setAttribute( "id", idStr );
        } else if ( name != null ) {
            arrayDesign = arrayDesignService.findArrayDesignByName( name );
            request.setAttribute( "name", name );
        }

        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( name + " not found" );
        }
        long id = arrayDesign.getId();

        Long numCompositeSequences = new Long( arrayDesignService.getCompositeSequenceCount( arrayDesign ) );
        Collection<ExpressionExperiment> ee = arrayDesignService.getExpressionExperiments( arrayDesign );
        Long numExpressionExperiments = new Long( ee.size() );
        Taxon t = arrayDesignService.getTaxon( id );
        String taxon = "";
        if ( t != null ) {
            taxon = t.getScientificName();
        } else {
            taxon = "(Taxon not known)";
        }
        String colorString = formatTechnologyType( arrayDesign );

        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( id );

        String eeIds = formatExpressionExperimentIds( ee );

        ModelAndView mav = new ModelAndView( "arrayDesign.detail" );

        Collection<ArrayDesign> subsumees = arrayDesign.getSubsumedArrayDesigns();
        ArrayDesign subsumer = arrayDesign.getSubsumingArrayDesign();

        Collection<ArrayDesign> mergees = arrayDesign.getMergees();
        ArrayDesign merger = arrayDesign.getMergedInto();

        mav.addObject( "subsumer", subsumer );
        mav.addObject( "subsumees", subsumees );
        mav.addObject( "merger", merger );
        mav.addObject( "mergees", mergees );
        mav.addObject( "taxon", taxon );
        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "numCompositeSequences", numCompositeSequences );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );

        mav.addObject( "expressionExperimentIds", eeIds );
        mav.addObject( "technologyType", colorString );
        mav.addObject( "summary", summary );
        return mav;
    }

    private String formatExpressionExperimentIds( Collection<ExpressionExperiment> ee ) {
        String[] eeIdList = new String[ee.size()];
        int i = 0;
        for ( ExpressionExperiment e : ee ) {
            eeIdList[i] = e.getId().toString();
            i++;
        }
        String eeIds = StringUtils.join( eeIdList, "," );
        return eeIds;
    }

    private String formatTechnologyType( ArrayDesign arrayDesign ) {
        String techType = arrayDesign.getTechnologyType().getValue();
        String colorString = "";
        if ( techType.equalsIgnoreCase( "ONECOLOR" ) ) {
            colorString = "one-color";
        } else if ( techType.equalsIgnoreCase( "TWOCOLOR" ) ) {
            colorString = "two-color";
        } else if ( techType.equalsIgnoreCase( "DUALMODE" ) ) {
            colorString = "dual mode";
        } else {
            colorString = "No color";
        }
        return colorString;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings({ "unused", "unchecked" })
    @SuppressWarnings("unchecked")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<ArrayDesignValueObject> valueObjects = new ArrayList<ArrayDesignValueObject>();
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject();
        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( sId == null ) {
            this.saveMessage( request, "Displaying all Arrays" );
            valueObjects.addAll( arrayDesignService.loadAllValueObjects() );
            arrayDesignReportService.fillInValueObjects( valueObjects );
        }

        // if ids are specified, then display only those arrayDesigns
        else {
            Collection ids = new ArrayList<Long>();

            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                ids.add( new Long( idList[i] ) );
            }
            valueObjects.addAll( arrayDesignService.loadValueObjects( ids ) );
        }

        arrayDesignReportService.fillEventInformation( valueObjects );
        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );

        /*
         * for ( ArrayDesignValueObject ad : arrayDesigns ) { ad.setLastSequenceAnalysis(
         * arrayDesignReportService.getLastSequenceAnalysisEvent( ad.getId() ) ); ad.setLastGeneMapping(
         * arrayDesignReportService.getLastGeneMappingEvent( ad.getId() ) ); ad.setLastSequenceUpdate(
         * arrayDesignReportService.getLastSequenceUpdateEvent( ad.getId() ) ); }
         */

        Long numArrayDesigns = new Long( valueObjects.size() );
        ModelAndView mav = new ModelAndView( "arrayDesigns" );
        mav.addObject( "arrayDesigns", valueObjects );
        mav.addObject( "numArrayDesigns", numArrayDesigns );
        mav.addObject( "summary", summary );

        return mav;
    }

    /**
     * Show statistics for all (by default) array designs.
     * 
     * @param request
     * @param response
     * @return
     */

    /*
     * // @SuppressWarnings({ "unused", "unchecked" }) @SuppressWarnings("unchecked") public ModelAndView showAllStats(
     * HttpServletRequest request, HttpServletResponse response ) { String sId = request.getParameter( "id" );
     * Collection<ArrayDesignValueObject> arrayDesigns = new ArrayList<ArrayDesignValueObject>(); Collection<ArrayDesignValueObjectSummary>
     * summaries = new ArrayList<ArrayDesignValueObjectSummary>(); // if no IDs are specified, then load all
     * expressionExperiments and show the summary (if available) if ( sId == null ) { this.saveMessage( request,
     * "Displaying all Arrays" ); arrayDesigns.addAll( arrayDesignService.loadAllValueObjects() ); } // if ids are
     * specified, then display only those arrayDesigns else { Collection ids = new ArrayList<Long>(); String[] idList =
     * StringUtils.split( sId, ',' ); for ( int i = 0; i < idList.length; i++ ) { ids.add( new Long( idList[i] ) ); }
     * arrayDesigns.addAll( arrayDesignService.loadValueObjects( ids ) ); } for ( ArrayDesignValueObject ad :
     * arrayDesigns ) { String summary = arrayDesignReportService.getArrayDesignReport( ad.getId() );
     * ArrayDesignValueObjectSummary adSummary = new ArrayDesignValueObjectSummary( ad, summary );
     * adSummary.setLastSequenceAnalysis( arrayDesignReportService.getLastSequenceAnalysisEvent( ad.getId() ) );
     * adSummary.setLastGeneMapping( arrayDesignReportService.getLastGeneMappingEvent( ad.getId() ) );
     * adSummary.setLastSequenceUpdate( arrayDesignReportService.getLastSequenceUpdateEvent( ad.getId() ) );
     * summaries.add( adSummary ); } Long numArrayDesigns = new Long( arrayDesigns.size() ); ModelAndView mav = new
     * ModelAndView( "arrayDesignStatistics" ); mav.addObject( "arrayDesigns", summaries ); mav.addObject(
     * "numArrayDesigns", numArrayDesigns ); return mav; }
     */
    /**
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings({ "unused", "unchecked" })
    public ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );

        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( sId == null ) {
            return startJob( request, new GenerateSummary( request, arrayDesignReportService ) );
        } else {
            Long id = Long.parseLong( sId );
            return startJob( request, new GenerateSummary( request, arrayDesignReportService, id ) );
        }
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String stringId = request.getParameter( "id" );

        if ( stringId == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide an id" );
        }

        Long id = null;
        try {
            id = Long.parseLong( stringId );
        } catch ( NumberFormatException e ) {
            throw new EntityNotFoundException( "Identifier was invalid" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( arrayDesign + " not found" );
        }

        // check that no EE depend on the arraydesign we want to delete
        // Do this by checking if there are any bioassays that depend this AD
        Collection assays = arrayDesignService.getAllAssociatedBioAssays( id );
        if ( assays.size() != 0 ) {
            // String eeName = ( ( BioAssay ) assays.iterator().next() )
            // todo tell user what EE depends on this array design
            addMessage( request, "Array  " + arrayDesign.getName()
                    + " can't be deleted. Dataset has a dependency on this Array.", new Object[] { messageName,
                    arrayDesign.getName() } );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );
        }

        return startJob( request, new RemoveArrayJob( request, arrayDesign, arrayDesignService ) );

    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showExpressionExperiments( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            this.addMessage( request, "errors.objectnotfound", new Object[] { "Array Design " } );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );
        }

        Collection ees = arrayDesignService.getExpressionExperiments( arrayDesign );
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( Object object : ees ) {
            eeIds.add( ( ( ExpressionExperiment ) object ).getId() );
        }
        String ids = StringUtils.join( eeIds.toArray(), "," );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + ids ) );
    }

    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            this.saveMessage( request, "No search critera provided" );
            return showAll( request, response );
        }

        Collection<ArrayDesign> searchResults = searchService.compassArrayDesignSearch( filter );

        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            this.saveMessage( request, "Your search yielded no results" );
            return showAll( request, response );
        }

        String list = "";

        if ( searchResults.size() == 1 ) {
            ArrayDesign arrayDesign = searchResults.iterator().next();
            this.saveMessage( request, "Matched one : " + arrayDesign.getName() + "(" + arrayDesign.getShortName()
                    + ")" );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id=" + arrayDesign.getId() ) );
        } else {
            for ( ArrayDesign ad : searchResults )
                list += ad.getId() + ",";

            this.saveMessage( request, "Search Criteria: " + filter );
            this.saveMessage( request, searchResults.size() + " Array Designs matched your search." );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?id=" + list ) );
        }

    }

    /**
     * @return the searchService
     */
    public SearchService getSearchService() {
        return searchService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * Inner class used for deleting array designs
     * <hr>
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$
     */
    class RemoveArrayJob extends BackgroundControllerJob<ModelAndView> {

        private ArrayDesignService arrayDesignService;
        private ArrayDesign ad;

        public RemoveArrayJob( HttpServletRequest request, ArrayDesign ad, ArrayDesignService arrayDesignService ) {
            super( request, getMessageUtil() );
            this.arrayDesignService = arrayDesignService;
            this.ad = ad;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Deleting Array: " + ad.getShortName() );

            arrayDesignService.remove( ad );
            saveMessage( "Array " + ad.getShortName() + " removed from Database." );
            ad = null;

            ProgressManager.destroyProgressJob( job );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );

        }
    }

    /**
     * Inner class used for deleting array designs
     * <hr>
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$
     */
    class GenerateSummary extends BackgroundControllerJob<ModelAndView> {

        private ArrayDesignReportService arrayDesignReportService;
        private Long id;

        public GenerateSummary( HttpServletRequest request, ArrayDesignReportService arrayDesignReportService ) {
            super( request, getMessageUtil() );
            this.arrayDesignReportService = arrayDesignReportService;
            id = null;
        }

        public GenerateSummary( HttpServletRequest request, ArrayDesignReportService arrayDesignReportService, Long id ) {
            super( request, getMessageUtil() );
            this.arrayDesignReportService = arrayDesignReportService;
            this.id = id;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Generating ArrayDesign Report summary" );

            if ( id == null ) {
                saveMessage( "Generated summary for all platforms" );
                job.updateProgress( "Generated summary for all platforms" );
                arrayDesignReportService.generateArrayDesignReport();
            } else {
                saveMessage( "Generating summary for platform " + id );
                job.updateProgress( "Generating summary for specified platform" );
                arrayDesignReportService.generateArrayDesignReport( id );
            }
            ProgressManager.destroyProgressJob( job );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesignStatistics.html" ) );

        }
    }

    /**
     * @param arrayDesignMapResultService the arrayDesignMapResultService to set
     */
    public void setArrayDesignMapResultService( ArrayDesignMapResultService arrayDesignMapResultService ) {
        this.arrayDesignMapResultService = arrayDesignMapResultService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
}

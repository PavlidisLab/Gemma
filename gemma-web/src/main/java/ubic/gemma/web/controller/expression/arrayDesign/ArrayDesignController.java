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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.audit.AuditableUtil;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.taglib.arrayDesign.ArrayDesignHtmlUtil;
import ubic.gemma.web.taglib.displaytag.ArrayDesignValueObjectComparator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/arrays")
public class ArrayDesignController extends AbstractTaskService {

    /**
     * Inner class used for building array design summary
     */
    class GenerateSummary extends BackgroundJob<TaskCommand> {

        public GenerateSummary( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {

            if ( this.command.getEntityId() == null ) {
                log.info( "Generating summary for all platforms" );
                arrayDesignReportService.generateArrayDesignReport();
                return new TaskResult( command, new ModelAndView( new RedirectView(
                        "/Gemma/arrays/showAllArrayDesignStatistics.html" ) ) );
            } else {
                ArrayDesignValueObject report = arrayDesignReportService.generateArrayDesignReport( this.command
                        .getEntityId() );
                return new TaskResult( command, report );
            }

        }
    }

    /**
     * Inner class used for deleting array designs
     */
    class RemoveArrayJob extends BackgroundJob<TaskCommand> {

        public RemoveArrayJob( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            ArrayDesign ad = arrayDesignService.load( command.getEntityId() );
            if ( ad == null ) {
                throw new IllegalArgumentException( "Could not load array design with id=" + command.getEntityId() );
            }
            arrayDesignService.remove( ad );
            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject( "message", "Array " + ad.getShortName()
                    + " removed from Database." ) );

        }
    }

    private static boolean AJAX = true;

    /**
     * Instead of showing all the probes for the array, we might only fetch some of them.
     */
    private static final int NUM_PROBES_TO_SHOW = 500;

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService = null;

    @Autowired
    private ArrayDesignReportService arrayDesignReportService = null;

    @Autowired
    private ArrayDesignService arrayDesignService = null;

    @Autowired
    private AuditableUtil auditableUtil;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private CompositeSequenceService compositeSequenceService = null;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    public String addAlternateName( Long arrayDesignId, String alternateName ) {
        ArrayDesign ad = arrayDesignService.load( arrayDesignId );
        if ( ad == null ) {
            throw new IllegalArgumentException( "No such array design with id=" + arrayDesignId );
        }

        if ( StringUtils.isBlank( alternateName ) ) {
            return formatAlternateNames( ad );
        }

        AlternateName newName = AlternateName.Factory.newInstance( alternateName );

        ad.getAlternateNames().add( newName );

        arrayDesignService.update( ad );
        return formatAlternateNames( ad );
    }

    /**
     * Delete an arrayDesign.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/deleteArrayDesign.html")
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
            throw new EntityNotFoundException( "Array design with id=" + id + " not found" );
        }

        // check that no EE depend on the arraydesign we want to delete
        // Do this by checking if there are any bioassays that depend this AD
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( id );
        if ( assays.size() != 0 ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Array  " + arrayDesign.getName()
                            + " can't be deleted. Dataset has a dependency on this Array." );
        }

        RemoveArrayJob job = new RemoveArrayJob( new TaskCommand( arrayDesign.getId() ) );
        super.startTask( job );

        return new ModelAndView().addObject( "taskId", job.getTaskId() );

    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/downloadAnnotationFile.html")
    public ModelAndView downloadAnnotationFile( HttpServletRequest request, HttpServletResponse response ) {

        String arrayDesignIdStr = request.getParameter( "id" );
        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name or Id" );
        }

        String fileType = request.getParameter( "fileType" );
        if ( fileType == null )
            fileType = ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        else if ( fileType.equalsIgnoreCase( "noParents" ) )
            fileType = ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        else if ( fileType.equalsIgnoreCase( "bioProcess" ) )
            fileType = ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        else
            fileType = ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;

        ArrayDesign arrayDesign = arrayDesignService.load( Long.parseLong( arrayDesignIdStr ) );
        String fileBaseName = ArrayDesignAnnotationService.mungeFileName( arrayDesign.getShortName() );
        String fileName = fileBaseName + fileType + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;

        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
        InputStream reader;
        try {
            reader = new BufferedInputStream( new FileInputStream( f ) );
        } catch ( FileNotFoundException fnfe ) {
            log.warn( "Annotation file " + fileName + " can't be found at " + fnfe );
            return null;
        }

        response.setHeader( "Content-disposition", "attachment; filename=" + fileName );
        response.setContentType( "application/octet-stream" );

        try {
            OutputStream outputStream = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ( ( len = reader.read( buf ) ) > 0 ) {
                outputStream.write( buf, 0, len );
            }
            reader.close();

        } catch ( IOException ioe ) {
            log.warn( "Failure during streaming of annotation file " + fileName + " Error: " + ioe );
        }

        return null;
    }

    /**
     * Show array designs that match search criteria.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/filterArrayDesigns.html")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "No search criteria provided" );
        }

        Collection<SearchResult> searchResults = searchService.search( SearchSettings.arrayDesignSearch( filter ) )
                .get( ArrayDesign.class );

        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "No search criteria provided" );

        }

        String list = "";

        if ( searchResults.size() == 1 ) {
            ArrayDesign arrayDesign = arrayDesignService.load( searchResults.iterator().next().getId() );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id=" + arrayDesign.getId() ) )
                    .addObject( "message", "Matched one : " + arrayDesign.getName() + "(" + arrayDesign.getShortName()
                            + ")" );
        }

        for ( SearchResult ad : searchResults ) {
            list += ad.getId() + ",";
        }

        overallWatch.stop();
        Long overallElapsed = overallWatch.getTime();
        log.info( "Generating the AD list:  (" + list + ") took: " + overallElapsed / 1000 + "s " );

        return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?id=" + list ) ).addObject(
                "message", searchResults.size() + " Array Designs matched your search." );

    }

    /**
     * Build summary report for an array design
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/generateArrayDesignSummary.html")
    public ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );

        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        GenerateSummary job;
        if ( sId == null ) {
            job = new GenerateSummary( new TaskCommand() );
        } else {
            Long id = Long.parseLong( sId );
            job = new GenerateSummary( new TaskCommand( id ) );
        }
        startTask( job );

        return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?id=" + sId ) ).addObject(
                "taskId", job.getTaskId() );
    }

    /**
     * AJAX
     * 
     * @param arrayDesignIds
     * @param showMergees
     * @param showOrphans
     * @return
     */
    public Collection<ArrayDesignValueObject> getArrayDesigns( Collection<Long> arrayDesignIds, boolean showMergees,
            boolean showOrphans ) {
        List<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        // If no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( arrayDesignIds == null || arrayDesignIds.isEmpty() ) {
            result.addAll( arrayDesignService.loadAllValueObjects() );
        } else {// if ids are specified, then display only those arrayDesigns
            result.addAll( arrayDesignService.loadValueObjects( arrayDesignIds ) );
        }

        // Filter...
        Collection<ArrayDesignValueObject> toHide = new HashSet<ArrayDesignValueObject>();
        for ( ArrayDesignValueObject a : result ) {
            if ( !showMergees && a.getIsMergee() && a.getExpressionExperimentCount() == 0 ) {
                toHide.add( a );
            }
            if ( !showOrphans && ( a.getExpressionExperimentCount() == null || a.getExpressionExperimentCount() == 0 ) ) {
                toHide.add( a );
            }
        }
        result.removeAll( toHide );

        // flag or remove troubled - these are not usable. If we're admin, show them anyway.
        if ( SecurityService.isUserAdmin() ) {
            auditableUtil.flagTroubledArrayDesigns( result );
        } else {
            auditableUtil.removeTroubledArrayDesigns( result );
        }

        Collections.sort( result, new ArrayDesignValueObjectComparator() );

        return result;
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param ed
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        return this.getDesignSummaries( arrayDesign );
    }

    /**
     * @param arrayDesign
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign ) {
        Collection rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, NUM_PROBES_TO_SHOW );
        Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
                .getSummaryMapValueObjects( rawSummaries );
        return summaries;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return the HTML to display.
     */
    public Map<String, String> getReportHtml( EntityDelegator ed ) {
        assert ed.getId() != null;
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( ed.getId() );
        Map<String, String> result = new HashMap<String, String>();

        result.put( "id", ed.getId().toString() );
        if ( summary == null )
            result.put( "html", "Not available" );
        else
            result.put( "html", ArrayDesignHtmlUtil.getSummaryHtml( summary ) );
        return result;
    }

    /**
     * AJAX
     * 
     * @return the taskid
     */
    public String remove( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( ed.getId() + " not found" );
        }
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( ed.getId() );
        if ( assays.size() != 0 ) {
            throw new IllegalArgumentException( "Cannot delete " + arrayDesign
                    + ", it is used by an expression experiment" );
        }

        RemoveArrayJob job = new RemoveArrayJob( new TaskCommand( arrayDesign.getId() ) );

        super.startTask( job );

        return job.getTaskId();
    }

    /**
     * Show all array designs, or according to a list of IDs passed in.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/showAllArrayDesigns.html")
    public ModelAndView showAllArrayDesigns( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        String sId = request.getParameter( "id" );
        String sShowMerge = request.getParameter( "showMerg" );
        String sShowOrph = request.getParameter( "showOrph" );

        boolean showMergees = Boolean.parseBoolean( sShowMerge );
        boolean showOrphans = Boolean.parseBoolean( sShowOrph );

        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject();

        Collection<Long> ids = new ArrayList<Long>();
        if ( sId != null ) {
            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                try {
                    ids.add( new Long( idList[i] ) );
                } catch ( NumberFormatException e ) {
                    // just keep going
                }
            }
            if ( ids.isEmpty() ) {
                throw new IllegalArgumentException( "No valid ids in " + sId );
            }
        }

        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( ids, showMergees, showOrphans );

        if ( !SecurityService.isUserAdmin() ) {
            removeTroubledArrayDesigns( valueObjects );
        }

        arrayDesignReportService.fillInValueObjects( valueObjects );
        arrayDesignReportService.fillEventInformation( valueObjects );
        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );

        int numArrayDesigns = valueObjects.size();
        ModelAndView mav = new ModelAndView( "arrayDesigns" );
        mav.addObject( "showMergees", showMergees );
        mav.addObject( "showOrphans", showOrphans );
        mav.addObject( "arrayDesigns", valueObjects );
        mav.addObject( "numArrayDesigns", numArrayDesigns );
        mav.addObject( "summary", summary );

        log.info( "ArrayDesign.showall took: " + overallWatch.getTime() + "ms for " + numArrayDesigns );

        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @RequestMapping( { "/showArrayDesign.html", "/" })
    public ModelAndView showArrayDesign( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );
        String idStr = request.getParameter( "id" );

        if ( ( name == null ) && ( idStr == null ) ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Must provide an array design name or id. Displaying all Arrays" );

        }
        ArrayDesign arrayDesign = null;
        if ( idStr != null ) {
            arrayDesign = arrayDesignService.load( Long.parseLong( idStr ) );
            request.setAttribute( "id", idStr );
        } else if ( name != null ) {
            arrayDesign = arrayDesignService.findByName( name );
            request.setAttribute( "name", name );
        }

        if ( arrayDesign == null ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Unable to load Array Design with id: " + idStr + ". Displaying all Arrays" );
        }

        arrayDesign = arrayDesignService.thawLite( arrayDesign );

        long id = arrayDesign.getId();

        Integer numCompositeSequences = arrayDesignService.getCompositeSequenceCount( arrayDesign );
        Collection<ExpressionExperiment> ee = arrayDesignService.getExpressionExperiments( arrayDesign );
        int numExpressionExperiments = ee.size();

        Collection<Taxon> t = arrayDesignService.getTaxa( id );

        Taxon primaryTaxon = arrayDesign.getPrimaryTaxon();

        String taxa = formatTaxa( primaryTaxon, t );

        String colorString = formatTechnologyType( arrayDesign );

        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( id );

        String eeIds = formatExpressionExperimentIds( ee );

        ModelAndView mav = new ModelAndView( "arrayDesign.detail" );

        AuditEvent troubleEvent = auditTrailService.getLastTroubleEvent( arrayDesign );
        if ( troubleEvent != null ) {
            //auditEventService.thaw( troubleEvent );
            mav.addObject( "troubleEvent", troubleEvent );
            mav.addObject( "troubleEventDescription", StringEscapeUtils.escapeHtml( troubleEvent.toString() ) );
        }
        AuditEvent validatedEvent = auditTrailService.getLastValidationEvent( arrayDesign );
        if ( validatedEvent != null ) {
            //auditEventService.thaw( validatedEvent );
            mav.addObject( "validatedEvent", validatedEvent );
            mav.addObject( "validatedEventDescription", StringEscapeUtils.escapeHtml( validatedEvent.toString() ) );
        }

        Collection<ArrayDesign> subsumees = arrayDesignService.thawLite( arrayDesign.getSubsumedArrayDesigns() );

        ArrayDesign subsumer = arrayDesign.getSubsumingArrayDesign();

        Collection<ArrayDesign> mergees = arrayDesignService.thawLite( arrayDesign.getMergees() );

        ArrayDesign merger = arrayDesign.getMergedInto();

        getAnnotationFileLinks( arrayDesign, mav );

        mav.addObject( "subsumer", subsumer );
        mav.addObject( "subsumees", subsumees );
        mav.addObject( "merger", merger );
        mav.addObject( "mergees", mergees );
        mav.addObject( "taxon", taxa );
        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "alternateNames", this.formatAlternateNames( arrayDesign ) );
        mav.addObject( "numCompositeSequences", numCompositeSequences );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );

        mav.addObject( "expressionExperimentIds", eeIds );
        mav.addObject( "technologyType", colorString );
        mav.addObject( "summary", summary );
        return mav;
    }

    /**
     * Show (some of) the probes from an array.
     * 
     * @param request
     * @return
     */
    @RequestMapping("/showCompositeSequenceSummary.html")
    public ModelAndView showCompositeSequences( HttpServletRequest request ) {

        String arrayDesignIdStr = request.getParameter( "id" );

        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name or Id" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( Long.parseLong( arrayDesignIdStr ) );
        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );

        if ( !AJAX ) {
            Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = getDesignSummaries( arrayDesign );
            if ( compositeSequenceSummary == null || compositeSequenceSummary.size() == 0 ) {
                throw new RuntimeException( "No probes found for " + arrayDesign );
            }
            mav.addObject( "sequenceData", compositeSequenceSummary );
            mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        }

        mav.addObject( "arrayDesign", arrayDesign );

        return mav;
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showExpressionExperiments.html")
    public ModelAndView showExpressionExperiments( HttpServletRequest request ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Not found" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Array design with id=" + id + " not found" );
        }

        Collection<ExpressionExperiment> ees = arrayDesignService.getExpressionExperiments( arrayDesign );
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment object : ees ) {
            eeIds.add( object.getId() );
        }
        String ids = StringUtils.join( eeIds.toArray(), "," );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + ids ) );
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return the taskid
     */
    public String updateReport( EntityDelegator ed ) {
        GenerateSummary runner = new GenerateSummary( new TaskCommand( ed.getId() ) );
        super.startTask( runner );
        return runner.getTaskId();
    }

    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
    }

    /**
     * @param ad
     * @return
     */
    private String formatAlternateNames( ArrayDesign ad ) {
        Collection<String> names = new HashSet<String>();
        for ( AlternateName an : ad.getAlternateNames() ) {
            names.add( an.getName() );
        }
        return StringUtils.join( names, "; " );
    }

    private String formatExpressionExperimentIds( Collection<ExpressionExperiment> ee ) {
        Collection<Long> eeIds = EntityUtils.getIds( ee );
        String eeIdString = StringUtils.join( eeIds, "," );
        return eeIdString;
    }

    /**
     * Method to format taxon list for display.
     * 
     * @param primaryTaxon
     * @param taxonSet Collection of taxon used to create array/platform
     * @return Alpabetically sorted semicolon separated list of scientific names of taxa used on array/platform
     */
    private String formatTaxa( Taxon primaryTaxon, Collection<Taxon> taxonSet ) {

        taxonService.thaw( primaryTaxon );

        String taxonListString = primaryTaxon.getScientificName();
        int i = 0;
        if ( !taxonSet.isEmpty() ) {
            Collection<String> taxonList = new TreeSet<String>();
            for ( Taxon taxon : taxonSet ) {
                if ( taxon.equals( primaryTaxon ) ) continue;

                taxonService.thaw( taxon );
                taxonList.add( taxon.getScientificName() );

                i++;
            }

            if ( taxonList.size() > 0 ) {
                taxonListString = taxonListString + " (primary; Also contains sequences from: "
                        + StringUtils.join( taxonList, "; " ) + ")";
            }

        }

        return taxonListString;
    }

    /**
     * @param arrayDesign
     * @return
     */
    private String formatTechnologyType( ArrayDesign arrayDesign ) {
        TechnologyType technologyType = arrayDesign.getTechnologyType();

        if ( technologyType == null ) {
            return "Not specified";
        }

        String techType = technologyType.getValue();
        String colorString = "";
        if ( techType.equalsIgnoreCase( "ONECOLOR" ) ) {
            colorString = "one-color";
        } else if ( techType.equalsIgnoreCase( "TWOCOLOR" ) ) {
            colorString = "two-color";
        } else if ( techType.equalsIgnoreCase( "DUALMODE" ) ) {
            colorString = "dual mode";
        } else {
            colorString = "Not specified";
        }
        return colorString;
    }

    /**
     * @param arrayDesign
     * @param mav
     */
    private void getAnnotationFileLinks( ArrayDesign arrayDesign, ModelAndView mav ) {

        ArrayDesign merger = arrayDesign.getMergedInto();
        ArrayDesign annotationFileDesign;
        if ( merger != null )
            annotationFileDesign = merger;
        else
            annotationFileDesign = arrayDesign;

        String mungedShortName = ArrayDesignAnnotationService.mungeFileName( annotationFileDesign.getShortName() );
        File fnp = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedShortName
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

        File fap = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedShortName
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

        File fbp = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedShortName
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

        // context here is Gemma/arrays
        if ( fnp.exists() ) {
            mav.addObject( "noParentsAnnotationLink", "downloadAnnotationFile.html?id=" + annotationFileDesign.getId()
                    + "&fileType=noParents" );
        }
        if ( fap.exists() ) {
            mav.addObject( "allParentsAnnotationLink", "downloadAnnotationFile.html?id=" + annotationFileDesign.getId()
                    + "&fileType=allParents" );
        }
        if ( fbp.exists() ) {
            mav.addObject( "bioProcessAnnotationLink", "downloadAnnotationFile.html?id=" + annotationFileDesign.getId()
                    + "&fileType=bioProcess" );
        }

    }

    /**
     * @param valueObjects
     */
    private void removeTroubledArrayDesigns( Collection<ArrayDesignValueObject> valueObjects ) {

        if ( valueObjects == null || valueObjects.size() == 0 ) {
            log.warn( "No ads to remove troubled from" );
            return;
        }

        Collection<Long> ids = new HashSet<Long>();
        for ( ArrayDesignValueObject advo : valueObjects ) {
            ids.add( advo.getId() );
        }

        int size = valueObjects.size();
        final Map<Long, AuditEvent> trouble = arrayDesignService.getLastTroubleEvent( ids );

        CollectionUtils.filter( valueObjects, new Predicate() {
            public boolean evaluate( Object vo ) {
                boolean hasTrouble = trouble.get( ( ( ArrayDesignValueObject ) vo ).getId() ) != null;
                return !hasTrouble;
            }
        } );
        int newSize = valueObjects.size();
        if ( newSize != size ) {
            assert newSize < size;
            log.info( "Removed " + ( size - newSize ) + " array designs with 'trouble' flags, leaving " + newSize );
        }
    }
}

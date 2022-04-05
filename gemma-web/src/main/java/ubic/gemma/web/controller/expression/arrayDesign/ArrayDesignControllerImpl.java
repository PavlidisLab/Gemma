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

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.security.audit.AuditableUtil;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.taglib.arrayDesign.ArrayDesignHtmlUtil;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * Note: do not use parametrized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 *
 * @author keshav
 */
@Controller
@RequestMapping("/arrays")
public class ArrayDesignControllerImpl implements ArrayDesignController {

    private static final String SUPPORT_EMAIL = "pavlab-support@msl.ubc.ca"; // FIXME factor out as config

    private static final Log log = LogFactory.getLog( ArrayDesignControllerImpl.class.getName() );

    /**
     * Instead of showing all the probes for the array, we might only fetch some of them.
     */
    private static final int NUM_PROBES_TO_SHOW = 500;
    private static final boolean AJAX = true;
    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService;
    @Autowired
    private ArrayDesignReportService arrayDesignReportService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private AuditableUtil auditableUtil;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private SearchService searchService;

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private ArrayDesignAnnotationService annotationFileService;

    @Override
    public String addAlternateName( Long arrayDesignId, String alternateName ) {
        ArrayDesign ad = arrayDesignService.load( arrayDesignId );
        if ( ad == null ) {
            throw new IllegalArgumentException( "No such platform with id=" + arrayDesignId );
        }

        if ( StringUtils.isBlank( alternateName ) ) {
            return formatAlternateNames( ad );
        }

        AlternateName newName = AlternateName.Factory.newInstance( alternateName );

        ad.getAlternateNames().add( newName );

        arrayDesignService.update( ad );
        return formatAlternateNames( ad );
    }

    @Override
    public JsonReaderResponse<ArrayDesignValueObject> browse( ListBatchCommand batch, Long[] ids, boolean showMerged,
            boolean showOrphans ) {

        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( ids, showMerged, showOrphans );

        if ( !SecurityUtil.isUserAdmin() ) {
            auditableUtil.removeTroubledArrayDesigns( valueObjects );
        }
        int count = valueObjects.size();

        arrayDesignReportService.fillInValueObjects( valueObjects );
        arrayDesignReportService.fillEventInformation( valueObjects );
        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );

        return new JsonReaderResponse<>( new ArrayList<>( valueObjects ), count );
    }

    @Override
    @RequestMapping("/deleteArrayDesign.html")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String stringId = request.getParameter( "id" );

        if ( stringId == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide an id" );
        }

        Long id;
        try {
            id = Long.parseLong( stringId );
        } catch ( NumberFormatException e ) {
            throw new EntityNotFoundException( "Identifier was invalid" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( "Platform with id=" + id + " not found" );
        }

        // check that no EE depend on the arraydesign we want to remove
        // Do this by checking if there are any bioassays that depend this AD
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( arrayDesign );
        if ( assays.size() != 0 ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "Array  " + arrayDesign.getName()
                            + " can't be deleted. Dataset has a dependency on this Array." );
        }

        String taskId = taskRunningService.submitLocalTask( new TaskCommand( arrayDesign.getId() ) );

        return new ModelAndView().addObject( "taskId", taskId );

    }

    @Override
    @RequestMapping(value = "/downloadAnnotationFile.html", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ModelAndView downloadAnnotationFile( HttpServletRequest request, HttpServletResponse response ) {

        String arrayDesignIdStr = request.getParameter( "id" );
        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a platform name or Id" );
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
        String fileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( arrayDesign.getShortName() );
        String fileName = fileBaseName + fileType + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;

        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );

        if ( !f.exists() || !f.canRead() ) {
            try {
                // Experimental. Ideally make a background process. But usually these files should be available anyway...
                log.info( "Annotation file not found, creating for " + arrayDesign );
                annotationFileService.create( arrayDesign, true );
                f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
                if ( !f.exists() || !f.canRead() ) {
                    throw new IOException( "Created but could not read?" );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                throw new RuntimeException(
                        "The file could not be found and could not be created for " + arrayDesign.getShortName() + " ("
                                + e.getMessage() + "). " + "Please contact " + SUPPORT_EMAIL + " for assistance" );
            }
        }

        try ( InputStream reader = new BufferedInputStream( new FileInputStream( f ) ) ) {

            response.setHeader( "Content-disposition", "attachment; filename=" + fileName );
            response.setContentLength( ( int ) f.length() );
            // response.setContentType( "application/x-gzip" ); // see Bug4206

            try ( OutputStream outputStream = response.getOutputStream() ) {

                byte[] buf = new byte[1024];
                int len;
                while ( ( len = reader.read( buf ) ) > 0 ) {
                    outputStream.write( buf, 0, len );
                }
                reader.close();

            } catch ( IOException ioe ) {
                log.warn( "Failure during streaming of annotation file " + fileName + " Error: " + ioe );
            }
        } catch ( FileNotFoundException e ) {
            log.warn( "Annotation file " + fileName + " can't be found at " + e );
            return null;
        } catch ( IOException e ) {
            log.warn( "Annotation file " + fileName + " could not be read: " + e.getMessage() );
            return null;
        }
        return null;
    }

    @Override
    @RequestMapping("/filterArrayDesigns.html")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "No search criteria provided" );
        }

        List<SearchResult<?>> searchResults = null;
        try {
            searchResults = searchService.search( SearchSettings.arrayDesignSearch( filter ) )
                    .get( ArrayDesign.class );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }

        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "No search criteria provided" );

        }

        StringBuilder list = new StringBuilder();

        if ( searchResults.size() == 1 ) {
            ArrayDesign arrayDesign = arrayDesignService.load( searchResults.iterator().next().getResultId() );
            return new ModelAndView(
                    new RedirectView( "/arrays/showArrayDesign.html?id=" + arrayDesign.getId(), true ) )
                    .addObject( "message",
                            "Matched one : " + arrayDesign.getName() + "(" + arrayDesign.getShortName() + ")" );
        }

        for ( SearchResult ad : searchResults ) {
            list.append( ad.getResultId() ).append( "," );
        }

        overallWatch.stop();
        Long overallElapsed = overallWatch.getTime();
        log.info( "Generating the AD list:  (" + list + ") took: " + overallElapsed / 1000 + "s " );

        return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html?id=" + list, true ) )
                .addObject( "message", searchResults.size() + " Platforms matched your search." );

    }

    @Override
    @RequestMapping("/generateArrayDesignSummary.html")
    public ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );

        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        GenerateArraySummaryLocalTask job;
        if ( StringUtils.isBlank( sId ) ) {
            job = new GenerateArraySummaryLocalTask( new TaskCommand() );
            String taskId = taskRunningService.submitLocalTask( job );
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "taskId", taskId );
        }

        try {
            Long id = Long.parseLong( sId );
            job = new GenerateArraySummaryLocalTask( new TaskCommand( id ) );
            String taskId = taskRunningService.submitLocalTask( job );
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html?id=" + sId, true ) )
                    .addObject( "taskId", taskId );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Invalid ID: " + sId );
        }

    }

    @Override
    public Collection<ArrayDesignValueObject> getArrayDesigns( Long[] arrayDesignIds, boolean showMergees,
            boolean showOrphans ) {
        List<ArrayDesignValueObject> result = new ArrayList<>();

        // If no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( arrayDesignIds == null || arrayDesignIds.length == 0 ) {
            result.addAll( arrayDesignService.loadAllValueObjects() );

        } else {// if ids are specified, then display only those arrayDesigns

            Collection<Long> adCol = new LinkedList<>( Arrays.asList( arrayDesignIds ) );
            result.addAll( arrayDesignService.loadValueObjectsByIds( adCol ) );
        }

        // Filter...
        Collection<ArrayDesignValueObject> toHide = new HashSet<>();
        for ( ArrayDesignValueObject a : result ) {
            if ( !showMergees && a.getIsMergee() && a.getExpressionExperimentCount() == 0 ) {
                toHide.add( a );
            }
            if ( !showOrphans && ( a.getExpressionExperimentCount() == null
                    || a.getExpressionExperimentCount() == 0 ) ) {
                toHide.add( a );
            }
        }
        result.removeAll( toHide );

        Collections.sort( result );

        return result;
    }

    @Override
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        return this.getDesignSummaries( arrayDesign );
    }

    @Override
    public Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign ) {
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, NUM_PROBES_TO_SHOW );
        if ( rawSummaries == null ) {
            return new HashSet<>();
        }
        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    @Override
    public ArrayDesignValueObjectExt getDetails( Long id ) {

        ArrayDesign arrayDesign = this.getADSafely( id );
        log.info( "Loading details of " + arrayDesign );

        ArrayDesignValueObject vo = arrayDesignService.loadValueObject( arrayDesign );
        if ( vo == null ) {
            throw new IllegalArgumentException(
                    "You do not have appropriate rights to see this platform. This is likely due "
                            + "to the platform being marked as unusable." );
        }
        arrayDesignReportService.fillInValueObjects( Collections.singleton( vo ) );
        arrayDesignReportService.fillInSubsumptionInfo( Collections.singleton( vo ) );

        ArrayDesignValueObjectExt result = new ArrayDesignValueObjectExt( vo );
        result = this.setExtRefsAndCounts( result, arrayDesign );
        result = this.setAlternateNames( result, arrayDesign );
        result = this.setExtRefsAndCounts( result, arrayDesign );
        result = this.setSummaryInfo( result, id );
        result.setSwitchedExpressionExperimentCount( arrayDesignService.getSwitchedExperimentIds( arrayDesign ).size() );

        populateMergeStatus( arrayDesign, result ); // SLOW if we follow down to mergees of mergees etc.

        if ( result.getIsAffymetrixAltCdf() )
            result.setAlternative( new ArrayDesignValueObject( arrayDesign.getAlternativeTo() ) );

        log.info( "Finished loading details of " + arrayDesign );
        return result;
    }

    /**
     * Loads, checks not null, and thaws the Array Design with given ID;
     */
    private ArrayDesign getADSafely( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ID cannot be null" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );

        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "No platform with id=" + id + " could be loaded" );
        }

        arrayDesign = arrayDesignService.thawLite( arrayDesign );
        return arrayDesign;
    }

    /**
     * Set alternate names on the given value object.
     */
    private ArrayDesignValueObjectExt setAlternateNames( ArrayDesignValueObjectExt result, ArrayDesign arrayDesign ) {
        Collection<String> names = new HashSet<>();
        for ( AlternateName an : arrayDesign.getAlternateNames() ) {
            names.add( an.getName() );
        }
        result.setAlternateNames( names );
        return result;
    }

    /**
     * Sets external references, design element count and express. experiment count on the given value object.
     */
    private ArrayDesignValueObjectExt setExtRefsAndCounts( ArrayDesignValueObjectExt result, ArrayDesign arrayDesign ) {
        Integer numCompositeSequences = arrayDesignService.getCompositeSequenceCount( arrayDesign ).intValue();

        int numExpressionExperiments = arrayDesignService.numExperiments( arrayDesign );

        Collection<DatabaseEntryValueObject> externalReferences = new HashSet<>();
        for ( DatabaseEntry en : arrayDesign.getExternalReferences() ) {
            externalReferences.add( new DatabaseEntryValueObject( en ) );
        }
        result.setExternalReferences( externalReferences );
        result.setDesignElementCount( numCompositeSequences );
        result.setExpressionExperimentCount( numExpressionExperiments );
        return result;
    }

    /**
     * Sets the summary info on the given value object.
     */
    private ArrayDesignValueObjectExt setSummaryInfo( ArrayDesignValueObjectExt result, Long id ) {
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( id );
        if ( summary != null ) {
            result.setNumProbeAlignments( summary.getNumProbeAlignments() );
            result.setNumProbesToGenes( summary.getNumProbesToGenes() );
            result.setNumProbeSequences( summary.getNumProbeSequences() );
        }
        return result;
    }

    @Override
    public Map<String, String> getReportHtml( EntityDelegator ed ) {
        assert ed.getId() != null;
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( ed.getId() );
        Map<String, String> result = new HashMap<>();

        result.put( "id", ed.getId().toString() );
        if ( summary == null )
            result.put( "html", "Not available" );
        else
            result.put( "html", ArrayDesignHtmlUtil.getSummaryHtml( summary ) );
        return result;
    }

    @Override
    public String getSummaryForArrayDesign( Long id ) {

        Collection<Long> ids = new ArrayList<>();
        ids.add( id );
        Collection<ArrayDesignValueObject> adVos = arrayDesignService.loadValueObjectsByIds( ids );
        arrayDesignReportService.fillInValueObjects( adVos );

        if ( !adVos.isEmpty() && adVos.toArray()[0] != null ) {
            ArrayDesignValueObject advo = ( ArrayDesignValueObject ) adVos.toArray()[0];
            StringBuilder buf = new StringBuilder();

            buf.append( "<div style=\"float:left\" >" );

            if ( advo.getNumProbeAlignments() != null ) {
                buf.append( ArrayDesignHtmlUtil.getSummaryHtml( advo ) );
            } else {
                buf.append( "[Not avail.]" );
            }

            buf.append( "</div>" );
            return buf.toString();
        }
        return "[Not avail.]";
    }

    @Override
    public Collection<ArrayDesignValueObject> loadArrayDesignsForShowAll( Long[] arrayDesignIds ) {

        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( arrayDesignIds, true, true );

        if ( SecurityUtil.isUserAdmin() ) {
            arrayDesignReportService.fillEventInformation( valueObjects );
        } else {
            auditableUtil.removeTroubledArrayDesigns( valueObjects );
        }

        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );
        arrayDesignReportService.fillInValueObjects( valueObjects );

        return valueObjects;
    }

    @Override
    public ArrayDesignValueObject loadArrayDesignsSummary() {

        return arrayDesignReportService.getSummaryObject();
    }

    @Override
    public String remove( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( ed.getId() + " not found" );
        }
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( arrayDesign );
        if ( assays.size() != 0 ) {
            throw new IllegalArgumentException(
                    "Cannot remove " + arrayDesign + ", it is used by an expression experiment" );
        }

        RemoveArrayLocalTask job = new RemoveArrayLocalTask( new TaskCommand( arrayDesign.getId() ) );

        return taskRunningService.submitLocalTask( job );

    }

    @Override
    @RequestMapping("/showAllArrayDesigns.html")
    public ModelAndView showAllArrayDesigns( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "arrayDesigns" );
    }

    @Override
    @RequestMapping({ "/showArrayDesign.html", "/" })
    public ModelAndView showArrayDesign( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );
        String idStr = request.getParameter( "id" );

        if ( ( name == null ) && ( idStr == null ) ) {
            throw new IllegalArgumentException( "Must provide a platform identifier or name" );
        }

        ArrayDesign arrayDesign;
        if ( idStr != null ) {
            arrayDesign = arrayDesignService.load( Long.parseLong( idStr ) );
            request.setAttribute( "id", idStr );
        } else {
            arrayDesign = arrayDesignService.findByShortName( name );
            request.setAttribute( "name", name );

            if ( arrayDesign == null ) {
                Collection<ArrayDesign> byName = arrayDesignService.findByName( name );
                if ( byName.isEmpty() ) {
                    throw new IllegalArgumentException( "Must provide a valid platform identifier or name" );

                }
                arrayDesign = byName.iterator().next();
            }
        }

        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Must provide a valid platform identifier or name" );
        }

        long id = arrayDesign.getId();

        ModelAndView mav = new ModelAndView( "arrayDesign.detail" );

        mav.addObject( "arrayDesignId", id );
        mav.addObject( "arrayDesignShortName", arrayDesign.getShortName() );
        mav.addObject( "arrayDesignName", arrayDesign.getName() );

        return mav;
    }

    @Override
    @RequestMapping("/showCompositeSequenceSummary.html")
    public ModelAndView showCompositeSequences( HttpServletRequest request ) {

        String arrayDesignIdStr = request.getParameter( "id" );

        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a platform name or Id" );
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

    @Override
    @RequestMapping("/showExpressionExperiments.html")
    public ModelAndView showExpressionExperiments( HttpServletRequest request ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "Platform with id=" + id + " not found" );
        }
        // seems inefficient? but need security filtering.
        Collection<ExpressionExperiment> ees = arrayDesignService.getExpressionExperiments( arrayDesign );

        String ids = StringUtils.join( EntityUtils.getIds( ees ).toArray(), "," );
        return new ModelAndView(
                new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html?id=" + ids, true ) );
    }

    @Override
    public String updateReport( EntityDelegator ed ) {
        GenerateArraySummaryLocalTask job = new GenerateArraySummaryLocalTask( new TaskCommand( ed.getId() ) );
        return taskRunningService.submitLocalTask( job );
    }

    @Override
    public String updateReportById( Long id ) {
        GenerateArraySummaryLocalTask job = new GenerateArraySummaryLocalTask( new TaskCommand( id ) );
        return taskRunningService.submitLocalTask( job );
    }

    private String formatAlternateNames( ArrayDesign ad ) {
        Collection<String> names = new HashSet<>();
        for ( AlternateName an : ad.getAlternateNames() ) {
            names.add( an.getName() );
        }
        return StringUtils.join( names, "; " );
    }

    /**
     * Recursively populate the status. Recursion only goes 'up' - so the subsumer and merger, not the subsumees and
     * mergees.
     */
    private void populateMergeStatus( ArrayDesign arrayDesign, ArrayDesignValueObjectExt result ) {
        assert arrayDesign != null;
        assert result != null;

        Collection<ArrayDesign> subsumees = arrayDesign.getSubsumedArrayDesigns();
        subsumees = arrayDesignService.thawLite( subsumees );
        ArrayDesign subsumer = arrayDesign.getSubsumingArrayDesign();
        Collection<ArrayDesign> mergees = arrayDesign.getMergees();
        mergees = arrayDesignService.thawLite( mergees );
        ArrayDesign merger = arrayDesign.getMergedInto();

        if ( subsumees != null && !subsumees.isEmpty() ) {
            Collection<ArrayDesignValueObject> subsumeesVos = ArrayDesignValueObject.create( subsumees );
            result.setSubsumees( subsumeesVos );
        }
        if ( subsumer != null ) {
            ArrayDesignValueObjectExt subsumerVo = new ArrayDesignValueObjectExt(
                    new ArrayDesignValueObject( subsumer ) );
            result.setSubsumer( subsumerVo );
            // subsumer = arrayDesignService.thawLite( subsumer );
            // populateMergeStatus( subsumer, subsumerVo );
        }
        if ( mergees != null && !mergees.isEmpty() ) {
            Collection<ArrayDesignValueObject> mergeesVos = ArrayDesignValueObject.create( mergees );
            result.setMergees( mergeesVos );
        }
        if ( merger != null ) {
            ArrayDesignValueObjectExt mergerVo = new ArrayDesignValueObjectExt( new ArrayDesignValueObject( merger ) );
            result.setMerger( mergerVo );
            // merger = arrayDesignService.thawLite( merger );
            // populateMergeStatus( merger, mergerVo );
        }

    }

    /**
     * Inner class used for building array design summary
     */
    class GenerateArraySummaryLocalTask extends AbstractTask<TaskResult, TaskCommand> {

        public GenerateArraySummaryLocalTask( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {

            if ( this.taskCommand.getEntityId() == null ) {
                log.info( "Generating summary for all platforms" );
                arrayDesignReportService.generateArrayDesignReport();
                return new TaskResult( taskCommand,
                        new ModelAndView( new RedirectView( "/arrays/showAllArrayDesignStatistics.html", true ) ) );
            }
            ArrayDesignValueObject report = arrayDesignReportService
                    .generateArrayDesignReport( taskCommand.getEntityId() );
            return new TaskResult( taskCommand, report );

        }
    }

    /**
     * Inner class used for deleting array designs
     */
    class RemoveArrayLocalTask extends AbstractTask<TaskResult, TaskCommand> {

        public RemoveArrayLocalTask( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {
            ArrayDesign ad = arrayDesignService.load( taskCommand.getEntityId() );
            if ( ad == null ) {
                throw new IllegalArgumentException( "Could not load platform with id=" + taskCommand.getEntityId() );
            }
            arrayDesignService.remove( ad );
            return new TaskResult( taskCommand,
                    new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                            .addObject( "message", "Array " + ad.getShortName() + " removed from Database." ) );

        }

    }

}

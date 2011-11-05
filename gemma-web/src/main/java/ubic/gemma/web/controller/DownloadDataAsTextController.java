package ubic.gemma.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.web.controller.diff.DifferentialExpressionSearchController;
import ubic.gemma.web.view.TextView;

/**
 * 
 * 
 * 
 * @author anton
 *
 */
@Controller(value = "downloadDataAsTextController")
@RequestMapping("/downloadText")
public class DownloadDataAsTextController {
    protected static Log log = LogFactory.getLog( DownloadDataAsTextController.class.getName() );

    @Autowired
    private DifferentialExpressionSearchController diffExSearch;
    
    /*
     * Handle case of text export of the results.
     */
    @RequestMapping("/downloadMetaheatmapData.html")
    protected ModelAndView handleRequestInternal( HttpServletRequest request ) throws Exception {

        Long taxonId = Long.parseLong( request.getParameter( "t" ).trim() );
        //String geneSort = request.getParameter( "gs" ).trim();
        //String experimentSort = request.getParameter( "es" ).trim();

//        List<String> factorFilters = extractParamList( request.getParameter( "ff" ) ); // param.ff.split(',')
        List<String> geneSessionGroupQueries = extractParamList( request.getParameter( "gq" ) ); // param.gq.split(',')
        List<String> experimentSessionGroupQueries = extractParamList( request.getParameter( "eq" ) ); // param.eq.split(',')
        
        List<Long> geneIds = extractIds( request.getParameter( "g" ) ); // gene
        List<Long> eeIds = extractIds( request.getParameter( "e" ) ); // experiment
        List<Long> geneGroupIds = extractIds( request.getParameter( "gg" ) ); // gene group
        List<Long> experimentGroupIds = extractIds( request.getParameter( "eg" ) ); //experiment group

        /* TODO REWORK BOOKMARKS THEN UNCOMMENT THIS SECTION
        Collection<Reference> datasetGroupReferences = new LinkedList<Reference> (); 
        Collection<Reference> geneGroupReferences = new LinkedList<Reference> ();

        for (Long geneId : geneIds) {
            geneGroupReferences.add( new GeneReference( geneId ) );
        }

        for (Long eeId : eeIds) {
            datasetGroupReferences.add( new ExpressionExperimentReference( eeId ) );
        }
       
        for (Long geneGroupId : geneGroupIds) {
            geneGroupReferences.add( new DatabaseBackedGeneSetReference( geneGroupId ) );
        }

        for (Long experimentGroupId : experimentGroupIds) {
            datasetGroupReferences.add( new DatabaseBackedExpressionExperimentSetReference( experimentGroupId ) );
        }

        DifferentialExpressionVisualizationValueObject searchResult = diffExSearch.differentialExpressionAnalysisVisualizationSearch(taxonId, datasetGroupReferences, geneGroupReferences,
                geneSessionGroupQueries, experimentSessionGroupQueries );
        
        String text = searchResult.toTextFile();
        
        // Convert result to text        
        ModelAndView mav = new ModelAndView( new TextView() );
        mav.addObject( "text", text );
        */
        ModelAndView mav = new ModelAndView( new TextView() );
        return mav;

    }
    
    private List<String> extractParamList( String paramString ) {
        List<String> paramList = new ArrayList<String>();
        if ( paramString != null ) {
            for ( String s : paramString.split( "," ) ) {
                paramList.add( s.trim() );
            }
        }
        return paramList;
    }

    protected List<Long> extractIds( String idString ) {
        List<Long> ids = new ArrayList<Long>();
        if ( idString != null ) {
            for ( String s : idString.split( "," ) ) {
                try {
                    ids.add( Long.parseLong( s.trim() ) );
                } catch ( NumberFormatException e ) {
                    log.warn( "invalid id " + s );
                }
            }
        }
        return ids;
    }

    
}

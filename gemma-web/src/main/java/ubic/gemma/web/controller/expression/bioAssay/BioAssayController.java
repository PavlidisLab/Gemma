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
package ubic.gemma.web.controller.expression.bioAssay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.preprocess.OutlierDetails;
import ubic.gemma.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.analysis.expression.BioAssayOutlierProcessingTaskCommand;
import ubic.gemma.web.controller.WebConstants;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/bioAssay")
public class BioAssayController {

    private static final String identifierNotFound = "You must provide a valid BioAssay identifier";
    private static final Log log = LogFactory.getLog( BioAssayController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    /**
     * @param eeId
     * @return
     */
    public Collection<BioAssayValueObject> getBioAssays( Long eeId ) {
        ExpressionExperiment ee = eeService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with ID=" + eeId );
        }

        ee = eeService.thawLite( ee );
        Collection<BioAssayValueObject> result = new HashSet<>();

        for ( BioAssay assay : ee.getBioAssays() ) {

            BioAssayValueObject bioAssayValueObject = new BioAssayValueObject( assay );

            result.add( bioAssayValueObject );
        }

        log.debug( "Loaded " + result.size() + " bioassays for experiment ID=" + eeId );

        return result;
    }

    /**
     * @param eeId
     * @return
     */
    public Collection<BioAssayValueObject> getIdentifiedOutliers( Long eeId ) {
        if ( eeId == null ) {
            log.warn( "No id!" );
            return null;
        }

        ExpressionExperiment ee = eeService.load( eeId );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + eeId );
            return null;
        }
        ee = eeService.thawLite( ee );
        Collection<BioAssayValueObject> result = new HashSet<>();
        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = sampleCoexpressionMatrixService.findOrCreate( ee );
        Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliers( ee, sampleCorrelationMatrix );

        for ( OutlierDetails details : outliers ) {
            BioAssay bioAssay = details.getBioAssay();
            bioAssayService.thaw( bioAssay );
            BioAssayValueObject bioAssayValueObject = new BioAssayValueObject( bioAssay );
            result.add( bioAssayValueObject );
        }

        log.debug( "Loaded " + result.size() + " bioassays for experiment ID=" + eeId );

        return result;
    }

    /**
     * AJAX
     * 
     * @param id
     * @return
     */
    public String markOutlier( Collection<Long> ids ) {
        return taskRunningService.submitLocalTask( new BioAssayOutlierProcessingTaskCommand( ids ) );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = { "/showBioAssay.html", "/" })
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = null;

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            return new ModelAndView( WebConstants.HOME_PAGE ).addObject( "message", identifierNotFound );
        }

        if ( id == null ) {
            return new ModelAndView( WebConstants.HOME_PAGE ).addObject( "message", identifierNotFound );
        }

        BioAssay bioAssay = bioAssayService.load( id );
        if ( bioAssay == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        bioAssayService.thaw( bioAssay );

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssay.detail" ).addObject( "bioAssay", new BioAssayValueObject( bioAssay ) );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showAllBioAssays.html")
    public ModelAndView showAllBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        String sId = request.getParameter( "id" );
        Collection<BioAssay> bioAssays = new ArrayList<>();
        if ( StringUtils.isBlank( sId ) ) {
            /*
             * Probably not desirable ... there are >70,000 of them
             */
            bioAssays = bioAssayService.loadAll();
        } else {
            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                BioAssay bioAssay = bioAssayService.load( id );
                if ( bioAssay == null ) {
                    throw new EntityNotFoundException( id + " not found" );
                }
                bioAssayService.thaw( bioAssay );
                bioAssays.add( bioAssay );
            }
        }
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", bioAssays );
    }

    /**
     * AJAX
     * 
     * @param id
     * @return
     */
    public String unmarkOutlier( Collection<Long> ids ) {
        return taskRunningService.submitLocalTask( new BioAssayOutlierProcessingTaskCommand( ids, true ) );
    }

}

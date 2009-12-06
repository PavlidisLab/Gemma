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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/bioAssay")
public class BioAssayController extends BackgroundProcessingMultiActionController {

    /**
     * Inner class used for switching bioassays to be missing values.
     */
    class RemoveBioAssayJob extends BackgroundControllerJob<ModelAndView> {

        private BioAssay bioAssay;

        public RemoveBioAssayJob( BioAssay bioAssay ) {
            super();
            this.bioAssay = bioAssay;
        }

        public ModelAndView call() throws Exception {

            ProgressJob job = init( "Marking BioAssay: " + bioAssay.getName() + " as missing data" );provideAuthentication();

            sampleRemoveService.markAsMissing( this.bioAssay );
            saveMessage( "BioAssay  " + bioAssay.getName() + " marked as missing data." );
            bioAssay = null;

            ProgressManager.destroyProgressJob( job, true );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );

        }
    }

    private static Log log = LogFactory.getLog( BioAssayController.class.getName() );

    @Autowired
    private BioAssayService bioAssayService = null;

    private final String identifierNotFound = "You must provide a valid BioAssay identifier";

    @Autowired
    private SampleRemoveService sampleRemoveService;

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("markBioAssayOutlier.html")
    public ModelAndView markBioAssayOutlier( HttpServletRequest request, HttpServletResponse response ) {
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

        BioAssay bioAssay = bioAssayService.load( id );
        if ( bioAssay == null ) {
            throw new EntityNotFoundException( "BioAssay with id=" + id + " not found" );
        }
        return startJob( new RemoveBioAssayJob( bioAssay ) );
    }

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    public void setSampleRemoveService( SampleRemoveService sampleRemoveService ) {
        this.sampleRemoveService = sampleRemoveService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping(value = { "/showBioAssay.html" })
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = null;

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            saveMessage( request, identifierNotFound );
            return new ModelAndView( "mainMenu.html" );
        }

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            saveMessage( request, identifierNotFound );
            return new ModelAndView( "mainMenu.html" );
        }

        BioAssay bioAssay = bioAssayService.load( id );
        if ( bioAssay == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssay.detail" ).addObject( "bioAssay", bioAssay );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showAllBioAssays.html")
    public ModelAndView showAllBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        String sId = request.getParameter( "id" );
        Collection<BioAssay> bioAssays = new ArrayList<BioAssay>();
        if ( sId == null ) {
            bioAssays = bioAssayService.loadAll();
        } else {
            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                BioAssay bioAssay = bioAssayService.load( id );
                if ( bioAssay == null ) {
                    throw new EntityNotFoundException( id + " not found" );
                }
                bioAssays.add( bioAssay );
            }
        }
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", bioAssays );
    }
}

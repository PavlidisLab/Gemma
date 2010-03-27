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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/bioAssay")
public class BioAssayController extends AbstractTaskService {

    private class RemoveBioAssayJob extends BackgroundJob<TaskCommand> {

        public RemoveBioAssayJob( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            BioAssay bioAssay = bioAssayService.load( command.getEntityId() );
            if ( bioAssay == null ) {
                throw new EntityNotFoundException( "BioAssay with id=" + command.getEntityId() + " not found" );
            }
            sampleRemoveService.markAsMissing( bioAssay );
            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/arrays/showAllArrayDesigns.html" ) ) );
        }
    }

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

        RemoveBioAssayJob job = new RemoveBioAssayJob( new TaskCommand( id ) );
        super.startTask( job );
        return new ModelAndView( new RedirectView( "/Gemma" ) ).addObject( "taskId", job.getTaskId() );
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
    @RequestMapping("/showBioAssay.html")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = null;

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            return new ModelAndView( "mainMenu.html" ).addObject( "message", identifierNotFound );
        }

        if ( id == null ) {
            return new ModelAndView( "mainMenu.html" ).addObject( "message", identifierNotFound );
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

    @Override
    protected BackgroundJob<TaskCommand> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
    }
}

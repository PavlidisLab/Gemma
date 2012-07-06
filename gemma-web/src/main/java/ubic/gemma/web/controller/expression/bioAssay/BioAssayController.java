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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.analysis.expression.BioAssayOutlierProcessingTask;
import ubic.gemma.web.controller.WebConstants;
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
            return bioAssayOutlierProcessingTask.execute( command );
        }
    }

    private class RemoveBioAssaySpaceJob extends BackgroundJob<TaskCommand> {
        final BioAssayOutlierProcessingTask taskProxy = ( BioAssayOutlierProcessingTask ) getProxy();

        public RemoveBioAssaySpaceJob( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }
    }

    private static final String identifierNotFound = "You must provide a valid BioAssay identifier";

    @Autowired
    private BioAssayOutlierProcessingTask bioAssayOutlierProcessingTask;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private AuditEventService auditEventService;

    /**
     * AJAX
     * 
     * @param id
     * @return
     */
    public String markOutlier( Long id ) {
        RemoveBioAssayJob job = new RemoveBioAssayJob( new TaskCommand( id ) );
        return super.startTask( job );
    }

    /**
     * @param request
     * @param response
     * @param errors
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
     * @param eeId
     * @return
     */
    public Collection<BioAssayValueObject> getBioAssays( Long eeId ) {
        ExpressionExperiment ee = eeService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with ID=" + eeId );
        }

        ee = eeService.thawLite( ee );
        Collection<BioAssayValueObject> result = new HashSet<BioAssayValueObject>();
        for ( BioAssay assay : ee.getBioAssays() ) {

            BioAssayValueObject bioAssayValueObject = new BioAssayValueObject( assay );

            bioAssayValueObject.setOutlier( auditEventService.hasEvent( assay, SampleRemovalEvent.class ) );

            result.add( bioAssayValueObject );
        }

        log.info( "Loaded " + result.size() + " bioassays for experiment ID=" + eeId );

        return result;
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

    @Override
    protected BackgroundJob<TaskCommand> getInProcessRunner( TaskCommand command ) {
        return new RemoveBioAssayJob( command );
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return new RemoveBioAssaySpaceJob( command );
    }
}

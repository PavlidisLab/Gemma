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
package ubic.gemma.web.controller.compass;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.compass.spring.web.mvc.CompassIndexResults;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.index.IndexGemmaTask;
import ubic.gemma.grid.javaspaces.index.SpacesIndexGemmaCommand;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.gemmaspaces.AbstractSpacesFormController;
import ubic.gemma.web.util.MessageUtil;

/**
 * A general Spring's MVC Controller that perform the index operation of <code>CompassGps</code>. The indexing here
 * rebuilds the database. That is, the index is deleted, then rebuilt (at which time the indicies exist but are empty),
 * and then updated.
 * <p>
 * Will perform the index operation if the {@link org.compass.spring.web.mvc.CompassIndexCommand} <code>doIndex</code>
 * property is set to true.
 * <p>
 * The controller has two views to be set, the <code>indexView</code>, which is the view that holds the screen which
 * the user will initiate the index operation, and the <code>indexResultsView</code>, which will show the results of
 * the index operation.
 * <p>
 * The results of the index operation will be saved under the <code>indexResultsName</code>, which defaults to
 * "indexResults".
 * 
 * @author keshav, klc	
 * @version $Id$
 * @spring.bean id="indexController"
 * @spring.property name="formView" value="indexer"
 * @spring.property name="successView" value="indexer"
 * @spring.property name="arrayGps" ref="arrayGps"
 * @spring.property name="expressionGps" ref="expressionGps"
 * @spring.property name="geneGps" ref="geneGps" 
 * @spring.property name="bibliographicGps" ref="bibliographicGps"
 * @spring.property name="probeGps" ref="probeGps"
 * @spring.property name="biosequenceGps" ref="biosequenceGps"
 * 
 */
public class CustomCompassIndexController extends AbstractSpacesFormController {

    private Log log = LogFactory.getLog( CustomCompassIndexController.class );

    private CompassGpsInterfaceDevice expressionGps;
    private CompassGpsInterfaceDevice arrayGps;
    private CompassGpsInterfaceDevice geneGps;
    private CompassGpsInterfaceDevice bibliographicGps;
    private CompassGpsInterfaceDevice probeGps;
    private CompassGpsInterfaceDevice biosequenceGps;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

    	//TODO: Make use of command object (so that indexing can be batched)
    	//Kind of mute case using ajax now
    	
    	
    	 IndexGemmaCommand indexCommand = new IndexGemmaCommand();

        if ( StringUtils.hasText( request.getParameter( "geneIndex" ) ) ) {
            indexCommand.setIndexGene(true);
        } else if ( StringUtils.hasText( request.getParameter( "eeIndex" ) ) ) {
           indexCommand.setIndexEE(true);
        } else if ( StringUtils.hasText( request.getParameter( "arrayIndex" ) ) ) {
            indexCommand.setIndexArray(true);
        } else if ( StringUtils.hasText( request.getParameter( "bibliographicIndex" ) ) ) {
           indexCommand.setIndexBibliographic(true);
        } else if ( StringUtils.hasText( request.getParameter( "biosequenceIndex" ) ) ) {
           indexCommand.setIndexBibliographic(true);
        }
        else
            return new ModelAndView( this.getFormView() );
    	
    	
    	return startJob( indexCommand, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), IndexGemmaTask.class.getName(),
                true );

    }

    
    /**
     * Main entry point for AJAX calls.
     * 
     * @param IndexGemmaCommand
     * @return
     */
    public String run( IndexGemmaCommand command ) {
        return run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), IndexGemmaTask.class.getName(), true );
    }
    
    
    
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        if ( request.getParameter( "cancel" ) != null ) {
            this.saveMessage( request, "Cancelled Index" );
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "geneIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "eeIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "arrayIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "bibliographicIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }
        if ( request.getParameter( "probeIndex" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }

        return super.showForm( request, response, errors );
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
    
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            Object command, MessageUtil messenger ) {

        return new IndexJob( taskId, securityContext, command, messenger );
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.javaspaces.gigaspaces.AbstractGigaSpacesFormController#getSpaceRunner(java.lang.String,
     *      org.acegisecurity.context.SecurityContext, javax.servlet.http.HttpServletRequest, java.lang.Object,
     *      ubic.gemma.web.util.MessageUtil)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String taskId, SecurityContext securityContext,
            Object command, MessageUtil messenger ) {
        return new LoadInSpaceJob( taskId, securityContext, command, messenger );
    }
    
    
    /**
     * Job that loads in a javaspace.
     * 
     * @author Paul
     * @version $Id$
     */
    private class LoadInSpaceJob extends IndexJob {

        final IndexGemmaTask indexGemmaTaskProxy = ( IndexGemmaTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public LoadInSpaceJob( String taskId, SecurityContext parentSecurityContext, Object commandObj,
                MessageUtil messenger ) {
            super( taskId, parentSecurityContext, commandObj, messenger );

        }

        @Override
        protected void index(IndexGemmaCommand indexGemmaCommand){
        	
        	 SpacesIndexGemmaCommand jsCommand = createCommandObject( indexGemmaCommand );
             SpacesResult result = indexGemmaTaskProxy.execute( jsCommand );
             //return result;
        }
      


        private SpacesIndexGemmaCommand createCommandObject( IndexGemmaCommand ic ) {
            return new SpacesIndexGemmaCommand( taskId, true, ic.isIndexArray(), ic.isIndexEE(), ic.isIndexGene()
            		, ic.isIndexProbe(),  ic.isIndexBibliographic(), ic.isIndexBioSequence());
        }

    }
    

    /**
     * @author klc
     * @version $Id$ This inner class is used
     *          for creating a seperate thread that will delete the compass ee index
     */
    class IndexJob extends BackgroundControllerJob<ModelAndView> {

        private String description;

        
        /**
         * @param taskId
         * @param parentSecurityContext
         * @param commandObj
         * @param messenger
         */
        public IndexJob( String taskId, SecurityContext parentSecurityContext, Object commandObj, MessageUtil messenger ) {
            super( taskId, parentSecurityContext, commandObj, messenger );

        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Attempting to index" );

            long time = System.currentTimeMillis();

            job.updateProgress( "Preparing to rebuild selected indexes ");
            log.info( "Preparing to rebuild selected indexes" );
            
            IndexGemmaCommand indexGemmaCommand = ( ( IndexGemmaCommand ) command );
   
            
            index(indexGemmaCommand);

            time = System.currentTimeMillis() - time;
            CompassIndexResults indexResults = new CompassIndexResults( time );
            Map<Object, Object> data = new HashMap<Object, Object>();
            data.put( "indexResults", indexResults );

            ProgressManager.destroyProgressJob( job );

            ModelAndView mv = new ModelAndView( "indexer" );
            mv.addObject( "time", time );
            mv.addObject( "description", this.description );

            return mv;

        }
        
        protected void index(IndexGemmaCommand command){

        	if (command.isIndexArray())
        		CompassUtils.rebuildCompassIndex( arrayGps );
        	else if (command.isIndexBibliographic())
        		CompassUtils.rebuildCompassIndex(bibliographicGps);
        	else if (command.isIndexEE())
        		CompassUtils.rebuildCompassIndex(expressionGps);
        	else if (command.isIndexGene())
        		CompassUtils.rebuildCompassIndex(geneGps);
        	else if (command.isIndexProbe())
        		CompassUtils.rebuildCompassIndex(probeGps);
        	else if (command.isIndexBioSequence())
        		CompassUtils.rebuildCompassIndex(biosequenceGps);
        		
        	
        }
    }


	public void setArrayGps(CompassGpsInterfaceDevice arrayGps) {
		this.arrayGps = arrayGps;
	}


	public void setBibliographicGps(CompassGpsInterfaceDevice bibliographicGps) {
		this.bibliographicGps = bibliographicGps;
	}


	public void setExpressionGps(CompassGpsInterfaceDevice expressionGps) {
		this.expressionGps = expressionGps;
	}


	public void setGeneGps(CompassGpsInterfaceDevice geneGps) {
		this.geneGps = geneGps;
	}

	public void setProbeGps(CompassGpsInterfaceDevice probeGps){
		this.probeGps = probeGps;
		
	}

	public void setBiosequenceGps(CompassGpsInterfaceDevice biosequenceGps){
		this.biosequenceGps = biosequenceGps;
		
	}
}

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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.util.MessageUtil;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignController" name="arrayDesignController"
 * @springproperty name="validator" ref="arrayDesignValidator"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name="methodNameResolver" ref="arrayDesignActions"
 */
public class ArrayDesignController extends BackgroundProcessingMultiActionController {

    private static Log log = LogFactory.getLog( ArrayDesignController.class.getName() );

    private ArrayDesignService arrayDesignService = null;
    private final String messageName = "Array design with name";
    private final String messageId = "Array design with id";

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @SuppressWarnings("unused")
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
            this.addMessage( request, "object.found", new Object[] { messageId, idStr } );
            request.setAttribute( "id", idStr );
        } else if ( name != null ) {
            arrayDesign = arrayDesignService.findArrayDesignByName( name );
            this.addMessage( request, "object.found", new Object[] { messageName, name } );
            request.setAttribute( "name", name );
        }

        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( name + " not found" );
        }
        long id = arrayDesign.getId();
        
        Long numBioSequences = arrayDesignService.numBioSequencesById( id );
        Long numBlatResults = arrayDesignService.numBlatResultsById( id );
        Long numGenes = arrayDesignService.numGenesById( id );
        Collection<ExpressionExperiment> ee = arrayDesignService.getExpressionExperimentsById( id );
        Long numExpressionExperiments = new Long(ee.size());
        
        String[] eeIdList = new String[ee.size()];
        int i = 0;
        for (ExpressionExperiment e : ee) {
            eeIdList[i] = e.getId().toString();
            i++;
        }
        String eeIds = StringUtils.join( eeIdList,",");

        ModelAndView mav =  new ModelAndView( "arrayDesign.detail" );
        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "numBioSequences", numBioSequences );
        mav.addObject( "numBlatResults",numBlatResults);
        mav.addObject( "numGenes", numGenes );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );
        mav.addObject( "expressionExperimentIds", eeIds );       
        return mav;
    }

    /**
     * Disabled for now
     * 
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "arrayDesigns" ).addObject( "arrayDesigns", arrayDesignService.loadAll() );
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
            addMessage( request, "Array Design " + arrayDesign.getName()
                    + " can't be Deleted. ExpressionExperiments depend on it.", new Object[] { messageName,
                    arrayDesign.getName() } );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );
        }

        String taskId = startJob( arrayDesign, request );
        return new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
       

    }

     
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseBackgroundProcessingFormController#getRunner(org.acegisecurity.context.SecurityContext,
     *      java.lang.Object, java.lang.String)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            HttpServletRequest request, Object command, MessageUtil messenger ) {

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, request, command, messenger ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
     
                ArrayDesign ad = (ArrayDesign) command;
                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Deleting Array Design: "
                        + ad.getShortName());
                            
                arrayDesignService.remove( ad );
                saveMessage( "Array Design "+ad.getShortName()  +" removed from Database." );                
                ad = null;


                ProgressManager.destroyProgressJob( job );
                return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html") );
            }
        };
    }

}

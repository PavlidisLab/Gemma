/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.gemmaspaces;

import net.jini.core.lease.Lease;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.gemmaspaces.expression.experiment.ExpressionExperimentReportTaskImpl;
import ubic.gemma.util.gemmaspaces.GemmaSpacesEnum;
import ubic.gemma.util.gemmaspaces.GemmaSpacesJobObserver;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;
import ubic.gemma.util.gemmaspaces.entry.GemmaSpacesProgressEntry;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.controller.BackgroundControllerJob;

import com.j_spaces.core.client.NotifyModifiers;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractGemmaSpacesService {

    protected GemmaSpacesUtil gemmaSpacesUtil = null;

    protected ApplicationContext updatedContext = null;

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGemmaSpacesToApplicationContext() {
        if ( gemmaSpacesUtil == null ) gemmaSpacesUtil = new GemmaSpacesUtil();

        return gemmaSpacesUtil.addGemmaSpacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    protected void startJob( String spaceUrl, String taskName, boolean runInLocalContext ) {
        run( spaceUrl, taskName, runInLocalContext );
    }

    protected String run( String spaceUrl, String taskName, boolean runInLocalContext ) {
        /*
         * all new threads need this to acccess protected resources (like services)
         */
        SecurityContext context = SecurityContextHolder.getContext();

        String taskId = null;

        updatedContext = addGemmaSpacesToApplicationContext();
        BackgroundControllerJob<ModelAndView> job = null;
        if ( updatedContext.containsBean( "gigaspacesTemplate" ) ) {

            taskId = ( String ) ( ( ExpressionExperimentReportTaskImpl ) updatedContext.getBean( "taskBean" ) )
                    .getTaskId();

            if ( !gemmaSpacesUtil.canServiceTask( taskName, spaceUrl ) ) {

                throw new RuntimeException( "No workers are registered to service task "
                        + taskName.getClass().getSimpleName() + " on the compute server at this time." );
            }
            /* register this "spaces client" to receive notifications */
            GemmaSpacesJobObserver javaSpacesJobObserver = new GemmaSpacesJobObserver( taskId );

            GigaSpacesTemplate template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

            template.addNotifyDelegatorListener( javaSpacesJobObserver, new GemmaSpacesProgressEntry(), null, true,
                    Lease.FOREVER, NotifyModifiers.NOTIFY_ALL );

            runRemotely( taskId );
        } else if ( !updatedContext.containsBean( "gigaspacesTemplate" ) && !runInLocalContext ) {
            throw new RuntimeException(
                    "This task must be run on the compute server, but the space is not running. Please try again later" );
        }

        else {
            taskId = TaskRunningService.generateTaskId();
            runLocally( taskId );
        }

        assert taskId != null;

        // taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );
        return taskId;
    }

    public abstract void runLocally( String taskId );

    public abstract void runRemotely( String taskId );

    /**
     * Controllers extending this class must implement this method. The implementation should call
     * injectGigaspacesUtil(GigaSpacesUtil gigaSpacesUtil) to "inject" a spring loaded GigaSpacesUtil into this abstract
     * class.
     * 
     * @param gemmaSpacesUtil
     */
    abstract protected void setGemmaSpacesUtil( GemmaSpacesUtil gigaSpacesUtil );

    /**
     * @param gemmaSpacesUtil
     */
    protected void injectGemmaSpacesUtil( GemmaSpacesUtil gemmaSpacesUtil ) {
        this.gemmaSpacesUtil = gemmaSpacesUtil;
    }

}

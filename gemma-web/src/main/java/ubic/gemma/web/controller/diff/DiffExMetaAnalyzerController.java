/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.diff;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.diff.GeneDiffExMetaAnalysisHelperService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.BaseValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.tasks.analysis.diffex.DiffExMetaAnalyzerTask;
import ubic.gemma.tasks.analysis.diffex.DiffExMetaAnalyzerTaskCommand;
import ubic.gemma.util.ConfigUtils;

/**
 * A controller to analyze result sets either locally or in a space.
 * 
 * @author frances
 * @version $Id$
 */
@Controller
public class DiffExMetaAnalyzerController extends AbstractTaskService {

    /**
     * Local task.
     */
    private class DiffExMetaAnalyzerJob extends BackgroundJob<DiffExMetaAnalyzerTaskCommand> {
        public DiffExMetaAnalyzerJob( DiffExMetaAnalyzerTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return diffExMetaAnalyzerTask.execute( command );
        }
    }

    /**
     * Job that loads in a javaspace.
     */
    private class DiffExMetaAnalyzerSpaceJob extends BackgroundJob<DiffExMetaAnalyzerTaskCommand> {
        final DiffExMetaAnalyzerTask taskProxy = ( DiffExMetaAnalyzerTask ) getProxy();

        public DiffExMetaAnalyzerSpaceJob( DiffExMetaAnalyzerTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }
    }

    @Autowired
    private DiffExMetaAnalyzerTask diffExMetaAnalyzerTask;

    @Autowired
    private GeneDiffExMetaAnalysisHelperService geneDiffExMetaAnalysisHelperService;

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Autowired
    private UserManager userManager;

    private BaseValueObject generateBaseValueObject( Throwable throwable ) {
        final BaseValueObject baseValueObject = new BaseValueObject();
        baseValueObject.setErrorFound(true);

        if ( throwable instanceof AccessDeniedException ) {
            if ( this.userManager.loggedIn() ) {
                baseValueObject.setAccessDenied( true );
            } else {
                baseValueObject.setUserNotLoggedIn( true );
            }
        } else {
            // If type of throwable is not known, log it.
            log.error( throwable.getMessage(), throwable );
        }

        return baseValueObject;
    }

    public DiffExMetaAnalyzerController() {
        this.setBusinessInterface( DiffExMetaAnalyzerTask.class );
    }

    /**
     * @param analysisResultSetIds
     * @return
     */
    public String analyzeResultSets( Collection<Long> analysisResultSetIds ) {
        DiffExMetaAnalyzerTaskCommand cmd = new DiffExMetaAnalyzerTaskCommand( analysisResultSetIds );
        return super.run( cmd );
    }

    /**
     * @param id
     * @return
     */
	public BaseValueObject findDetailMetaAnalysisById(Long id) {
		BaseValueObject baseValueObject = new BaseValueObject();
		
        try {
        	GeneDifferentialExpressionMetaAnalysisDetailValueObject analysisVO = this.geneDiffExMetaAnalysisHelperService.findDetailMetaAnalysisById(id);        	
        	
        	if (analysisVO == null) {
        		baseValueObject.setErrorFound(true);
        		baseValueObject.setObjectAlreadyRemoved(true);
        	} else {
            	baseValueObject.setValueObject(analysisVO);
        	}
        } catch ( Throwable throwable ) {
            baseValueObject = generateBaseValueObject( throwable );
        }
        return baseValueObject;
	}
	
    /**
     * @return
     */
	public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMyMetaAnalyses() {
		return this.geneDiffExMetaAnalysisHelperService.findMyMetaAnalyses();
	}

    /**
     * @param id
     */
	public BaseValueObject removeMetaAnalysis(Long id) {
		BaseValueObject baseValueObject;
		
        try {
            baseValueObject = this.geneDiffExMetaAnalysisService.delete(id);
        } catch ( Throwable throwable ) {
            baseValueObject = generateBaseValueObject( throwable );
        }
        return baseValueObject;
	}

    /**
     * @param analysisResultSetIds
     * @param name
     * @param description
     * @return
     */
    public String saveResultSets( Collection<Long> analysisResultSetIds, String name, String description ) {
        boolean persist = StringUtils.isNotBlank( name );
        DiffExMetaAnalyzerTaskCommand cmd = new DiffExMetaAnalyzerTaskCommand( analysisResultSetIds, name, description,
                persist );
        return super.run( cmd );
    }

    /**
     * Show meta-analysis manager
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = { "/metaAnalysisManager.html" })
    public ModelAndView showMetaAnalysisManager( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "metaAnalysisManager" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getInProcessRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<DiffExMetaAnalyzerTaskCommand> getInProcessRunner( TaskCommand command ) {
        if ( ConfigUtils.getBoolean( "gemma.grid.gridonly.diff" ) ) {
            return null;
        }
        return new DiffExMetaAnalyzerJob( ( DiffExMetaAnalyzerTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.AbstractTaskService#getSpaceRunner(ubic.gemma.job.TaskCommand)
     */
    @Override
    protected BackgroundJob<DiffExMetaAnalyzerTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new DiffExMetaAnalyzerSpaceJob( ( DiffExMetaAnalyzerTaskCommand ) command );

    }
}

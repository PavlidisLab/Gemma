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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisHelperService;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;
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

    public DiffExMetaAnalyzerController() {
        this.setBusinessInterface( DiffExMetaAnalyzerTask.class );
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

    @Autowired
    private DiffExMetaAnalyzerTask diffExMetaAnalyzerTask;

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Autowired
    private GeneDiffExMetaAnalysisHelperService geneDiffExMetaAnalysisHelperService;
    
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
    
	public String analyzeResultSets(Collection<Long> analysisResultSetIds, int resultSetCount) {
		DiffExMetaAnalyzerTaskCommand cmd = new DiffExMetaAnalyzerTaskCommand( analysisResultSetIds, resultSetCount );
		return super.run( cmd ); 
    }
    
	public String saveResultSets(Collection<Long> analysisResultSetIds, String name, String description) {
		DiffExMetaAnalyzerTaskCommand cmd = new DiffExMetaAnalyzerTaskCommand( analysisResultSetIds, name, description );
		return super.run( cmd );
	}

	public GeneDifferentialExpressionMetaAnalysisDetailValueObject getMetaAnalysis(Long id) {
		return this.geneDiffExMetaAnalysisHelperService.getMetaAnalysis(id);
	}
	
	public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> getMyMetaAnalyses() {
		return this.geneDiffExMetaAnalysisHelperService.getMyMetaAnalyses();
	}

	// TODO: should do something if analysis cannot be removed.
	public void removeAnalysis(Long id) {
		this.geneDiffExMetaAnalysisService.delete(id);
	}
	
    @Override
    protected BackgroundJob<DiffExMetaAnalyzerTaskCommand> getInProcessRunner( TaskCommand command ) {
        if ( ConfigUtils.getBoolean( "gemma.grid.gridonly.diff" ) ) {
            return null;
        }
        return new DiffExMetaAnalyzerJob( ( DiffExMetaAnalyzerTaskCommand ) command );
    }

    @Override
    protected BackgroundJob<DiffExMetaAnalyzerTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new DiffExMetaAnalyzerSpaceJob( ( DiffExMetaAnalyzerTaskCommand ) command );

    }
}

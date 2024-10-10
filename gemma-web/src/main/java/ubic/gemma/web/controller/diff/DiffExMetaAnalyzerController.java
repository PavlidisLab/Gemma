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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.expression.diff.GeneDiffExMetaAnalysisHelperService;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.tasks.analysis.diffex.DiffExMetaAnalyzerTaskCommand;
import ubic.gemma.model.common.BaseValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.GeneDiffExMetaAnalysisService;

import java.util.Collection;

/**
 * A controller to analyze result sets either locally or in a space.
 *
 * @author frances
 */
@Controller
public class DiffExMetaAnalyzerController {
    protected static final Log log = LogFactory.getLog( DiffExMetaAnalyzerController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private GeneDiffExMetaAnalysisHelperService geneDiffExMetaAnalysisHelperService;
    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;
    @Autowired
    private UserManager userManager;

    public String analyzeResultSets( Collection<Long> analysisResultSetIds ) {
        DiffExMetaAnalyzerTaskCommand cmd = new DiffExMetaAnalyzerTaskCommand( analysisResultSetIds );
        return taskRunningService.submitTaskCommand( cmd );
    }

    public BaseValueObject findDetailMetaAnalysisById( Long id ) {
        BaseValueObject baseValueObject = new BaseValueObject();

        try {
            GeneDifferentialExpressionMetaAnalysisDetailValueObject analysisVO = this.geneDiffExMetaAnalysisHelperService
                    .findDetailMetaAnalysisById( id );

            if ( analysisVO == null ) {
                baseValueObject.setErrorFound( true );
                baseValueObject.setObjectAlreadyRemoved( true );
            } else {
                baseValueObject.setValueObject( analysisVO );
            }
        } catch ( Throwable throwable ) {
            log.error( throwable.getMessage(), throwable );
            baseValueObject = generateBaseValueObject( throwable );
        }
        return baseValueObject;
    }

    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> loadAllMetaAnalyses() {
        return this.geneDiffExMetaAnalysisHelperService.loadAllMetaAnalyses();
    }

    public BaseValueObject removeMetaAnalysis( Long id ) {
        BaseValueObject baseValueObject;

        try {
            baseValueObject = this.geneDiffExMetaAnalysisService.delete( id );
        } catch ( Throwable throwable ) {
            log.error( throwable.getMessage(), throwable );
            baseValueObject = generateBaseValueObject( throwable );
        }
        return baseValueObject;
    }

    public String saveResultSets( Collection<Long> analysisResultSetIds, String name, String description ) {
        boolean persist = StringUtils.isNotBlank( name );
        DiffExMetaAnalyzerTaskCommand cmd = new DiffExMetaAnalyzerTaskCommand( analysisResultSetIds, name, description,
                persist );
        return taskRunningService.submitTaskCommand( cmd );
    }

    @RequestMapping(value = { "/metaAnalysisManager.html" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showMetaAnalysisManager() {
        return new ModelAndView( "metaAnalysisManager" );
    }

    private BaseValueObject generateBaseValueObject( Throwable throwable ) {
        final BaseValueObject baseValueObject = new BaseValueObject();
        baseValueObject.setErrorFound( true );

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
}

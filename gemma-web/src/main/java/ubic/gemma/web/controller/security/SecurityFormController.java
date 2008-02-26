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
package ubic.gemma.web.controller.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysisService;
import ubic.gemma.model.analysis.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.ProbeCoexpressionAnalysisService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Manages data-level security (ie. can make data private).
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="securityFormController"
 * @spring.property name = "commandName" value="securityCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.security.SecurityCommand"
 * @spring.property name="securityService" ref="securityService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="probeCoexpressionAnalysisService" ref="probeCoexpressionAnalysisService"
 * @spring.property name="geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 * @spring.property name="differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name="formView" value="securityManager"
 * @spring.property name="successView" value="securityManager"
 */
public class SecurityFormController extends BaseFormController {

    private SecurityService securityService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ArrayDesignService arrayDesignService = null;
    private ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService = null;
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService = null;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    private final String expressionExperimentType = ExpressionExperiment.class.getSimpleName();
    private final String arrayDesignType = ArrayDesign.class.getSimpleName();
    private final String probeCoexpressionAnalysisType = ProbeCoexpressionAnalysis.class.getSimpleName();
    private final String geneCoexpressionAnalysisType = GeneCoexpressionAnalysis.class.getSimpleName();
    private final String differentialExpressionAnalysisType = DifferentialExpressionAnalysis.class.getSimpleName();

    private final String PUBLIC = "Public";
    private final String PRIVATE = "Private";

    public SecurityFormController() {
        /*
         * if true, reuses the same command object across the get-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * Populates drop downs.
     * 
     * @param request
     * @return Map
     */
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        log.debug( "referenceData" );

        Map<String, List<? extends Object>> dropDownMap = new HashMap<String, List<? extends Object>>();

        List<String> securableTypes = new ArrayList<String>();
        securableTypes.add( expressionExperimentType );
        securableTypes.add( arrayDesignType );
        securableTypes.add( probeCoexpressionAnalysisType );
        securableTypes.add( geneCoexpressionAnalysisType );
        securableTypes.add( differentialExpressionAnalysisType );

        dropDownMap.put( "securableTypes", securableTypes );

        return dropDownMap;
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        log.debug( "processFormSubmission" );

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Cancelled" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    @SuppressWarnings("unused")
    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        log.debug( "onSubmit" );

        SecurityCommand sc = ( SecurityCommand ) command;
        String shortName = sc.getShortName();

        if ( StringUtils.isEmpty( shortName ) ) {
            return processErrors( request, response, command, errors,
                    "Must enter the short name of either the experiment or array design. " );
        }

        String type = sc.getSecurableType();

        Object target = null;
        if ( StringUtils.equalsIgnoreCase( type, expressionExperimentType ) ) {
            target = this.expressionExperimentService.findByShortName( shortName );
        } else if ( StringUtils.equalsIgnoreCase( type, probeCoexpressionAnalysisType ) ) {
            ExpressionExperiment investigation = this.expressionExperimentService.findByShortName( shortName );
            target = this.probeCoexpressionAnalysisService.findByInvestigation( investigation );
        } else if ( StringUtils.equalsIgnoreCase( type, geneCoexpressionAnalysisType ) ) {
            ExpressionExperiment investigation = this.expressionExperimentService.findByShortName( shortName );
            target = this.geneCoexpressionAnalysisService.findByInvestigation( investigation );
        } else if ( StringUtils.equalsIgnoreCase( type, differentialExpressionAnalysisType ) ) {
            ExpressionExperiment investigation = this.expressionExperimentService.findByShortName( shortName );
            target = this.differentialExpressionAnalysisService.findByInvestigation( investigation );
        }

        else if ( StringUtils.equalsIgnoreCase( type, arrayDesignType ) ) {
            // target = this.arrayDesignService.findArrayDesignByName( name );//TODO no findById ... maybe use name
            return processErrors( request, response, command, errors,
                    "Cannot change permissions of array designs at this time." );
        }

        if ( target == null ) {
            return processErrors( request, response, command, errors, "No securable object with name " + shortName
                    + " found." );
        }

        String mask = sc.getMask();
        if ( StringUtils.equalsIgnoreCase( mask, PUBLIC ) ) {
            securityService.makePublic( target );
        }

        else if ( StringUtils.equalsIgnoreCase( mask, PRIVATE ) ) {
            securityService.makePrivate( target );
        }

        else
            return processErrors( request, response, command, errors,
                    "Supported masks are 0 (private) and 6 (public), not " + mask );

        saveMessage( request, target + " made " + mask + "." );
        String url = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
                + "/" + getSuccessView() + ".html";

        log.debug( "Redirecting to " + url );
        return new ModelAndView( new RedirectView( url ) );
    }

    /**
     * @param securityService
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param arrayDesignService
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param probeCoexpressionAnalysisService
     */
    public void setProbeCoexpressionAnalysisService( ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService ) {
        this.probeCoexpressionAnalysisService = probeCoexpressionAnalysisService;
    }

    /**
     * @param geneCoexpressionAnalysisService
     */
    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    /**
     * @param differentialExpressionAnalysisService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

}

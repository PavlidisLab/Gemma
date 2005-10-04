package edu.columbia.gemma.web.controller.flow.action.expression.experiment;

import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;

import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GetExpressionExperimentAction extends AbstractAction {

    private ExpressionExperimentService expressionExperimentService;

    /**
     * @return Returns the expressionExperimentService.
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param context
     * @return Event
     */
    protected Event doExecute( RequestContext context ) throws Exception {
        Long id = new Long( ( String ) context.getFlowScope().getRequiredAttribute( "experimentID" ) );
        ExpressionExperiment ex = getExpressionExperimentService().find( id );
        if ( ex != null ) {
            context.getRequestScope().setAttribute( "expressionExperiment", ex );
            return success();
        }
        return error();
    }
}
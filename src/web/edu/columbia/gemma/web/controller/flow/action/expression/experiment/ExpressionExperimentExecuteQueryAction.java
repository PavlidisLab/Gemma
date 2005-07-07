package edu.columbia.gemma.web.controller.flow.action.expression.experiment;

import java.util.Collection;

import org.springframework.web.flow.Event;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.action.AbstractAction;

import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentExecuteQueryAction extends AbstractAction {

    private ExpressionExperimentService expressionExperimentService;

    /**
     * 
     */
    public ExpressionExperimentExecuteQueryAction() {

    }

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
     * This is the equivalent of writing the onSubmit method in a Spring Controller, or a doGet (doPost) method in a
     * Java Servlet.
     * 
     * @param context
     * @return Event
     * @exception Exception
     */
    protected Event doExecute( RequestContext context ) throws Exception {
        Collection col = getExpressionExperimentService().getAllExpressionExperiments();
        context.getRequestScope().setAttribute( "expressionExperiments", col );

        return success();
    }
}
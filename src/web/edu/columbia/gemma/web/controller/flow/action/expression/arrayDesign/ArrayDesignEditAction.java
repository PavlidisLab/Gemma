package edu.columbia.gemma.web.controller.flow.action.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.flow.Event;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.ScopeType;
import org.springframework.web.flow.action.FormAction;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignEditAction extends FormAction {
    protected final transient Log log = LogFactory.getLog( getClass() );
    private ArrayDesignService arrayDesignService;

    /**
     * 
     */
    public ArrayDesignEditAction() {
        setFormObjectName( "arrayDesign" );
        setFormObjectClass( ArrayDesignImpl.class );
        setFormObjectScope( ScopeType.FLOW );
    }

    public Object createFormObject( RequestContext context ) throws InstantiationException, IllegalAccessException {
        log.info( "originating event: " + context.getOriginatingEvent() );
        log.info( "flow scope: " + context.getFlowScope() );
        log.info( "request scope: " + context.getRequestScope() );

        ArrayDesign arrayDesign = ( ArrayDesignImpl ) super.createFormObject( context );
        // TODO arrayDesign.setXXX(?);
        return arrayDesign;
    }

    // protected void initBinder(RequestContext context, DataBinder binder){
    //        
    // }

    public Event save( RequestContext context ) throws Exception {
        log.info( "saving ArrayDesign" );

        ArrayDesign arrayDesign = ( ArrayDesign ) context.getFlowScope().getAttribute( "arrayDesign" );
        getArrayDesignService().saveArrayDesign( arrayDesign );
        // String msg = "<strong>" + arrayDesign.getName() + "</strong> added successfully.";
        // context.getRequestScope().setAttribute( "message", msg );

        return success();
    }

    // /**
    // * This is the equivalent of writing the onSubmit method in a Spring Controller, or a doGet (doPost) method in a
    // * Java Servlet.
    // *
    // * @param context
    // * @return Event
    // * @exception Exception
    // */
    // protected Event doExecuteAction( RequestContext context ) throws Exception {
    // Collection col = getArrayDesignService().getAllArrayDesigns();
    // context.getRequestScope().setAttribute( "arrayDesigns", col );
    //
    // return success();
    // }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @return Returns the arrayDesignService.
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }
}
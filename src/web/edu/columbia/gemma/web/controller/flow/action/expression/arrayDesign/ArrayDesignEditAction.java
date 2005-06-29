package edu.columbia.gemma.web.controller.flow.action.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
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
    private ArrayDesign ad = null;

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

        String name = ( String ) context.getFlowScope().getRequiredAttribute( "name", String.class );

        // ad = ( ArrayDesignImpl ) super.createFormObject( context );
        ad = getArrayDesignService().findArrayDesignByName( name );

        if ( ad != null ) context.getRequestScope().setAttribute( "arrayDesign", ad );

        return ad;
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     * @param binder
     */
    protected void initBinder( RequestContext context, DataBinder binder ) {

        this.setBindOnSetupForm( true );

        log.info( ( this.isBindOnSetupForm() ) );

    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {

        log.info( "flow scope: " + context.getFlowScope().getAttributeMap() );
        log.info( "request scope: " + context.getRequestScope().getAttributeMap() );

        ad.setName( ( String ) context.getFlowScope().getRequiredAttribute( "name", String.class ) );
        ad.setDescription( ( String ) context.getRequestScope().getAttribute( "description", String.class ) );

        // ad.setNumberOfFeatures( Integer.parseInt( ( String ) context.getRequestScope().getAttribute(
        // "numberOfFeatures",
        // String.class ) ) );

        log.info( "updating ArrayDesign " + ad.getName() );

        getArrayDesignService().updateArrayDesign( ad );

        return success();
    }

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
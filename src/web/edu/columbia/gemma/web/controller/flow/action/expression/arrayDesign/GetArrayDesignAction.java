package edu.columbia.gemma.web.controller.flow.action.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GetArrayDesignAction extends AbstractAction {
    private static Log log = LogFactory.getLog( GetArrayDesignAction.class );

    private ArrayDesignService arrayDesignService;

    /**
     * @return Returns the arrayDesignService.
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param context
     * @return Event
     */
    protected Event doExecute( RequestContext context ) throws Exception {

        String name = ( String ) context.getSourceEvent().getAttribute( "name" );
        context.getFlowScope().setAttribute( "name", name );

        log.info( context.getFlowScope().getAttribute( "name" ) );

        ArrayDesign ad = getArrayDesignService().findArrayDesignByName( name );
        if ( ad != null ) {
            context.getRequestScope().setAttribute( "arrayDesign", ad );
            return success();
        }
        return error();
    }
}
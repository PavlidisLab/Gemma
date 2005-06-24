package edu.columbia.gemma.web.controller.flow.action.expression.arrayDesign;

import org.springframework.web.flow.Event;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.action.AbstractAction;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * 
 */
public class GetArrayDesignAction extends AbstractAction {

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

    protected Event doExecuteAction( RequestContext context ) throws Exception {
        String name = ( String ) context.getFlowScope().getRequiredAttribute( "name", String.class );
        ArrayDesign ad = getArrayDesignService().findArrayDesignByName( name );
        if ( ad != null ) {
            context.getRequestScope().setAttribute( "arrayDesign", ad );
            return success();
        } else {
            return error();
        }
    }
}
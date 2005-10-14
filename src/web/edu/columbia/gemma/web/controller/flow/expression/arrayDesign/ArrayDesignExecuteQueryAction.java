/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.controller.flow.expression.arrayDesign;

import java.util.Collection;

import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignExecuteQueryAction extends AbstractAction {

    private ArrayDesignService arrayDesignService;

    /**
     * 
     */
    public ArrayDesignExecuteQueryAction() {

    }

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
     * This is the equivalent of writing the onSubmit method in a Spring Controller, or a doGet (doPost) method in a
     * Java Servlet.
     * 
     * @param context
     * @return Event
     * @exception Exception
     */
    protected Event doExecute( RequestContext context ) throws Exception {
        Collection col = getArrayDesignService().getAllArrayDesigns();
        context.getRequestScope().setAttribute( "arrayDesigns", col );

        return success();
    }
}
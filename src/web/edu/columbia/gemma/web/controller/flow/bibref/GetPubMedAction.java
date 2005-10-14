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
package edu.columbia.gemma.web.controller.flow.bibref;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GetPubMedAction extends AbstractAction {
    private static Log log = LogFactory.getLog( GetPubMedAction.class.getName() );
    private BibliographicReferenceService bibliographicReferenceService;

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    @Override
    protected Event doExecute( RequestContext context ) throws Exception {
        String pubMedId = ( String ) context.getFlowScope().getRequiredAttribute( "pubMedId", String.class );
        BibliographicReference br = getBibliographicReferenceService().findByExternalId( pubMedId );
        if ( br != null ) {
            context.getRequestScope().setAttribute( "bibliographicReference", br );
            return success();
        }
        log.error("Didn't find pubMedId " + pubMedId );
        return error();
    }
}
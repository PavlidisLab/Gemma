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
package ubic.gemma.web.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceImpl;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryImpl;
import ubic.gemma.web.controller.common.description.bibref.PubMedSearchCommand;

/**
 * Validating object for BibliographicReference
 * 
 * @spring.bean id="pubMedAccessionValidator"
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class PubMedAccessionValidator implements Validator {

    private static Log log = LogFactory.getLog( PubMedAccessionValidator.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public boolean supports( Class clazz ) {
        return clazz.isAssignableFrom( PubMedSearchCommand.class );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
     */
    public void validate( Object obj, Errors errors ) {
        log.debug( "Validating: " + obj );

        PubMedSearchCommand p = ( PubMedSearchCommand ) obj;

        try {
            Integer.parseInt( p.getAccession() );
        } catch ( NumberFormatException e ) {
            errors.reject( "error.integerFormat", new Object[] { p.getAccession() }, "Accession must be an integer." );
        }
    }
}
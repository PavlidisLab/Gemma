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
package edu.columbia.gemma.web.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceImpl;

/**
 * Validating object for BibliographicReference
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PubMedValidator implements Validator {

    private static Log log = LogFactory.getLog( PubMedValidator.class.getName() );

    public boolean supports( Class clazz ) {
        return clazz.equals( BibliographicReferenceImpl.class );
    }

    // TODO either replace this with a BibliographicReferenceServiceQueryValidator
    // (yes, it should be "...Service...") or add the pubMedId to the POJO BibliographicReference.
    public void validate( Object obj, Errors errors ) {
        log.debug( "Validating: " + obj.toString() );
        BibliographicReference query = ( BibliographicReference ) obj;
        if ( ( query.getAuthorList() == null || query.getAuthorList().length() == 0 )
                && ( query.getTitle() == null || query.getTitle().length() == 0 ) ) {
            errors.reject( "noCriteria", "Please provide some query criteria!" );
        }
    }
}
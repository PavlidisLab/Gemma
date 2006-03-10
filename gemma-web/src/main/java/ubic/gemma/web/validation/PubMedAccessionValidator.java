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

/**
 * Validating object for BibliographicReference
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
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
        return clazz.isAssignableFrom( DatabaseEntryImpl.class ) || clazz.isAssignableFrom( BibliographicReferenceImpl.class ) ;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
     */
    public void validate( Object obj, Errors errors ) {
        log.debug( "Validating: " + obj.toString() );
        
        String accession = null;
        if (obj.getClass().isAssignableFrom(DatabaseEntryImpl.class)) {
            DatabaseEntry databaseEntry = ( DatabaseEntry ) obj;
            accession = databaseEntry.getAccession();
        } else if ( obj.getClass().isAssignableFrom(BibliographicReferenceImpl.class)) {
            BibliographicReference bibRef = (BibliographicReference)obj;
            accession = bibRef.getPubAccession().getAccession();
        } else {
            throw new IllegalStateException("Can't validate a " + obj.getClass().getName()); 
        }
        
       
        if ( accession == null ) {
            errors.reject( "error.noCriteria", "Accession cannot be empty" );
            return;
        }

        try {
            Integer.parseInt( accession );
        } catch ( NumberFormatException e ) {
            errors.reject( "error.integerFormat", new Object[] { accession },
                    "Accession must be an integer." );
        }
    }
}
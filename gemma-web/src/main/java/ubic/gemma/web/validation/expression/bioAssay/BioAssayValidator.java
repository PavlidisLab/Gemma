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
package ubic.gemma.web.validation.expression.bioAssay;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;

/**
 * @author Kiran Keshav
 * @version $Id$
 * @spring.bean id="bioAssayProgValidator"
 */
public class BioAssayValidator implements Validator {

    Log log = LogFactory.getLog( this.getClass() );

    private BioAssay bioAssay = null;

    /**
     * @param clazz Supported class
     * @return boolean
     */
    public boolean supports( Class clazz ) {

        log.debug( "supports " + clazz );

        return clazz.equals( BioAssayImpl.class );
    }

    /**
     * @param command
     * @param errors
     */
    public void validate( Object command, Errors errors ) {

        log.debug( "validating ... " );

        bioAssay = ( BioAssay ) command;
        validateAccession( command, errors );
    }

    /**
     * @param command
     * @param errors
     */
    private void validateAccession( Object command, Errors errors ) {

        log.debug( "accession: " + bioAssay.getAccession() );

        if ( bioAssay.getAccession() == null ) {
            log.debug( "null database entry" );
        }

        else {
            log.debug( "validating accession: " + bioAssay.getAccession().getAccession() );
            ValidationUtils.rejectIfEmptyOrWhitespace( errors, "accession.accession", "required.accession",
                    "Accession is required" );
        }
    }

}

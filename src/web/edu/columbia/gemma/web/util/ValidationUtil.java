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
package edu.columbia.gemma.web.util;

import org.apache.commons.validator.Field;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.util.ValidatorUtils;
import org.springframework.validation.Errors;
import org.springmodules.commons.validator.FieldChecks;

/**
 * ValidationUtil Helper class for performing custom validations that aren't already included in the core Commons
 * Validator.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 */
public class ValidationUtil extends FieldChecks {

    /**
     * Validates that two fields match. This goes with the custom declaration in the validation-rules.xml file.
     * 
     * @param bean
     * @param va
     * @param field
     * @param errors
     * @param request
     * @return boolean
     */
    public static boolean validateTwoFields( Object bean, ValidatorAction va, Field field, Errors errors ) {
        String value = ValidatorUtils.getValueAsString( bean, field.getProperty() );
        String sProperty2 = field.getVarValue( "secondProperty" );
        String value2 = ValidatorUtils.getValueAsString( bean, sProperty2 );

        if ( !GenericValidator.isBlankOrNull( value ) ) {
            try {
                if ( !value.equals( value2 ) ) {
                    FieldChecks.rejectValue( errors, field, va );
                    return false;
                }
            } catch ( Exception e ) {
                FieldChecks.rejectValue( errors, field, va );
                return false;
            }
        }

        return true;
    }
}

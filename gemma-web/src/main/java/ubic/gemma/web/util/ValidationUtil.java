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
package ubic.gemma.web.util;

import org.apache.commons.validator.Field;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.util.ValidatorUtils;
import org.springframework.validation.Errors;
import org.springmodules.validation.commons.FieldChecks;

/**
 * ValidationUtil Helper class for performing custom validations that aren't already included in the core Commons
 * Validator.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 */
public class ValidationUtil extends FieldChecks {

    /**
     * Validates that two fields match. This goes with the custom declaration in the validation-rules-custom.xml file.
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

    /**
     * Validates that a field has a length in some range. This goes with the custom declaration in the
     * validation-rules-custom.xml file.
     * 
     * @param bean
     * @param va
     * @param field
     * @param errors
     * @return boolean
     * @author pavlidis
     */
    public static boolean validateLengthRange( Object bean, ValidatorAction va, Field field, Errors errors ) {
        String value = ValidatorUtils.getValueAsString( bean, field.getProperty() );
        String minLengthS = field.getVarValue( "minlength" );
        String maxLengthS = field.getVarValue( "maxlength" );

        assert minLengthS != null && maxLengthS != null : "Length variables names aren't valid for field";

        if ( !GenericValidator.isBlankOrNull( value ) ) {
            try {

                int minLength = Integer.parseInt( minLengthS );
                int maxLength = Integer.parseInt( maxLengthS );

                if ( value.length() < minLength || value.length() > maxLength ) {
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

    /**
     * Check that a value is an integer greater than zero.
     * 
     * @param bean
     * @param va
     * @param field
     * @param errors
     * @return
     */
    public static boolean validatePositiveNonZeroInteger( Object bean, ValidatorAction va, Field field, Errors errors ) {
        String value = ValidatorUtils.getValueAsString( bean, field.getProperty() );

        try {
            int v = Integer.parseInt( value );
            if ( v <= 0 ) {
                FieldChecks.rejectValue( errors, field, va );
                return false;
            }
        } catch ( Exception e ) {
            FieldChecks.rejectValue( errors, field, va );
            return false;
        }

        return true;
    }

}

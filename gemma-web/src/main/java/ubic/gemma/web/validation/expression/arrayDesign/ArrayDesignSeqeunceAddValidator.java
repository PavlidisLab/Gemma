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
package ubic.gemma.web.validation.expression.arrayDesign;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignSequenceAddCommand;

/**
 * This custom validator is needed because the fields in this command object aren't simple java objects (int, String).
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="arrayDesignSequenceAddValidator"
 */
public class ArrayDesignSeqeunceAddValidator implements Validator {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @Override
    public boolean supports( Class clazz ) {
        return ArrayDesignSequenceAddCommand.class.isAssignableFrom( clazz );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
     */
    @Override
    public void validate( Object obj, Errors errors ) {
        // ValidationUtils.rejectIfEmptyOrWhitespace( errors, "sequenceFile", "errors.required",
        // "Sequence file is required" );

        ValidationUtils.rejectIfEmptyOrWhitespace( errors, "arrayDesign", "errors.required",
                "Array Design is required" );

        ValidationUtils.rejectIfEmptyOrWhitespace( errors, "sequenceType", "errors.required",
                "Sequence type is required" );

    }
}

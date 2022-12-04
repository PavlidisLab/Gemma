/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.remote;

import org.apache.commons.lang3.StringUtils;
import org.directwebremoting.ConversionException;
import org.directwebremoting.convert.StringConverter;
import org.directwebremoting.extend.InboundVariable;
import ubic.gemma.model.association.GOEvidenceCode;

/**
 * @author luke
 */
public class GOEvidenceCodeConverter extends StringConverter {
    @Override
    public Object convertInbound( Class<?> paramType, InboundVariable iv ) throws ConversionException {
        String value = ( String ) super.convertInbound( paramType, iv );
        if ( StringUtils.isBlank( value ) ) {
            return null;
        }
        return GOEvidenceCode.valueOf( value );
    }
}

/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import java.io.Serializable;

import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * Indicates that a value object represents a Securable so security filtering can be provided during reading.
 * 
 * @author cmcdonald
 * @version $Id$
 */
public interface SecureValueObject extends Serializable {

    /**
     * @return the serializable ID for the represented entity
     */
    public Long getId();

    /**
     * @return the securable Class of the represented entity.
     */
    public Class<? extends Securable> getSecurableClass();

}

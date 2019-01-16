/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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

import javax.persistence.Transient;

/**
 * 
 * Not a persistent class, not used, can be removed
 * 
 * @author     paul
 * @deprecated not needed
 */
@Deprecated
public class Person extends Contact implements gemma.gsec.model.Person {

    private static final long serialVersionUID = -7873047856249494633L;
    private String lastName;

    @Transient
    @Override
    public String getFullName() {
        return this.getName() + " " + this.getLastName();
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }

    @Override
    public void setLastName( String lastName ) {
        this.lastName = lastName;
    }

}
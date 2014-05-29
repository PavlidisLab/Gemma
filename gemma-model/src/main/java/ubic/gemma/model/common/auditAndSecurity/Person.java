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

/**
 * 
 */
public abstract class Person extends ContactImpl implements gemma.gsec.model.Person {

    /**
     * Constructs new instances of {@link Person}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Person}.
         */
        public static Person newInstance() {
            return new PersonImpl();
        }

    }

    private String lastName;

    /**
     * 
     */
    @Override
    public abstract String getFullName();

    /**
     * 
     */
    @Override
    public String getLastName() {
        return this.lastName;
    }

    @Override
    public void setLastName( String lastName ) {
        this.lastName = lastName;
    }

}
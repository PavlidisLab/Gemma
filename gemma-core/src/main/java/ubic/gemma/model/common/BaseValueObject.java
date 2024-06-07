/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.common;

import java.io.Serializable;

/**
 * @author frances
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class BaseValueObject implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5290562301261202171L;

    private Serializable valueObject;

    private boolean errorFound;

    private boolean accessDenied;
    private boolean userNotLoggedIn;
    private boolean objectAlreadyRemoved;

    public Serializable getValueObject() {
        return this.valueObject;
    }

    public void setValueObject( Serializable valueObject ) {
        this.valueObject = valueObject;
    }

    public boolean isAccessDenied() {
        return this.accessDenied;
    }

    public void setAccessDenied( boolean accessDenied ) {
        this.accessDenied = accessDenied;
    }

    public boolean isErrorFound() {
        return this.errorFound;
    }

    public void setErrorFound( boolean errorFound ) {
        this.errorFound = errorFound;
    }

    public boolean isObjectAlreadyRemoved() {
        return this.objectAlreadyRemoved;
    }

    public void setObjectAlreadyRemoved( boolean objectAlreadyRemoved ) {
        this.objectAlreadyRemoved = objectAlreadyRemoved;
    }

    public boolean isUserNotLoggedIn() {
        return this.userNotLoggedIn;
    }

    public void setUserNotLoggedIn( boolean userNotLoggedIn ) {
        this.userNotLoggedIn = userNotLoggedIn;
    }
}

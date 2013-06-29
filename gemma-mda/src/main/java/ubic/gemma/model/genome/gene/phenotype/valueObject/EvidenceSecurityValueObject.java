/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ubic.gemma.model.genome.gene.phenotype.valueObject;

/**
 * TODO Document Me
 * 
 * @author ?
 * @version $Id$
 */
public class EvidenceSecurityValueObject {

    private boolean currentUserHasWritePermission = false;

    private boolean currentUserIsOwner = false;

    // rendered as "publik" in javascript.
    private boolean isPublik = true;

    private boolean isShared = false;

    private String owner = null;

    public EvidenceSecurityValueObject() {
    }

    public EvidenceSecurityValueObject( boolean currentUserHasWritePermission, boolean currentUserIsOwner,
            boolean isPublic, boolean isShared, String owner ) {
        super();
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.currentUserIsOwner = currentUserIsOwner;
        this.isPublik = isPublic;
        this.isShared = isShared;
        this.owner = owner;
    }

    public boolean isCurrentUserHasWritePermission() {
        return this.currentUserHasWritePermission;
    }

    public void setCurrentUserHasWritePermission( boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    public boolean isCurrentUserIsOwner() {
        return this.currentUserIsOwner;
    }

    public void setCurrentUserIsOwner( boolean currentUserIsOwner ) {
        this.currentUserIsOwner = currentUserIsOwner;
    }

    public boolean isPublik() {
        return this.isPublik;
    }

    public void setPublik( boolean isPublic ) {
        this.isPublik = isPublic;
    }

    public boolean isShared() {
        return this.isShared;
    }

    public void setShared( boolean isShared ) {
        this.isShared = isShared;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner( String owner ) {
        this.owner = owner;
    }

}

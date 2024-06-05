/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.model.blacklist;

import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.description.DatabaseEntry;

import java.io.Serializable;

/**
 * 
 * @author paul
 */
public abstract class BlacklistedEntity extends AbstractDescribable implements Serializable {

    /**
     * The external accession
     */
    private DatabaseEntry externalAccession;
    
    /**
     * The reason the entity was blacklisted.
     */
    private String reason;

    private String shortName;

    public DatabaseEntry getExternalAccession() {
        return externalAccession;
    }

    public String getReason() {
        return reason;
    }

    public String getShortName() {
        return shortName;
    }

    public void setExternalAccession( DatabaseEntry externalAccession ) {
        this.externalAccession = externalAccession;
    }

    public void setReason( String reason ) {
        this.reason = reason;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

}

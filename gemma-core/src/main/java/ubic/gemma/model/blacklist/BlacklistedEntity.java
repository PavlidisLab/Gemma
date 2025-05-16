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

import javax.annotation.Nullable;

/**
 * Represents a blacklisted entity that should not be loaded into Gemma.
 * @author paul
 * @see BlacklistedPlatform
 * @see BlacklistedExperiment
 */
public abstract class BlacklistedEntity extends AbstractDescribable {

    /**
     * A short name if this was previously a Gemma platform or dataset.
     */
    @Nullable
    private String shortName;

    /**
     * An external accession.
     */
    @Nullable
    private DatabaseEntry externalAccession;

    /**
     * The reason the entity was blacklisted.
     */
    private String reason;

    @Nullable
    public String getShortName() {
        return shortName;
    }

    public void setShortName( @Nullable String shortName ) {
        this.shortName = shortName;
    }

    @Nullable
    public DatabaseEntry getExternalAccession() {
        return externalAccession;
    }

    public void setExternalAccession( @Nullable DatabaseEntry externalAccession ) {
        this.externalAccession = externalAccession;
    }

    public String getReason() {
        return reason;
    }

    public void setReason( String reason ) {
        this.reason = reason;
    }
}

/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.model.common.description;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.core.loader.util.ExternalDatabaseUtils;
import ubic.gemma.model.common.IdentifiableValueObject;

import javax.annotation.Nullable;

/**
 * ValueObject for database entry
 */
@SuppressWarnings("WeakerAccess") // Used in frontend
@Data
@EqualsAndHashCode(of = { "accession", "externalDatabase" }, callSuper = false)
public class DatabaseEntryValueObject extends IdentifiableValueObject<DatabaseEntry> {

    private static final long serialVersionUID = -527323410580090L;
    private String accession;
    @Nullable
    private String uri;
    /**
     * A label for display purposes.
     * <p>
     * This is usually the {@link #accession}, but for some databases, it may be a bit cryptic for the end-user, so
     * another string is used.
     */
    @Nullable
    private String label;
    private ExternalDatabaseValueObject externalDatabase;

    public DatabaseEntryValueObject() {
        super();
    }

    public DatabaseEntryValueObject( DatabaseEntry de ) {
        super( de );
        this.accession = de.getAccession();
        this.uri = ExternalDatabaseUtils.getUri( de );
        this.label = ExternalDatabaseUtils.getLabel( de );
        this.externalDatabase =
                de.getExternalDatabase() != null ? new ExternalDatabaseValueObject( de.getExternalDatabase() ) : null;
    }

    public DatabaseEntryValueObject( long id ) {
        super( id );
    }
}

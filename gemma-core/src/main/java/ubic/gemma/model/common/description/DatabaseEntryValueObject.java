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

import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;

import java.io.Serializable;

/**
 * ValueObject for database entry
 */
@SuppressWarnings("WeakerAccess") // Used in frontend
@EqualsAndHashCode(of = { "accession", "externalDatabase" }, callSuper = false)
public class DatabaseEntryValueObject extends IdentifiableValueObject<DatabaseEntry> implements Serializable {

    private static final long serialVersionUID = -527323410580090L;
    private String accession;
    private ExternalDatabaseValueObject externalDatabase;

    protected DatabaseEntryValueObject( Long id ) {
        super( id );
    }

    public DatabaseEntryValueObject( DatabaseEntry de ) {
        super( de );
        this.accession = de.getAccession();
        this.externalDatabase =
                de.getExternalDatabase() != null ? new ExternalDatabaseValueObject( de.getExternalDatabase() ) : null;
    }

    public String getAccession() {
        return this.accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public ExternalDatabaseValueObject getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ExternalDatabaseValueObject externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

}

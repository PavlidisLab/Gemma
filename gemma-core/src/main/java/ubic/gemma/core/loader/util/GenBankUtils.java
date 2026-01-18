/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.util;

import org.springframework.stereotype.Component;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;

/**
 * Provides convenience methods for GenBank.
 *
 * @author pavlidis
 */
@Component
public class GenBankUtils {

    /**
     * @return a transient instance of the GenBank database reference.
     */
    public static ExternalDatabase getGenBank() {
        ExternalDatabase genBank = ExternalDatabase.Factory.newInstance();
        genBank.setType( DatabaseType.SEQUENCE );
        genBank.setName( ExternalDatabases.GENBANK );
        return genBank;
    }

    /**
     * @param accession in the form XXXXXX or XXXXX.N where N is a version number. The first part becomes the accession,
     *                  the second the version
     * @return a DatabaseEntry representing the genbank accession.
     */
    public static DatabaseEntry getGenBankAccession( String accession ) {
        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();

        String[] split = accession.split( "\\." );

        dbEntry.setAccession( split[0] );

        if ( split.length == 2 )
            dbEntry.setAccessionVersion( split[1] );

        dbEntry.setExternalDatabase( GenBankUtils.getGenBank() );

        return dbEntry;
    }

}

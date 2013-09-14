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
package ubic.gemma.loader.util.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;

/**
 * Provides convenience methods to provide ExternalDatabases and DatabaseEntries for common cases, such as Genbank.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class ExternalDatabaseUtils {

    private static Log log = LogFactory.getLog( ExternalDatabaseUtils.class.getName() );

    /**
     * @return a transient instance of the Genbank database referece.
     */
    public static ExternalDatabase getGenbank() {
        ExternalDatabase genBank = ExternalDatabase.Factory.newInstance();
        genBank.setType( DatabaseType.SEQUENCE );
        genBank.setName( "Genbank" );
        return genBank;
    }

    /**
     * @param accession in the form XXXXXX or XXXXX.N where N is a version number. The first part becomes the accession,
     *        the second the version
     * @return a DatabaseEntry representing the genbank accession.
     */
    public static DatabaseEntry getGenbankAccession( String accession ) {
        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();

        String[] split = accession.split( "\\." );

        dbEntry.setAccession( split[0] );

        if ( split.length == 2 ) dbEntry.setAccessionVersion( split[1] );

        dbEntry.setExternalDatabase( getGenbank() );

        return dbEntry;
    }

    @Autowired
    ExternalDatabaseDao externalDatabaseDao;

    /**
     * @param seekPersistent if true, searches the database for an existing persistent copy. If false, just returns a
     *        transient instance.
     * @return
     */
    public ExternalDatabase getGenbank( boolean seekPersistent ) {
        if ( seekPersistent ) {
            ExternalDatabase genBank = externalDatabaseDao.findByName( "Genbank" );
            if ( genBank == null ) {
                log.error( "Persistent Genbank not found" );
                return getGenbank();
            }
            return genBank;
        }
        return getGenbank();
    }

    /**
     * @param accession
     * @param seekPersistentDb
     * @return
     */
    public DatabaseEntry getGenbankAccession( String accession, boolean seekPersistentDb ) {
        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();

        dbEntry.setAccession( accession );
        dbEntry.setExternalDatabase( this.getGenbank( seekPersistentDb ) );

        return dbEntry;
    }

    /**
     * @param dao the dao to set
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

}

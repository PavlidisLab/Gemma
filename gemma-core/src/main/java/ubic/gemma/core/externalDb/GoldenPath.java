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
package ubic.gemma.core.externalDb;

import lombok.Getter;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Settings;

/**
 * Perform useful queries against GoldenPath (UCSC) databases.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Getter
public abstract class GoldenPath {

    protected static final Log log = LogFactory.getLog( GoldenPath.class );

    private final JdbcTemplate jdbcTemplate;
    private final ExternalDatabase searchedDatabase;
    private final Taxon taxon;

    /**
     * Create a GoldenPath database for a given taxon.
     */
    public GoldenPath( Taxon taxon ) {
        this.jdbcTemplate = createJdbcTemplateFromConfig( taxon );
        this.searchedDatabase = createExternalDatabase( taxon );
        this.taxon = taxon;
    }

    private static JdbcTemplate createJdbcTemplateFromConfig( Taxon taxon ) {
        String host;
        int port;
        String user;
        String password;
        String databaseName = getDbNameForTaxon( taxon );
        host = Settings.getString( "gemma.goldenpath.db.host" );
        port = Settings.getInt( "gemma.goldenpath.db.port", 3306 );

        user = Settings.getString( "gemma.goldenpath.db.user" );
        password = Settings.getString( "gemma.goldenpath.db.password" );

        BasicDataSource dataSource = new BasicDataSource();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true&useSSL=false";
        GoldenPath.log.info( "Connecting to " + databaseName );
        GoldenPath.log.debug( "Connecting to Golden Path : " + url + " as " + user );

        String driver = Settings.getString( "gemma.goldenpath.db.driver" );
        if ( StringUtils.isBlank( driver ) ) {
            driver = Settings.getString( "gemma.db.driver" );
            GoldenPath.log.warn( "No DB driver configured for GoldenPath, falling back on gemma.db.driver=" + driver );
        }
        dataSource.setDriverClassName( driver );
        dataSource.setUrl( url );
        dataSource.setUsername( user );
        dataSource.setPassword( password );

        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.setFetchSize( 50 );

        return jdbcTemplate;
    }

    private static ExternalDatabase createExternalDatabase( Taxon taxon ) {
        ExternalDatabase externalDatabase = ExternalDatabase.Factory.newInstance();
        externalDatabase.setName( getDbNameForTaxon( taxon ) );
        externalDatabase.setType( DatabaseType.SEQUENCE );
        return externalDatabase;
    }

    private static String getDbNameForTaxon( Taxon taxon ) {
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon cannot be null" );
        }
        if ( taxon.getCommonName() == null ) {
            throw new IllegalArgumentException( "Taxon common name cannot be null." );
        }
        String databaseName = Settings.getString( "gemma.goldenpath.db." + taxon.getCommonName() );
        if ( databaseName == null ) {
            throw new IllegalStateException( String.format( "No GoldenPath database for %s.", taxon ) );
        }
        return databaseName;
    }
}
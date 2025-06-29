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
package ubic.gemma.core.goldenpath;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;

import javax.sql.DataSource;

/**
 * Perform useful queries against GoldenPath (UCSC) databases.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class GoldenPath implements AutoCloseable {

    protected static final Log log = LogFactory.getLog( GoldenPath.class );

    private final HikariDataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ExternalDatabase searchedDatabase;
    private final Taxon taxon;

    /**
     * Create a GoldenPath database for a given taxon.
     */
    public GoldenPath( Taxon taxon ) {
        this.dataSource = createDataSource( taxon );
        this.jdbcTemplate = createJdbcTemplateFromConfig( this.dataSource, taxon );
        this.searchedDatabase = createExternalDatabase( taxon );
        this.taxon = taxon;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    protected ExternalDatabase getSearchedDatabase() {
        return searchedDatabase;
    }

    public Taxon getTaxon() {
        return taxon;
    }

    private static HikariDataSource createDataSource( Taxon taxon ) {
        String driverClassName = Settings.getString( "gemma.goldenpath.db.driver" );
        String url = Settings.getString( "gemma.goldenpath.db.url" );
        String user = Settings.getString( "gemma.goldenpath.db.user" );
        String password = Settings.getString( "gemma.goldenpath.db.password" );
        GoldenPath.log.debug( "Connecting to Golden Path : " + url + " as " + user );

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName( "goldenpath" );
        dataSource.setDriverClassName( driverClassName );
        dataSource.setJdbcUrl( url );
        dataSource.setUsername( user );
        dataSource.setPassword( password );
        dataSource.setMaximumPoolSize( Settings.getInt( "gemma.goldenpath.db.maximumPoolSize" ) );
        dataSource.addDataSourceProperty( "relaxAutoCommit", "true" );

        return dataSource;
    }

    private static JdbcTemplate createJdbcTemplateFromConfig( HikariDataSource dataSource, Taxon taxon ) {
        String databaseName = getDbNameForTaxon( taxon );
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        GoldenPath.log.info( "Connecting to " + databaseName );
        jdbcTemplate.execute( "use " + databaseName );
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
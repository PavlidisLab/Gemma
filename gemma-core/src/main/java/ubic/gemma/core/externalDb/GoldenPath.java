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

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Settings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Perform useful queries against GoldenPath (UCSC) databases.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class GoldenPath {

    static final Log log = LogFactory.getLog( GoldenPath.class );
    JdbcTemplate jdbcTemplate;
    private ExternalDatabase searchedDatabase;
    private String databaseName = null;
    private Taxon taxon;
    private int port;
    private String host;
    private String user;
    private String password;
    private String url;

    /**
     * Get golden path for the default database (human);
     */
    public GoldenPath() {
        this.taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );
        this.readConfig();
    }

    public GoldenPath( int port, String databaseName, String host, String user, String password ) {
        this.databaseName = databaseName;

        this.getTaxonForDbName( databaseName );

        this.port = port;
        this.host = host;
        this.user = user;
        this.password = password;

        this.init();
    }

    public GoldenPath( String databaseName ) {
        this.getTaxonForDbName( databaseName );
        this.readConfig();
    }

    public GoldenPath( Taxon taxon ) {
        this.taxon = taxon;
        this.readConfig();
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public ExternalDatabase getSearchedDatabase() {
        return searchedDatabase;
    }

    public Taxon getTaxon() {
        return taxon;
    }

    protected Connection getConnection() {
        try {
            return DriverManager.getConnection( url, user, password );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    void init() {
        assert databaseName != null;
        BasicDataSource dataSource = new BasicDataSource();

        this.url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true&useSSL=false";
        GoldenPath.log.info( "Connecting to " + databaseName );
        GoldenPath.log.debug( "Connecting to Golden Path : " + url + " as " + user );

        dataSource.setDriverClassName( this.getDriver() );
        dataSource.setUrl( url );
        dataSource.setUsername( user );
        dataSource.setPassword( password );

        jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.setFetchSize( 50 );

    }

    private String getDriver() {
        String driver = Settings.getString( "gemma.goldenpath.db.driver" );
        if ( StringUtils.isBlank( driver ) ) {
            driver = Settings.getString( "gemma.db.driver" );
            GoldenPath.log.warn( "No DB driver configured for GoldenPath, falling back on gemma.db.driver=" + driver );
        }
        return driver;
    }

    private void getTaxonForDbName( String dbname ) {
        // This is a little dumb
        this.taxon = Taxon.Factory.newInstance();
        if ( dbname.startsWith( "hg" ) ) {
            taxon.setCommonName( "human" );
        } else if ( dbname.startsWith( "mm" ) ) {
            taxon.setCommonName( "mouse" );
        } else if ( dbname.startsWith( "rn" ) ) {
            taxon.setCommonName( "rat" );
        } else {
            throw new IllegalArgumentException( "Cannot infer taxon for " + dbname );
        }
    }

    private void readConfig() {
        if ( taxon == null )
            throw new IllegalStateException( "Taxon cannot be null" );
        String commonName = taxon.getCommonName();
        switch ( commonName ) {
            case "mouse":
                databaseName = Settings.getString( "gemma.goldenpath.db.mouse" );
                break;
            case "human":
                databaseName = Settings.getString( "gemma.goldenpath.db.human" );
                break;
            case "rat":
                databaseName = Settings.getString( "gemma.goldenpath.db.rat" );
                break;
            default:
                throw new IllegalArgumentException( "No GoldenPath database for  " + taxon );
        }

        this.host = Settings.getString( "gemma.goldenpath.db.host" );
        try {
            this.port = Integer.valueOf( Settings.getString( "gemma.goldenpath.db.port" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Could not get configuration of port for goldenpath database" );
        }

        this.user = Settings.getString( "gemma.goldenpath.db.user" );
        this.password = Settings.getString( "gemma.goldenpath.db.password" );

        searchedDatabase = ExternalDatabase.Factory.newInstance();
        searchedDatabase.setName( databaseName );
        searchedDatabase.setType( DatabaseType.SEQUENCE );

        this.init();

    }

}
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
package ubic.gemma.externalDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase; 
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;

/**
 * Perform useful queries against GoldenPath (UCSC) databases.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPath {

    protected static final Log log = LogFactory.getLog( GoldenPath.class );

    private ExternalDatabase searchedDatabase;

    protected DriverManagerDataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

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
        readConfig();
    }

    /**
     * @param databaseName
     * @param host
     * @param user
     * @param password
     * @throws SQLException
     */
    public GoldenPath( int port, String databaseName, String host, String user, String password ) throws SQLException {
        this.databaseName = databaseName;

        getTaxonForDbName( databaseName );

        this.port = port;
        this.host = host;
        this.user = user;
        this.password = password;

        init();
    }

    /**
     * @param databaseName hg18, rn4 etc.
     * @throws SQLException
     */
    public GoldenPath( String databaseName ) throws SQLException {
        getTaxonForDbName( databaseName );
        readConfig();
    }

    /**
     * Get a GoldenPath instance for a given taxon, using configured database settings.
     * 
     * @param taxon
     */
    public GoldenPath( Taxon taxon ) {
        this.taxon = taxon;
        readConfig();
    }
    /**
     * @return
     */
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

    /**
     * @return
     */
    protected Connection getConnection() {
        try {
            return DriverManager.getConnection( url, user, password );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    protected void init() {
        assert databaseName != null;
        dataSource = new DriverManagerDataSource();

        this.url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true";
        log.info( "Connecting to " + databaseName );
        log.debug( "Connecting to Golden Path : " + url + " as " + user );

        dataSource.setDriverClassName( getDriver() );
        dataSource.setUrl( url );
        dataSource.setUsername( user );
        dataSource.setPassword( password );

        jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.setFetchSize( 50 );

    }

    /**
     * @return
     */
    private String getDriver() {
        String driver = ConfigUtils.getString( "gemma.goldenpath.db.driver" );
        if ( StringUtils.isBlank( driver ) ) {
            driver = ConfigUtils.getString( "gemma.db.driver" );
            log.warn( "No DB driver configured for GoldenPath, falling back on gemma.db.driver=" + driver );
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
        if ( taxon == null ) throw new IllegalStateException( "Taxon cannot be null" );
        String commonName = taxon.getCommonName();
        if ( commonName.equals( "mouse" ) ) {
            databaseName = ConfigUtils.getString( "gemma.goldenpath.db.mouse" ); // FIXME get these names from an
            // external source - e.g., the taxon
            // service.
        } else if ( commonName.equals( "human" ) ) {
            databaseName = ConfigUtils.getString( "gemma.goldenpath.db.human" );
        } else if ( commonName.equals( "rat" ) ) {
            databaseName = ConfigUtils.getString( "gemma.goldenpath.db.rat" );
        } else {
            throw new IllegalArgumentException( "No GoldenPath database for  " + taxon );
        }

        this.host = ConfigUtils.getString( "gemma.goldenpath.db.host" );
        try {
            this.port = Integer.valueOf( ConfigUtils.getString( "gemma.goldenpath.db.port" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Could not get configuration of port for goldenpath database" );
        }

        this.user = ConfigUtils.getString( "gemma.goldenpath.db.user" );
        this.password = ConfigUtils.getString( "gemma.goldenpath.db.password" );

        searchedDatabase = ExternalDatabase.Factory.newInstance();
        searchedDatabase.setName( databaseName );
        searchedDatabase.setType( DatabaseType.SEQUENCE );

        init();

    }

}
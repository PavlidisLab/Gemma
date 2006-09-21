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

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ubic.gemma.model.common.description.ExternalDatabaseService;
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

    ExternalDatabaseService externalDatabaseService;

    protected DriverManagerDataSource dataSource;

    protected JdbcTemplate jt;

    protected Connection conn;

    protected QueryRunner qr;

    private String databaseName;

    /**
     * Get golden path for the default database (human);
     */
    public GoldenPath() {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );
        init( taxon );
    }

    /**
     * @param databaseName
     * @param host
     * @param user
     * @param password
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public GoldenPath( int port, String databaseName, String host, String user, String password ) throws SQLException,
            InstantiationException, IllegalAccessException, ClassNotFoundException {

        this.databaseName = databaseName;

        init( port, host, databaseName, user, password );
    }

    /**
     * Get a GoldenPath instance for a given taxon, using configured database settings.
     * 
     * @param taxon
     */
    public GoldenPath( Taxon taxon ) {
        init( taxon );
    }

    /**
     * @return
     */
    public String getDatabaseName() {
        return databaseName;
    }

    private void init( int port, String host, String databaseName, String user, String password ) {
        dataSource = new DriverManagerDataSource();
        jt = new JdbcTemplate( dataSource );

        String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true";
        log.info( "Connecting to Golden Path : " + url );

        dataSource.setDriverClassName( "com.mysql.jdbc.Driver" );
        dataSource.setUrl( url );
        dataSource.setUsername( user );
        dataSource.setPassword( password );

        // jt.setFetchSize( 1 );
        jt.setDataSource( dataSource );

        try {
            Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
            conn = DriverManager.getConnection( url, user, password );
        } catch ( InstantiationException e ) {
            throw new RuntimeException( e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }

        qr = new QueryRunner();
    }

    private void init( Taxon taxon ) {
        String commonName = taxon.getCommonName();
        if ( commonName.equals( "mouse" ) ) {
            databaseName = "mm8"; // FIXME get these names from an external source.
        } else if ( commonName.equals( "human" ) ) {
            databaseName = "hg18";
        } else if ( commonName.equals( "rat" ) ) {
            databaseName = "rn4";
        } else {
            throw new IllegalArgumentException( "No GoldenPath database for  " + taxon );
        }

        String databaseHost = ConfigUtils.getString( "gemma.testdb.host" );
        String databaseUser = ConfigUtils.getString( "gemma.testdb.user" );
        String databasePassword = ConfigUtils.getString( "gemma.testdb.password" );

        this.init( 3306, databaseHost, databaseName, databaseUser, databasePassword );
    }

    /**
     * @param externalDatabaseService
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

}
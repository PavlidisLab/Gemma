package edu.columbia.gemma.externalDb;

import java.sql.SQLException;

import net.sf.hibernate.HibernateException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.externaldb.ExternalDatabaseDaoHg17;
import edu.columbia.gemma.externaldb.ExternalDatabaseDaoHg17Hibernate;

/**
 * Tests connections to external databases.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @see ExternalDatabase
 */
public class ExternalDatabaseTest extends BaseDAOTestCase {
    Configuration conf;
    ExternalDatabaseDaoHg17 db;

    /**
     * Get the bean name of the appropriate dao object. These dao objects will be replaced by the "table name + Dao" for
     * the database in question. For the moment, I have only set this up for the hg17 database as the other 3 will work
     * in the same manner.
     * 
     * @throws ConfigurationException
     */
    public void setUp() throws ConfigurationException {
        conf = new PropertiesConfiguration( "testResources.properties" );
        db = ( ExternalDatabaseDaoHg17Hibernate ) ctx.getBean( conf.getString( "external.database.0" ) );
        //ctx.getBean( conf.getString( "external.database.1" ) );
        //ctx.getBean( conf.getString( "external.database.2" ) )
        //ctx.getBean( conf.getString( "external.database.3" ) )
    }

    public void tearDown() {
        db = null;
    }

    /**
     * Tests the database connection.
     * 
     * @throws HibernateException
     * @throws SQLException
     */
    public void testConnectToDatabase() throws HibernateException, SQLException {
        boolean connectionIsClosed = db.connectToDatabase();
        assertEquals( connectionIsClosed, false );
    }

    /**
     * @throws HibernateException
     * @throws SQLException TODO implement when you have mapped the tables from these other databases to objects.
     */
    public void testRetreiveFromDatabase() throws HibernateException, SQLException {

    }

}
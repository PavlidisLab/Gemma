package edu.columbia.gemma.externaldb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.hibernate.HibernateTemplate;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @see ExternalDatabaseDaoHg17
 */

/*
 * TODO map classes from these other databases. The actual DAO's that will be used will be the ones corresponding to
 * tables in the database.
 */
public class ExternalDatabaseDaoHg17Hibernate extends HibernateDaoSupport implements ExternalDatabaseDaoHg17 {
    protected static final Log log = LogFactory.getLog( ExternalDatabaseDaoHg17Hibernate.class );
    private Connection c;
    private HibernateTemplate ht;
    private Query q;
    private Session s;
    private SessionFactory sf;

    /**
     * Makes the database connection. The main purpose of this method is to verify that the datasources to the external
     * databases are establishing a connection.
     * 
     * @return boolean
     */
    public boolean connectToDatabase() throws HibernateException, SQLException {
        ht = getHibernateTemplate();
        sf = ht.getSessionFactory();
        s = sf.openSession();
        c = s.connection();
        log.info( "Session Factory: " + sf );
        log.info( "Session: " + s );
        log.info( "Connection: " + c );

        return c.isClosed();
    }

    /**
     * TODO implement this when you actually have the pojo's mapped to tables (use Andromda schema2Xmi)
     */
    public Collection retreiveFromDatabase() throws HibernateException, SQLException {
        q = null;
        return null;

    }

}
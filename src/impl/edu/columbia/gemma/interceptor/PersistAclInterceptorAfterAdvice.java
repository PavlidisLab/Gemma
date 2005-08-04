package edu.columbia.gemma.interceptor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AfterReturningAdvice;

/**
 * This is an 'AfterAdvice' implementation of the 'AroundAdvice' PersistAclInterceptor
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptorAfterAdvice implements AfterReturningAdvice {
    private static Log log = LogFactory.getLog( PersistAclInterceptorAfterAdvice.class.getName() );

    /**
     * Must implement this method if this class is going to be used as an interceptor
     * 
     * @param invocation
     * @return
     * @throws Throwable
     */
    public void afterReturning( Object retValue, Method m, Object[] args, Object target ) throws Throwable {

        if ( isValidMethodToIntercept( m ) ) {
            Object object = null;
            log.info( "Before: method=[" + m + "]" );

            log.info( "The method is: " + m.getName() );

            Object[] arguments = args;
            for ( Object obj : arguments ) {
                object = obj;
            }

            String fullyQualifiedName = object.getClass().getName();
            log.info( "The object is: " + fullyQualifiedName );

            log.info( "Connecting to database ... " );
            Connection c = makeDatabaseConnection();

            // retVal = invocation.proceed();

            Statement s1 = c.createStatement();
            String last = null;
            try {
                ResultSet rs = s1.executeQuery( "SELECT * FROM array_design" );
                int index = 0;

                while ( rs.next() ) {
                    last = rs.getString( 1 );
                    log.info( "ID: " + last );
                }
                s1.close();
            } catch ( SQLException se ) {
                se.printStackTrace();
                System.exit( 1 );
            }

            Statement s2 = c.createStatement();
            try {
                s2.executeUpdate( "INSERT INTO acl_object_identity (object_identity, parent_object, acl_class)"
                        + "VALUES ('" + fullyQualifiedName + ":" + last
                        + "',1,'net.sf.acegisecurity.acl.basic.SimpleAclEntry')" );
            } catch ( SQLException se ) {
                se.printStackTrace();
                System.exit( 1 );
            }
            s2.close();

            Statement s3 = c.createStatement();

            try {

                s3.executeUpdate( "INSERT INTO acl_permission (acl_object_identity, recipient, mask)"
                        + "VALUES (LAST_INSERT_ID(),'pavlab',2)" );
            } catch ( SQLException se ) {
                se.printStackTrace();
                System.exit( 1 );
            }
            s3.close();

            log.info( "Acl persisted successfully." );

            c.close();
        } else {
            log.info( "Invalid method to intercept" );
        }

    }

    /**
     * Test to see if this is a valid method to intercept. This will be fleshed out to add the actual methods intercept
     * to the xml configuration file.
     * 
     * @param m
     * @return
     */
    private boolean isValidMethodToIntercept( Method m ) {
        if ( m.getName().contains( "save" ) ) return true;

        return false;

    }

    /**
     * Making a jdbc connection.
     * 
     * @return Connection
     */
    private Connection makeDatabaseConnection() {
        Connection c = null;
        try {
            c = DriverManager.getConnection( "jdbc:mysql://localhost/gemd", "pavlidis", "toast" );
        } catch ( SQLException se ) {
            log.info( "Couldn't establish connection.  View stace trace." );
            se.printStackTrace();
            System.exit( 1 );
        }
        return c;
    }

}

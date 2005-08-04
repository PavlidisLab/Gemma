package edu.columbia.gemma.interceptor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the interceptor to call to persist acl information when an object is created.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptor implements MethodInterceptor {
    private static Log log = LogFactory.getLog( PersistAclInterceptor.class.getName() );

    /**
     * Must implement this method if this class is going to be used as an interceptor
     * 
     * @param invocation
     * @return Object
     * @throws Throwable
     */
    public Object invoke( MethodInvocation invocation ) throws Throwable {
        if ( isValidMethodToIntercept( invocation ) ) {
            Object object = null;
            log.info( "Before: invocation=[" + invocation + "]" );

            log.info( "The method is: " + invocation.getMethod() );

            Object[] arguments = invocation.getArguments();
            for ( Object obj : arguments ) {
                object = obj;
            }

            String fullyQualifiedName = object.getClass().getName();
            log.info( "The object is: " + fullyQualifiedName );

            log.info( "Connecting to database ... " );
            Connection c = makeDatabaseConnection();

            invocation.proceed();

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
                s2.executeUpdate( "INSERT INTO acl_object_identity (object_identity, acl_class)" + "VALUES ('"
                        + fullyQualifiedName + ":" + last + "','net.sf.acegisecurity.acl.basic.SimpleAclEntry')" );
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

            return null;
        }
        return null;
    }

    /**
     * Test to see if this is a valid method to intercept. This will be fleshed out to add the actual methods intercept
     * to the xml configuration file.
     * 
     * @param invocation
     * @return
     */
    private boolean isValidMethodToIntercept( MethodInvocation invocation ) {
        if ( invocation.getMethod().getName().contains( "save" ) ) return true;

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

package edu.columbia.gemma.interceptor;

import java.sql.Connection;
import java.sql.DriverManager;
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
        Object object = null;
        log.info( "Before: invocation=[" + invocation + "]" );

        log.info( "The method is: " + invocation.getMethod() );

        Object[] arguments = invocation.getArguments();
        for ( Object obj : arguments ) {
            object = obj;
        }

        String fullyQualifiedName = object.getClass().toString();
        log.info( "The object is: " + fullyQualifiedName );

        invocation.proceed();
        // TODO I have shown here via jdbc that you can get the fully qualifed object name of the object you are
        // trying to create. Now, I just need the id and I should be able to run the arrayDesignLoaderTest and
        // insert the correct permission in the acl table.
        log.info( "Connecting to database ... " );
        Connection c = makeDatabaseConnection();

        Statement s = c.createStatement();
        String dummyId = ( new Date() ).toString();
        try {
            s.executeUpdate( "INSERT INTO acl_object_identity (object_identity, acl_class)" + "VALUES ('"
                    + fullyQualifiedName + ":" + dummyId + "','net.sf.acegisecurity.acl.basic.SimpleAclEntry')" );
        } catch ( SQLException se ) {
            se.printStackTrace();
            System.exit( 1 );
        }

        log.info( "Object persisted successfully." );

        return null;
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

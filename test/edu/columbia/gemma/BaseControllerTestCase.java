package edu.columbia.gemma;

import java.util.ResourceBundle;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import junit.framework.TestCase;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @author raible
 * @version $Id$
 */
public class BaseControllerTestCase extends TestCase{
    protected final static ApplicationContext ctx;
    
    static {
        ResourceBundle db = ResourceBundle.getBundle( "testdatabase" );
        String daoType = db.getString( "dao.type" );

        // Make sure you have the /web on the junit classpath.
        String[] paths = { "loader-servlet.xml", "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml" };
        ctx = new ClassPathXmlApplicationContext( paths );
    }
    

}

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
        ResourceBundle db = ResourceBundle.getBundle( "testResources" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString("servlet.name.0");
        // Make sure you have the /web on the junit classpath.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml", servletContext+"-servlet.xml" };
        ctx = new ClassPathXmlApplicationContext( paths );
    }
    

}

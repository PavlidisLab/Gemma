package edu.columbia.gemma;

import java.util.ResourceBundle;

import org.springframework.test.web.flow.AbstractFlowExecutionTests;

public class BaseFlowTestCase extends AbstractFlowExecutionTests{
    
    protected String flowId() {
        return null;
    }
    
    protected String[] getConfigLocations() {

        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        // Make sure you have the /web on the junit classpath.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml",
                servletContext + "-servlet.xml" };

        return paths;
    }

}

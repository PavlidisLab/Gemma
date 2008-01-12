package ubic.gemma.persistence;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.EntityMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import ubic.gemma.testing.BaseSpringContextTest;

public class HibernateXmlTest extends BaseSpringContextTest {

    /**
     * Not implemented.
     * 
     * @throws Exception
     */
    public void testFoo() throws Exception {
        // SessionFactory f = ( SessionFactory ) this.getBean( "sessionFactory" );
        //
        // Session session = f.openSession();
        // Session dom4jSession = session.getSession( EntityMode.DOM4J );
        //
        // Transaction tx = session.beginTransaction();
        // Query q = dom4jSession.createQuery( "from ArrayDesignImpl" );
        // q.setMaxResults( 1 );
        // List results = q.list();
        // OutputFormat format = OutputFormat.createPrettyPrint();
        // XMLWriter writer = new XMLWriter( System.out, format );
        // for ( Object object : results ) {
        // Element e = ( Element ) object;
        // writer.write( e );
        // }
        // tx.commit();
        // session.close();

    }

}

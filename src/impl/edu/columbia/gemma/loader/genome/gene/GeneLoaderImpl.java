package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderImpl implements GeneLoader {
    protected static final Log log = LogFactory.getLog( GeneParser.class );

    private static BeanFactory ctx;
    static {
        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );

        // CAREFUL, these paths are dependent on the classpath for the test.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml",
                servletContext + "-servlet.xml" };
        ctx = new ClassPathXmlApplicationContext( paths );
    }

    private GeneDao geneDao;

    /**
     * Persist genes in collection.
     * 
     * @param col
     */
    public void create( Collection col ) {
        // create( col );
        Iterator iter = col.iterator();
        while ( iter.hasNext() ) {
            Gene g = ( Gene ) iter.next();
            ( ( GeneDao ) ctx.getBean( "geneDao" ) ).create( g );
        }
    }

    /**
     * Persist gene.
     * 
     * @param gene
     */
    public void create( Gene gene ) {
        // TODO Auto-generated method stub
    }

    /**
     * @return Returns the geneDao.
     */
    public GeneDao getGeneDao() {
        return geneDao;
    }

    public void removeAll( Collection col ) {
        Iterator iter = col.iterator();
        while ( iter.hasNext() ) {
            Gene g = ( Gene ) iter.next();
            ( ( GeneDao ) ctx.getBean( "geneDao" ) ).remove( g );
        }
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }
}


package edu.columbia.gemma.loader.association;


import java.util.Collection;

import baseCode.util.StringUtil;

import edu.columbia.gemma.association.LiteratureAssociationImpl;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.association.LiteratureAssociationDao;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;


/**
 * Class to parse a file of literature associations. 
 * 
 * Format: (read whole row)
 * g1_dbase\t    gl_name\t   g1_ncbiid\t g2_dbase\t    g2_name\t   g2_ncbiid\t  action\t    count
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author anshu
 * @version $Id$
 */

public class LitAssociationFileParserImpl extends BasicLineMapParser /*implements Persister*/ {
    
    public static final int LIT_ASSOCIATION_FIELDS_PER_ROW=8;
    public static final int PERSIST_CONCURRENTLY=1;
    public static final int DO_NOT_PERSIST_CONCURRENTLY=0;
    public static final int PERSIST_DEFAULT=0;
    
    private int mPersist = PERSIST_DEFAULT;
    private GeneDao geneDao;
    private LiteratureAssociationDao laDao;
    
    public LitAssociationFileParserImpl(int persistType, GeneDao gdao, LiteratureAssociationDao ldao){
        this.mPersist=persistType;
        this.geneDao=gdao;
        this.laDao=ldao;        
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        System.out.println(line);
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != LIT_ASSOCIATION_FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + LIT_ASSOCIATION_FIELDS_PER_ROW );
        }
        
        Collection<Gene> c;
        LiteratureAssociationImpl assoc = new LiteratureAssociationImpl();
        Gene g1=null;
        Gene g2=null;
        Integer id=null;
        try {
            id=new Integer(fields[1]);
            c =  geneDao.findByNcbiId(id);
            if ((c!=null) && (c.size()==1)) {
                g1 = (c.iterator()).next();
            }else throw new Exception("gene "+id+" not found. Entry skipped.");
 
            id=new Integer(fields[4]);
            c = geneDao.findByNcbiId(id);
            if ((c!=null) && (c.size()==1)) {
                g2 = (c.iterator()).next();
            }else throw new Exception("gene "+id+" not found. Entry skipped.");
            assoc.setFirstGene(g1);
            assoc.setSecondGene(g2);
            assoc.setAction(fields[6]);
            assoc.setNumberOfMentions(new Integer(fields[7]).intValue());
            assoc.setSource(null); //change to GENEWAYS
            
            if (mPersist==PERSIST_CONCURRENTLY) {
                laDao.create(fields[6],g1,new Integer(fields[7]),g2);
            }
        }catch(Exception e){
            System.out.println(e.toString());
        }
        return null;
    }

    /**
     * 
     */
    public void removeAll() {
        Collection col = laDao.loadAll();
        laDao.remove( col );
    }
    
    public Object getKey(Object obj) {
        return null;
    }
    

}

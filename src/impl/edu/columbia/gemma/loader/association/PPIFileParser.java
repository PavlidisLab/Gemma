package edu.columbia.gemma.loader.association;


import java.util.Collection;

import baseCode.util.StringUtil;

import edu.columbia.gemma.association.ProteinProteinInteractionImpl;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.genome.gene.GeneProductDao;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.association.ProteinProteinInteractionDao;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;


/**
 * Class to parse a file of protein-protein interactions (retrieved from BIND). 
 * 
 * Format: (read whole row)
 * pl_ncbiid\t  p2_ncbiid\t  external_db\t  db_id\t numMentions\t   action\t
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author anshu
 * @version $Id$
 */

public class PPIFileParser extends BasicLineMapParser /*implements Persister*/ {
    
    public static final int PPI_FIELDS_PER_ROW=6;
    public static final int PERSIST_CONCURRENTLY=1;
    public static final int DO_NOT_PERSIST_CONCURRENTLY=0;
    public static final int PERSIST_DEFAULT=0;
    
    private int mPersist = PERSIST_DEFAULT;
    private GeneProductDao gpDao;
    private ProteinProteinInteractionDao ppiDao;
    private ExternalDatabaseDao dbDao;
    
    public PPIFileParser(int persistType, GeneProductDao gdao, ProteinProteinInteractionDao ldao, ExternalDatabaseDao dDao){
        this.mPersist=persistType;
        this.gpDao=gdao;
        this.ppiDao=ldao;        
        this.dbDao=dDao;
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        System.out.println(line);
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != PPI_FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + PPI_FIELDS_PER_ROW );
        }
        
        Collection<GeneProduct> c;
        ProteinProteinInteractionImpl assoc = new ProteinProteinInteractionImpl();
        GeneProduct g1=null;
        GeneProduct g2=null;
        Integer id=null;
        ExternalDatabase db;
        try {
            id=new Integer(fields[1]);
            c =  gpDao.findByNcbiId(id);
            if ((c!=null) && (c.size()==1)) {
                g1 = (c.iterator()).next();
            }else throw new Exception("gene product "+id+" not found. Entry skipped.");
 
            id=new Integer(fields[2]);
            c = gpDao.findByNcbiId(id);
            if ((c!=null) && (c.size()==1)) {
                g2 = (c.iterator()).next();
            }else throw new Exception("gene "+id+" not found. Entry skipped.");
            
            assoc.setFirstProduct(g1);
            assoc.setSecondProduct(g2);
            db=ExternalDatabase.Factory.newInstance();
            db.setName(fields[3]);
            db=dbDao.findOrCreate(db);
            //db=dbDao.findByName(fields[8]); //calls fior external db to be pre-loaded
            assoc.setSource(db); 
            
            if (mPersist==PERSIST_CONCURRENTLY) {
                ppiDao.create(assoc);
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
        Collection col = ppiDao.loadAll();
        ppiDao.remove( col );
    }
    
    public Object getKey(Object obj) {
        return null;
    }
    

}

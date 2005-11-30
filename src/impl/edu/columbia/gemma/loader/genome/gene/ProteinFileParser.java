
package edu.columbia.gemma.loader.genome.gene;


import java.util.Collection;

import baseCode.util.StringUtil;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.gene.GeneProductDao;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.genome.gene.GeneProductType;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;


/**
 * Class to parse a file of literature associations. 
 * 
 * Format: (read whole row)
 * taxon\t  ncbi_gene_id\t  ncbi_prot_id
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author anshu
 * @version $Id$
 */

public class ProteinFileParser extends BasicLineMapParser /*implements Persister*/ {
    
    public static final int FIELDS_PER_ROW=3;
    public static final int PERSIST_CONCURRENTLY=1;
    public static final int DO_NOT_PERSIST_CONCURRENTLY=0;
    public static final int PERSIST_DEFAULT=0;
    
    private int mPersist = PERSIST_DEFAULT;
    private GeneDao geneDao;
    private GeneProductDao gpDao;
    
    public ProteinFileParser(int persistType, GeneDao gdao, GeneProductDao pdao){
        this.mPersist=persistType;
        this.geneDao=gdao;
        this.gpDao=pdao;
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        System.out.println(line);
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + FIELDS_PER_ROW );
        }
        
        Collection<Gene> c;
        GeneProduct gp = GeneProduct.Factory.newInstance();
        Gene g1=null;
        Integer id=null;
        try {
            id=new Integer(fields[2]);
            c =  geneDao.findByNcbiId(id);
            if ((c!=null) && (c.size()==1)) {
                g1 = (c.iterator()).next();
            }else throw new Exception("gene "+id+" not found. Entry skipped.");

            gp.setType(GeneProductType.PROTEIN);
            gp.setGene(g1);
            gp.setName(fields[3]);
            gp.setDescription(fields[3]);
            
            if (mPersist==PERSIST_CONCURRENTLY) {
                gpDao.create(gp);
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
        Collection col = gpDao.loadAll();
        gpDao.remove( col );
    }
    
    public Object getKey(Object obj) {
        return null;
    }
    

}
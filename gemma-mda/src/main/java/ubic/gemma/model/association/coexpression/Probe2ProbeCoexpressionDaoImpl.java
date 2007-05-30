/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.association.coexpression;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.Session;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.TaxonUtility;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 * @version $Id$
 * @author joseph
 * @author paul
 */
public class Probe2ProbeCoexpressionDaoImpl extends
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase {

    private static final int LINK_DELETE_BATCH_SIZE = 100000;
    private long eeId = 0L;

    @Override
    protected Map handleFindCoexpressionRelationships( Collection genes, QuantitationType qt, Collection ees )
            throws Exception {

        String p2pClassName;
        Gene testG = ( Gene ) genes.iterator().next(); // todo: check to make sure that all the given genes are of the
        // same taxon throw exception

        if ( TaxonUtility.isHuman( testG.getTaxon() ) )
            p2pClassName = "HumanProbeCoExpressionImpl";
        else if ( TaxonUtility.isMouse( testG.getTaxon() ) )
            p2pClassName = "MouseProbeCoExpressionImpl";
        else if ( TaxonUtility.isRat( testG.getTaxon() ) )
            p2pClassName = "RatProbeCoExpressionImpl";
        else
            // must be other
            p2pClassName = "OtherProbeCoExpressionImpl";

        final String queryStringFirstVector =
        // source tables
        "select distinct gene,p2pc.secondVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc "
                // target tables
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and p2pc.firstVector.expressionExperiment.id in (:collectionOfEE)"
                + " and p2pc.quantitationType.id = :givenQtId" + " and gene.id in (:collectionOfGenes)";

        final String queryStringSecondVector =
        // source tables
        "select distinct gene, p2pc.firstVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc "
                // target tables
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and p2pc.secondVector.expressionExperiment.id in (:collectionOfEE)"
                + " and p2pc.quantitationType.id = :givenQtId" + " and gene.id in (:collectionOfGenes)";

        Map<Gene, Collection<DesignElementDataVector>> results = new HashMap<Gene, Collection<DesignElementDataVector>>();

        try {
            // Must transform collection of objects into a collection of ids
            Collection<Long> eeIds = new ArrayList<Long>();
            for ( Iterator iter = ees.iterator(); iter.hasNext(); ) {
                ExpressionExperiment e = ( ExpressionExperiment ) iter.next();
                eeIds.add( e.getId() );
            }

            Collection<Long> geneIds = new ArrayList<Long>();
            for ( Object obj : genes )
                geneIds.add( ( ( Gene ) obj ).getId() );

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setParameterList( "collectionOfGenes", geneIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            ScrollableResults list1 = queryObject.scroll();
            buildMap( results, list1 );

            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setParameterList( "collectionOfGenes", geneIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            ScrollableResults list2 = queryObject.scroll();
            buildMap( results, list2 );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return results;
    }

    /**
     * @param toBuild
     * @param list builds the hasmap by adding toBuild the results stored in the list
     */
    private void buildMap( Map<Gene, Collection<DesignElementDataVector>> toBuild, ScrollableResults list ) {

        while ( list.next() ) {
            Gene g = ( Gene ) list.get( 0 );
            DesignElementDataVector dedv = ( DesignElementDataVector ) list.get( 1 );

            if ( toBuild.containsKey( g ) )
                toBuild.get( g ).add( dedv );
            else {
                Collection<DesignElementDataVector> dedvs = new HashSet<DesignElementDataVector>();
                dedvs.add( dedv );
                toBuild.put( g, dedvs );
            }
        }

    }

    private static Log log = LogFactory.getLog( Probe2ProbeCoexpressionDaoImpl.class.getName() );

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @SuppressWarnings("unchecked")
    protected java.util.Collection handleFindCoexpressionRelationships( ubic.gemma.model.genome.Gene givenG,
            java.util.Collection ees, ubic.gemma.model.common.quantitationtype.QuantitationType qt ) {

        String p2pClassName;
        if ( TaxonUtility.isHuman( givenG.getTaxon() ) )
            p2pClassName = "HumanProbeCoExpressionImpl";
        else if ( TaxonUtility.isMouse( givenG.getTaxon() ) )
            p2pClassName = "MouseProbeCoExpressionImpl";
        else if ( TaxonUtility.isRat( givenG.getTaxon() ) )
            p2pClassName = "RatProbeCoExpressionImpl";
        else
            // must be other
            p2pClassName = "OtherProbeCoExpressionImpl";

        final String queryStringFirstVector =
        // source tables
        "select distinct p2pc.secondVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc "
                // target tables
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and p2pc.firstVector.expressionExperiment.id in (:collectionOfEE)"
                + " and p2pc.quantitationType.id = :givenQtId" + " and gene.id = :givenGId";

        final String queryStringSecondVector =
        // source tables
        "select distinct p2pc.firstVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc "
                // target tables
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and p2pc.secondVector.expressionExperiment.id in (:collectionOfEE)"
                + " and p2pc.quantitationType.id = :givenQtId" + " and gene.id = :givenGId";

        Collection<DesignElementDataVector> dedvs = new HashSet<DesignElementDataVector>();

        try {
            // do query joining coexpressed genes through the firstVector to the secondVector
            Collection<Long> eeIds = new ArrayList<Long>();
            for ( Iterator iter = ees.iterator(); iter.hasNext(); ) {
                ExpressionExperiment e = ( ExpressionExperiment ) iter.next();
                eeIds.add( e.getId() );
            }

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            queryObject.setLong( "givenGId", givenG.getId() );
            dedvs.addAll( queryObject.list() );
            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            queryObject.setLong( "givenGId", givenG.getId() );
            dedvs.addAll( queryObject.list() );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return dedvs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleDeleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteLinks( ExpressionExperiment ee ) throws Exception {

        // FIXME figure out the taxon instead of this iteration.
        String[] p2pClassNames = new String[] { "HumanProbeCoExpressionImpl", "MouseProbeCoExpressionImpl",
                "RatProbeCoExpressionImpl", "OtherProbeCoExpressionImpl" };

        int totalDone = 0;

        for ( String p2pClassName : p2pClassNames ) {

            /*
             * Note that we only have to query for the firstVector, because we're joining over all designelement
             * datavectors for this ee.
             */
            final String queryString = "select pp from ExpressionExperimentImpl ee inner join ee.designElementDataVectors as dv, "
                    + p2pClassName + " as pp where pp.firstVector" + " = dv and ee=:ee";

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setMaxResults( LINK_DELETE_BATCH_SIZE );
            queryObject.setParameter( "ee", ee );

            // we query iteratively until there are no more links to get. This takes much less memory than doing it all
            // at once.
            while ( true ) {
                final Collection results = queryObject.list();

                if ( results.size() == 0 ) break;

                remove( results );

                Integer numDone = results.size();

                totalDone += numDone;

                log.info( "Delete link progress: " + totalDone + " ..." );
            }

            if ( totalDone > 0 ) {
                break;
            }
        }

        if ( totalDone == 0 ) {
            log.info( "No coexpression results to remove for " + ee );
        } else {
            log.info( totalDone + " coexpression results removed for " + ee );
        }

    }

    @Override
    protected Integer handleCountLinks( ExpressionExperiment expressionExperiment ) throws Exception {

        // FIXME figure out the taxon instead of this iteration.
        String[] p2pClassNames = new String[] { "HumanProbeCoExpressionImpl", "MouseProbeCoExpressionImpl",
                "RatProbeCoExpressionImpl", "OtherProbeCoExpressionImpl" };

        Integer result = 0;
        for ( String p2pClassName : p2pClassNames ) {

            /*
             * Note that we only have to query for the firstVector, because we're joining over all designelement
             * datavectors for this ee.
             */
            final String queryString = "select count(pp) from ExpressionExperimentImpl ee inner join ee.designElementDataVectors as dv, "
                    + p2pClassName + " as pp where pp.firstVector" + " = dv and ee.id=:eeId";

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "eeId", expressionExperiment.getId() );
            java.util.List results = queryObject.list();

            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of 'Integer" + "' was found when executing query --> '"
                                    + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result += ( Integer ) results.iterator().next();
                }

            }

        }
        
        /*
         * We divide by 2 because all links are stored twice
         */
        return ( Integer ) result / 2 ;

    }
     public class Link{
 		private Long first_design_element_fk = 0L;
		private Long second_design_element_fk = 0L;
		private Double score = 0.0;
		Link(){
		}
		public Long getFirst_design_element_fk() {
			return first_design_element_fk;
		}
		public void setFirst_design_element_fk(Long first_design_element_fk) {
			this.first_design_element_fk = first_design_element_fk;
		}
		public Long getSecond_design_element_fk() {
			return second_design_element_fk;
		}
		public void setSecond_design_element_fk(Long second_design_element_fk) {
			this.second_design_element_fk = second_design_element_fk;
		}
		public Double getScore(){
			return score;
		}
		public void setScore(Double score){
			this.score = score;
		}
		public String toString(){
            String res = "(" + first_design_element_fk + "," + second_design_element_fk + ", " + score + ", "+ eeId + ")";
			return res;
		}
    }
    private String getTableName(String taxon, boolean cleanedTable){
        int tableIndex = -1;
        String tableName = "";
        String[] tableNames = new String[] { "HUMAN_PROBE_CO_EXPRESSION", "MOUSE_PROBE_CO_EXPRESSION",
                "RAT_PROBE_CO_EXPRESSION", "OTHER_PROBE_CO_EXPRESSION" };
        
        if(taxon.equalsIgnoreCase("human"))
        	tableIndex = 0;
        else if(taxon.equalsIgnoreCase("mouse"))
        	tableIndex = 1;
        else if(taxon.equalsIgnoreCase("rat"))
        	tableIndex = 2;
        else
        	tableIndex = 3;
        if(cleanedTable) tableName = "CLEANED_" + tableNames[tableIndex];
        else tableName = tableNames[tableIndex];
        return tableName;
    }
    private void createTable(String tableName) throws Exception{
        Session session = getSessionFactory().openSession();
        Connection conn = session.connection();
        Statement s = conn.createStatement();
        String queryString = "DROP TABLE IF EXISTS " + tableName + ";";
        s.executeUpdate(queryString);
        queryString = "CREATE TABLE " + tableName + "(id BIGINT NOT NULL AUTO_INCREMENT, FIRST_DESIGN_ELEMENT_FK BIGINT NOT NULL, " +
        		"SECOND_DESIGN_ELEMENT_FK BIGINT NOT NULL, SCORE DOUBLE, EXPRESSION_EXPERIMENT_FK BIGINT NOT NULL, " +
        		"PRIMARY KEY(id), KEY(EXPRESSION_EXPERIMENT_FK)) " +
        		"ENGINE=MYISAM";
        s.executeUpdate(queryString);
        conn.close();
        session.close();
    	
    }
    private Map<Long, Collection<Long>> getCs2GenesMap(Collection<Long> csIds){
    	Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();
    	if(csIds == null || csIds.size() == 0) return cs2genes;
    	int count = 0;
    	int CHUNK_LIMIT = 10000;
    	int total = csIds.size();
    	Collection<Long> idsInOneChunk = new HashSet<Long>();
        Session session = getSessionFactory().openSession();
        
    	for(Long csId:csIds){
    		idsInOneChunk.add(csId);
    		count++;
    		total--;
    		if(count == CHUNK_LIMIT || total == 0){
    			String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS, CHROMOSOME_FEATURE as C WHERE GENE2CS.GENE = C.ID and C.CLASS = 'GeneImpl' and " + 
    			" CS in (" +
    			StringUtils.join( idsInOneChunk.iterator(), "," ) + 
    			")";
    			
    	        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
    	        queryObject.addScalar( "id", new LongType() );
    	        queryObject.addScalar( "geneId", new LongType() );

    	        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
    	        while ( scroll.next() ) {
    	        	Long id = scroll.getLong( 0 );
    	        	Long geneId = scroll.getLong( 1 );
	        		Collection<Long> geneIds = cs2genes.get(id);
	        		if(geneIds == null){
	        			geneIds = new HashSet<Long>();
	        			cs2genes.put( id, geneIds );
	        		}
   	        		geneIds.add(geneId);
    	        }
    			count = 0;
    			idsInOneChunk.clear();
    		}
    	}
    	session.close();
    	return cs2genes;
    }
    private Collection<Link> filtering(Collection<Link> links,Map<Long, Collection<Long>> cs2genes){
    	Collection<Link> specificLinks = new ArrayList<Link>();
    	Collection<Link> nonRedudantLinks = new ArrayList<Link>();
    	Collection<Long> mergedCsIds = new HashSet<Long>();
    	long maximumId = 0;
    	for(Link link:links){
    		Collection<Long> firstGenes = cs2genes.get(link.getFirst_design_element_fk());
    		Collection<Long> secondGenes = cs2genes.get(link.getSecond_design_element_fk());
    		if(firstGenes == null || secondGenes == null){
    			//log.error("inconsistent links for csId (" + link.getFirst_design_element_fk() + ", " + link.getSecond_design_element_fk() + ") Problem: No genes for these two composite sequence id");
    			continue;
    		}
    		boolean filtered = false;
    		for(Long firstGene:firstGenes){
    			if(secondGenes.contains(firstGene)) //probe1 is hybridized with probe2
    				filtered = true;
    		}
    		if(filtered) continue;
    		if(link.getFirst_design_element_fk() > maximumId) maximumId = link.getFirst_design_element_fk();
    		if(link.getSecond_design_element_fk() > maximumId) maximumId = link.getSecond_design_element_fk();
    		specificLinks.add(link);
    	}
    	maximumId = maximumId + 1;
    	if(maximumId*maximumId > Long.MAX_VALUE){
    		log.warn("The maximum key value is too big. the redundant detection may not correct");
    		maximumId = (long)Math.sqrt((double)Long.MAX_VALUE);
    	}
    	//remove redundancy
    	for(Link link:specificLinks){
    		Long forwardMerged = link.getFirst_design_element_fk()*maximumId + link.getSecond_design_element_fk();
    		Long backwardMerged = link.getSecond_design_element_fk()*maximumId + link.getFirst_design_element_fk();
    		if(!mergedCsIds.contains(backwardMerged)){
    			nonRedudantLinks.add(link);
    			mergedCsIds.add(forwardMerged);
    		}
    	}
    	return nonRedudantLinks;
    }
//  private Collection getLinks(ExpressionExperiment expressionExperiment, String taxon, String tableName) throws Exception{
//  String baseQueryString = "SELECT FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK FROM " + getTableName(taxon, false) + " WHERE EXPRESSION_EXPERIMENT_FK = " + ee.getId() + " limit ";
//  int chunkSize = 1000000;
//  Session session = getSessionFactory().openSession();
//  Connection conn = session.connection();
//  Statement s = conn.createStatement();
//  
//  long start = 0;
//  Collection<Link> links = new ArrayList<Link>();
//  while(true){
//  	String queryString = baseQueryString + start + "," + chunkSize + ";";
//  	ResultSet rs = s.executeQuery(queryString);
//  	int count = 0;
//  	int iterations = 0;
//  	while ( rs.next() ) {
//  		Long first_design_element_fk = rs.getLong("FIRST_DESIGN_ELEMENT_FK");
//  		Long second_design_element_fk = rs.getLong("SECOND_DESIGN_ELEMENT_FK");
//
//  		Link oneLink = new Link();
//  		oneLink.setFirst_design_element_fk(first_design_element_fk);
//  		oneLink.setSecond_design_element_fk(second_design_element_fk);
//  		links.add(oneLink);
//  		count++;
//  		if(count == chunkSize){
//  			start = start + chunkSize;
//  			System.err.print(".");
//  			iterations++;
//  			if(iterations%10 == 0) System.err.println();
//  		}
//  	}
//  	if(count < chunkSize) break;
//  }
//  System.err.println("\n Load " + links.size());
//  session.close();
//  return links;
//}
    private Collection getLinks(ExpressionExperiment expressionExperiment, String tableName) throws Exception{
    	String baseQueryString = "SELECT FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, SCORE FROM " + tableName + " WHERE EXPRESSION_EXPERIMENT_FK = " + expressionExperiment.getId() + " limit ";
    	int chunkSize = 1000000;
    	Session session = getSessionFactory().openSession();
    	long start = 0;
    	Collection<Link> links = new ArrayList<Link>();
    	while(true){
    		String queryString = baseQueryString + start + "," + chunkSize + ";";

    		org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
    		queryObject.addScalar( "FIRST_DESIGN_ELEMENT_FK", new LongType() );
    		queryObject.addScalar( "SECOND_DESIGN_ELEMENT_FK", new LongType() );
    		queryObject.addScalar( "SCORE", new DoubleType() );

    		ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
    		int count = 0;
    		int iterations = 0;
    		while ( scroll.next() ) {
    			Long first_design_element_fk = scroll.getLong(0);
    			Long second_design_element_fk = scroll.getLong(1);
    			Double score = scroll.getDouble(2);

    			Link oneLink = new Link();
    			oneLink.setFirst_design_element_fk(first_design_element_fk);
    			oneLink.setSecond_design_element_fk(second_design_element_fk);
    			oneLink.setScore(score);
    			links.add(oneLink);
    			count++;
    			if(count == chunkSize){
    				start = start + chunkSize;
    				System.err.print(".");
    				iterations++;
    				if(iterations%10 == 0) System.err.println();
    			}
    		}
    		if(count < chunkSize) break;
    	}
    	log.info("Load " + links.size());
    	session.close();
    	return links;
    }
    private void saveLinks(Collection<Link> links, ExpressionExperiment ee, String tableName) throws Exception{
    	if(links == null || links.size() == 0) return;
        String queryString = "";
        Session session = getSessionFactory().openSession();
        Connection conn = session.connection();
        Statement s = conn.createStatement();

        this.eeId = ee.getId();
    	int count = 0;
    	int CHUNK_LIMIT = 10000;
    	int total = links.size();
    	Collection<Link> linksInOneChunk = new ArrayList<Link>();
    	log.info("Writing" + links.size() + " links into tables");
    	int chunkNum = 0;
    	for(Link link:links){
    		linksInOneChunk.add(link);
    		count++;
    		total--;
    		if(count == CHUNK_LIMIT || total == 0){
    			//saveLinks(linksInOneChunk,taxon);
//    			String values = "";
//    			int i = 0;
//    			for(Link oneLink:linksInOneChunk){
//    				values = values + "( " + oneLink.toString() + ", " + ee.getId()+" )";
//    				i++;
//    				if(i != linksInOneChunk.size())
//    					values = values + ",";
//    			}
//    			queryString = "INSERT INTO " + tableName + "(FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, EXPRESSION_EXPERIMENT_FK) " + " VALUES " + values+";"; 
                queryString = "INSERT INTO " + tableName + "(FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, SCORE, EXPRESSION_EXPERIMENT_FK) " + " VALUES " + StringUtils.join(linksInOneChunk, ",")+";";
                s.executeUpdate(queryString);
    	        //conn.commit(); //not needed if autocomsmit is true.
    			count = 0;
    			linksInOneChunk.clear();
    			chunkNum++;
    			System.err.print(total+ " " );
    			if(chunkNum % 20 == 0) System.err.println();
    		}
    	}
    	log.info(" Finish writing ");
    	conn.close();
    	session.close();

    }
    private void shuffleLinks(Collection<Link> links){
    	//Do shuffling
    	Random random = new Random();
    	Object[] linksInArray = links.toArray();
    	for(int i = linksInArray.length - 1; i >= 0; i--){
    		int pos = random.nextInt(i+1);
    		Long tmpId = ((Link)linksInArray[pos]).getSecond_design_element_fk();
    		((Link)linksInArray[pos]).setSecond_design_element_fk(((Link)linksInArray[i]).getSecond_design_element_fk());
    		((Link)linksInArray[i]).setSecond_design_element_fk(tmpId);
    	}
    }
    private void doFiltering(ExpressionExperiment ee, String taxon) throws Exception{
    	String tableName = getTableName(taxon, false);
    	Collection<Link> links = getLinks(ee, tableName);
    	Set<Long> csIds = new HashSet<Long>();
        for(Link link:links){
        	csIds.add(link.getFirst_design_element_fk());
        	csIds.add(link.getSecond_design_element_fk());
        }
        Map<Long, Collection<Long>> cs2genes = getCs2GenesMap(csIds);
    	links = filtering(links, cs2genes);
        String cleanedTableName = getTableName(taxon, true);
        saveLinks(links, ee, cleanedTableName);

    	
    }
    @Override
    protected Collection handleGetProbeCoExpression( ExpressionExperiment expressionExperiment, String taxon, boolean cleaned ) throws Exception {
        // TODO Auto-generated method stub
        String tableName = getTableName(taxon, cleaned);
        Collection<Link> links = getLinks(expressionExperiment, tableName); 
        return links;
    }

    @Override
    protected void handlePrepareForShuffling( Collection ees, String taxon ) throws Exception {
        // TODO Auto-generated method stub
        if(ees.size() > 70){
            String tableName = getTableName(taxon, true);
            createTable( tableName );
        }
        int i = 1;
        for(Object ee:ees){
            log.info("Filtering EE " + ((ExpressionExperiment)ee).getShortName() + "(" + i + "/" + ees.size() + ")" );
            doFiltering((ExpressionExperiment)ee, taxon);
            i++;
        }
    }
}
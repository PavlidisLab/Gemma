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
        return ( Integer ) result;

    }
     public class Link{
		private Long id = 0L;
		private Double score = 0.0;
		private Double pvalue = 0.0;
		private Long second_vector_fk = 0L;
		private Long first_vector_fk = 0L;
		private Long quantitation_type_fk = 0L;
		private Long source_fk = 0L;
		private Long souce_analysis_fk = 0L;
		private Long human_gene_co_expression = 0L;
		private Long first_design_element_fk = 0L;
		private Long second_design_element_fk = 0L;
		private Long expression_experiment_fk = 0L;
		Link(){
		}
		public Long getExpression_experiment_fk() {
			return expression_experiment_fk;
		}
		public void setExpression_experiment_fk(Long expression_experiment_fk) {
			this.expression_experiment_fk = expression_experiment_fk;
		}
		public Long getFirst_design_element_fk() {
			return first_design_element_fk;
		}
		public void setFirst_design_element_fk(Long first_design_element_fk) {
			this.first_design_element_fk = first_design_element_fk;
		}
		public Long getFirst_vector_fk() {
			return first_vector_fk;
		}
		public void setFirst_vector_fk(Long first_vector_fk) {
			this.first_vector_fk = first_vector_fk;
		}
		public Long getHuman_gene_co_expression() {
			return human_gene_co_expression;
		}
		public void setHuman_gene_co_expression(Long human_gene_co_expression) {
			this.human_gene_co_expression = human_gene_co_expression;
		}
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public Double getPvalue() {
			return pvalue;
		}
		public void setPvalue(Double pvalue) {
			this.pvalue = pvalue;
		}
		public Long getQuantitation_type_fk() {
			return quantitation_type_fk;
		}
		public void setQuantitation_type_fk(Long quantitation_type_fk) {
			this.quantitation_type_fk = quantitation_type_fk;
		}
		public Double getScore() {
			return score;
		}
		public void setScore(Double score) {
			this.score = score;
		}
		public Long getSecond_design_element_fk() {
			return second_design_element_fk;
		}
		public void setSecond_design_element_fk(Long second_design_element_fk) {
			this.second_design_element_fk = second_design_element_fk;
		}
		public Long getSecond_vector_fk() {
			return second_vector_fk;
		}
		public void setSecond_vector_fk(Long second_vector_fk) {
			this.second_vector_fk = second_vector_fk;
		}
		public Long getSouce_analysis_fk() {
			return souce_analysis_fk;
		}
		public void setSouce_analysis_fk(Long souce_analysis_fk) {
			this.souce_analysis_fk = souce_analysis_fk;
		}
		public Long getSource_fk() {
			return source_fk;
		}
		public void setSource_fk(Long source_fk) {
			this.source_fk = source_fk;
		}
		public String toString(){
			String res = "( " +	id + "," + score + "," + pvalue + "," + second_vector_fk + "," + first_vector_fk + "," + quantitation_type_fk
			+ "," +	source_fk + "," + souce_analysis_fk + "," + human_gene_co_expression + "," + first_design_element_fk + "," + second_design_element_fk
			+ "," + expression_experiment_fk + ") ";
			return res;
		}
    }
    private String getTableName(String taxon){
        int tableIndex = -1;
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
        return tableNames[tableIndex];
    }
    
    private Map<Long, Collection<Long>> getCs2GenesMap(Collection<Long> csIds){
    	Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();
    	if(csIds == null || csIds.size() == 0) return cs2genes;
    	int count = 0;
    	int CHUNK_LIMIT = 300;
    	int total = csIds.size();
    	Collection<Long> idsInOneChunk = new HashSet<Long>();
        Session session = getSessionFactory().openSession();
        
    	for(Long csId:csIds){
    		idsInOneChunk.add(csId);
    		count++;
    		total--;
    		if(count == CHUNK_LIMIT || total == 0){
    			String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS WHERE " + 
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

    private Collection<Link> getLinks(ExpressionExperiment expressionExperiment, String taxon){
        String queryString = "SELECT * FROM " + getTableName(taxon) + " WHERE EXPRESSION_EXPERIMENT_FK = " + expressionExperiment.getId();
        Session session = getSessionFactory().openSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        
        queryObject.addScalar( "ID", new LongType() );
        queryObject.addScalar( "SCORE", new DoubleType() );
        queryObject.addScalar( "PVALUE", new DoubleType() );
        queryObject.addScalar( "SECOND_VECTOR_FK", new LongType() );
        queryObject.addScalar( "FIRST_VECTOR_FK", new LongType() );
        queryObject.addScalar( "QUANTITATION_TYPE_FK", new LongType() );
        queryObject.addScalar( "SOURCE_FK", new LongType() );
        queryObject.addScalar( "SOURCE_ANALYSIS_FK", new LongType() );
        queryObject.addScalar( "HUMAN_GENE_CO_EXPRESSION_FK", new LongType() );
        queryObject.addScalar( "FIRST_DESIGN_ELEMENT_FK", new LongType() );
        queryObject.addScalar( "SECOND_DESIGN_ELEMENT_FK", new LongType() );
        queryObject.addScalar( "EXPRESSION_EXPERIMENT_FK", new LongType() );

        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        Collection<Link> links = new ArrayList<Link>();
        while ( scroll.next() ) {
        		Long id = scroll.getLong(0);
        		Double score = scroll.getDouble(1);
        		Double pvalue = scroll.getDouble(2);
        		Long second_vector_fk = scroll.getLong(3);
        		Long first_vector_fk = scroll.getLong(4);
        		Long quantitation_type_fk = scroll.getLong(5);
        		Long source_fk = scroll.getLong(6);
        		Long souce_analysis_fk = scroll.getLong(7);
        		Long human_gene_co_expression = scroll.getLong(8);
        		Long first_design_element_fk = scroll.getLong(9);
        		Long second_design_element_fk = scroll.getLong(10);
        		Long expression_experiment_fk = scroll.getLong(11);
        		
        		Link oneLink = new Link();
        		oneLink.setId(id);
        		oneLink.setScore(score);
        		oneLink.setPvalue(pvalue);
        		oneLink.setSecond_vector_fk(second_vector_fk);
        		oneLink.setFirst_vector_fk(first_vector_fk);
        		oneLink.setQuantitation_type_fk(quantitation_type_fk);
        		oneLink.setSource_fk(source_fk);
        		oneLink.setSouce_analysis_fk(souce_analysis_fk);
        		oneLink.setHuman_gene_co_expression(human_gene_co_expression);
        		oneLink.setFirst_design_element_fk(first_design_element_fk);
        		oneLink.setSecond_design_element_fk(second_design_element_fk);
        		oneLink.setExpression_experiment_fk(expression_experiment_fk);
        		links.add(oneLink);
        }
        session.close();
        return links;
    }
    private Collection<Link> shuffleLinks(Collection<Link> links,Map<Long, Collection<Long>> cs2genes){
    	Collection<Link> specificLinks = new ArrayList<Link>();
    	Collection<Link> nonRedudantLinks = new ArrayList<Link>();
    	Collection<Long> mergedCsIds = new HashSet<Long>();
    	long maximumId = 0;
    	for(Link link:links){
    		Collection<Long> firstGene = cs2genes.get(link.getFirst_design_element_fk());
    		Collection<Long> secondGene = cs2genes.get(link.getSecond_design_element_fk());
    		if(firstGene == null || secondGene == null){
    			log.error("inconsistent links for csId " + link.getFirst_design_element_fk() + ", " + link.getSecond_design_element_fk() + ") Problem: No genes for these two composite sequence id");
    			continue;
    		}
    		if(firstGene.size() > 1 || secondGene.size() > 1) continue; //non-specific
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
    		if(!mergedCsIds.contains(forwardMerged) && !mergedCsIds.contains(backwardMerged)){
    			nonRedudantLinks.add(link);
    			mergedCsIds.add(forwardMerged);
    		}
    	}
    	//Do shuffling
    	Random random = new Random();
    	Object[] linksInArray = nonRedudantLinks.toArray();
    	for(int i = linksInArray.length - 1; i >= 0; i--){
    		int pos = random.nextInt(i+1);
    		Long tmpId = ((Link)linksInArray[pos]).getSecond_design_element_fk();
    		((Link)linksInArray[pos]).setSecond_design_element_fk(((Link)linksInArray[i]).getSecond_design_element_fk());
    		((Link)linksInArray[i]).setSecond_design_element_fk(tmpId);
    	}
    	return nonRedudantLinks;
    }
    private void saveLinks(Collection<Link> links, String taxon){
        Session session = getSessionFactory().openSession();
        String  queryString = "INSERT INTO " + getTableName(taxon) + "() " + " VALUES ";
        for(Link link:links){
        	queryString = queryString + link + " , ";
        }
        queryString = queryString + ";";
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString ); // for native query.
        queryObject.executeUpdate();
		session.flush();
		session.clear();
    }
    private void saveShuffledLinks(Collection<Link> shuffledLinks, ExpressionExperiment expressionExperiment, String taxon){
    	if(shuffledLinks == null || shuffledLinks.size() == 0) return;
    	//delete links first
        String queryString = "DELETE FROM " + getTableName(taxon) + " WHERE EXPRESSION_EXPERIMENT_FK = " + expressionExperiment.getId();
        Session session = getSessionFactory().openSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        //queryObject.executeUpdate();
        session.flush();
        
    	int count = 0;
    	int CHUNK_LIMIT = 300;
    	int total = shuffledLinks.size();
    	Collection<Link> linksInOneChunk = new ArrayList<Link>();
    	for(Link link:shuffledLinks){
    		linksInOneChunk.add(link);
    		count++;
    		total--;
    		if(count == CHUNK_LIMIT || total == 0){
    			//saveLinks(linksInOneChunk,taxon);
    			queryString = "INSERT INTO " + getTableName(taxon) + "() " + " VALUES " + StringUtils.join(linksInOneChunk.iterator(), ",")+";"; 
    			queryObject = session.createSQLQuery( queryString ); // for native query.
    			session.flush();
    			count = 0;
    			linksInOneChunk.clear();
    		}
    	}
    	session.close();
    }
    @Override
    protected void handleShuffle( ExpressionExperiment expressionExperiment, String taxon ) throws Exception {
        // TODO Auto-generated method stub
        Set<Long> csIds = new HashSet<Long>();
        Collection<Link> links = getLinks(expressionExperiment, taxon);
        for(Link link:links){
        	csIds.add(link.getFirst_design_element_fk());
        	csIds.add(link.getSecond_design_element_fk());
        }
        Map<Long, Collection<Long>> cs2genes = getCs2GenesMap(csIds);
        Collection<Link> shuffledLinks = shuffleLinks(links, cs2genes);
        saveShuffledLinks(shuffledLinks,expressionExperiment ,taxon);
    }

    @Override
    protected void handleShuffle( String taxon ) throws Exception {
        // TODO Auto-generated method stub
        // for all EE.
    }
}
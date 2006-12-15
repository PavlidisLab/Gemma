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
package ubic.gemma.model.expression.bioAssayData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 * @author pavlidis
 * @version $Id$
 */
public class DesignElementDataVectorDaoImpl extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase {

    private static Log log = LogFactory.getLog( DesignElementDataVectorDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector find( DesignElementDataVector designElementDataVector ) {
        try {
            BusinessKey.checkKey( designElementDataVector );

            Criteria queryObject = super.getSession( false ).createCriteria( DesignElementDataVector.class );

            queryObject.createCriteria( "designElement" ).add(
                    Restrictions.eq( "name", designElementDataVector.getDesignElement().getName() ) ).createCriteria(
                    "arrayDesign" ).add(
                    Restrictions.eq( "name", designElementDataVector.getDesignElement().getArrayDesign().getName() ) );

            queryObject.createCriteria( "quantitationType" ).add(
                    Restrictions.eq( "name", designElementDataVector.getQuantitationType().getName() ) );

            queryObject.createCriteria( "expressionExperiment" ).add(
                    Restrictions.eq( "name", designElementDataVector.getExpressionExperiment().getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + DesignElementDataVector.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( DesignElementDataVector ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public DesignElementDataVector findOrCreate( DesignElementDataVector designElementDataVector ) {

        DesignElementDataVector existing = find( designElementDataVector );
        if ( existing != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing designElementDataVector: " + existing );
            return existing;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new designElementDataVector: " + designElementDataVector );
        return ( DesignElementDataVector ) create( designElementDataVector );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleQueryByGeneSymbolAndSpecies(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected Collection handleQueryByGeneSymbolAndSpecies( String geneOfficialSymbol, String species,
            Collection expressionExperiments ) throws Exception {

        String expressionExperimentIds = parenthesis( expressionExperiments );
        final String queryString = "from DesignElementDataVectorImpl as d " // get DesignElementDataVectorImpl
                + "inner join d.designElement as de " // where de.name='probe_5'";
                + "inner join de.biologicalCharacteristic as bs " // where bs.name='test_bs'";
                + "inner join bs.bioSequence2GeneProduct as b2g "// where b2g.score=1.5";
                + "inner join b2g.geneProduct as gp inner join gp.gene as g "
                + "inner join g.taxon as t where g.officialSymbol = :geneOfficialSymbol and t.commonName = :species "
                + "and d.expressionExperiment.id in " + expressionExperimentIds;

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "geneOfficialSymbol", geneOfficialSymbol );
            queryObject.setParameter( "species", species );
            // queryObject.setParameter( "expressionExperiments", get ids of each expression experiment );
            java.util.List results = queryObject.list();

            if ( results != null ) {
                log.debug( "size: " + results.size() );
                for ( Object obj : results ) {
                    log.debug( obj );
                }
            }

            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * Gets all the genes that are related to the DesignElementDataVector identified by the given ID.
     * 
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesById( long id ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id = :id ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", id );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }

    /**
     * Gets all the genes that are related to the DesignElementDataVector.
     * 
     * @param designElementDataVector
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenes( DesignElementDataVector dedv ) throws Exception {
        return this.handleGetGenesById( dedv.getId() );
    }

    /**
     * @param genes
     * @param ees
     * @param qt
     * @return
     * @throws Exception todo: still untested
     */
    protected Collection handleGetGeneCoexpressionPattern( Collection ees, Collection genes, QuantitationType qt )
            throws Exception {
        Collection<DesignElementDataVector> vectors = null;
        final String queryString = "select distinct compositeSequence.designElementDataVectors from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and gene.id in (:collectionOfGenes)"
                + " and compositeSequence.designElementDataVectors.expressionExperiment.id in (:collectionOfEE)"
                + " and compositeSequence.designElementDataVectors.quantitationType.id = :givenQtId";

        // Need to turn given collections into collections of IDs
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( Object e : ees )
            eeIds.add( ( ( ExpressionExperiment ) e ).getId() );

        Collection<Long> geneIds = new ArrayList<Long>();
        for ( Object gene : genes )
            geneIds.add( ( ( Gene ) gene ).getId() );

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setParameterList( "collectionOfGenes", geneIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            vectors = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vectors;
    }

    /**
     * Places objects in the collection in a list surrounded by parenthesis and separated by commas. Useful for placing
     * this String representation of the list in a sql "in" statement. ie. "... in stringReturnedFromThisMethod" will
     * look like "... in (a,b,c)"
     * 
     * @param objects
     * @return String
     */
    private String parenthesis( Collection objects ) {
        // TODO refactor me into a utilities class and use reflection to determine
        // the parameters that should live in the parenthesis.
        String expressionExperimentIds = "";

        Object[] objs = objects.toArray();
        for ( int i = 0; i < objs.length; i++ ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) objs[i];

            if ( StringUtils.isEmpty( expressionExperimentIds ) ) {
                expressionExperimentIds = "(";
            } else {
                expressionExperimentIds = expressionExperimentIds + ee.getId().toString();
                expressionExperimentIds = expressionExperimentIds + ",";
            }
        }

        expressionExperimentIds = expressionExperimentIds + ( ( ExpressionExperiment ) objs[objs.length - 1] ).getId()
                + ")";

        return expressionExperimentIds;
    }

    @Override
    protected void handleThaw( final DesignElementDataVector designElementDataVector ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( designElementDataVector, LockMode.READ );
                designElementDataVector.getBioAssayDimension().getBioAssays().size();
                for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
                    session.update( ba );
                    ba.getSamplesUsed().size();
                    ba.getDerivedDataFiles().size();
                }
                return null;
            }
        }, true );

    }

    @Override
    protected void handleThaw( final Collection designElementDataVectors ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Object object : designElementDataVectors ) {
                    DesignElementDataVector designElementDataVector = ( DesignElementDataVector ) object;
                    session.update( designElementDataVector );
                    designElementDataVector.getBioAssayDimension().getBioAssays().size();
                    for ( BioAssay ba : designElementDataVector.getBioAssayDimension().getBioAssays() ) {
                        session.update( ba );
                        ba.getSamplesUsed().size();
                        ba.getDerivedDataFiles().size();
                    }
                }
                return null;
            }
        }, true );

    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from DesignElementDataVectorImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleGetGenes(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetGenes( Collection dataVectors ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        // implementation details in callback class
        GetGeneCallbackHandler callback = new GetGeneCallbackHandler( dataVectors );
        Map<DesignElementDataVector, Collection<Gene>> geneMap = ( Map ) templ.execute( callback, true );
        return geneMap;
    }

    /**
     * Private helper class that allows a designElementDataVector collection to be used as an argument to a
     * HibernateCallback
     * 
     * @author jsantos
     */
    private class GetGeneCallbackHandler implements org.springframework.orm.hibernate3.HibernateCallback {
        private Collection dataVectors;

        public GetGeneCallbackHandler( Collection dataVectors ) {
            this.dataVectors = dataVectors;
        }

        /**
         * @param dataVectors the dataVectors to set
         */
        public void setDataVectors( Collection dataVectors ) {
            this.dataVectors = dataVectors;
        }

        /**
         * Callback method for HibernateTemplate
         */
        public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
            /*
             * Algorithm: for each 100 designElementDataVectors, do a query to get the associated genes. The results
             * will then be pushed into a map, associating the designElementDataVector with a collection of Genes.
             * Return the map.
             */
            int MAX_COUNTER = 100;
            Map<DesignElementDataVector, Collection<Gene>> geneMap = new HashMap<DesignElementDataVector, Collection<Gene>>();

            ArrayList<Long> idList = new ArrayList<Long>();
            final String queryString = "select dedv, gene from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence, DesignElementDataVectorImpl dedv where gene.products.id=bs2gp.geneProduct.id "
                    + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                    + " and compositeSequence.designElementDataVectors.id=dedv.id " + " and dedv.id in (:ids) ";

            Iterator iter = dataVectors.iterator();
            int counter = 0;
            // get up to the next 100 entries
            while ( iter.hasNext() ) {

                counter = 0;
                idList.clear();
                while ( ( counter < MAX_COUNTER ) && iter.hasNext() ) {
                    idList.add( ( ( DesignElementDataVector ) iter.next() ).getId() );
                    counter++;
                }

                org.hibernate.Query queryObject = session.createQuery( queryString );
                queryObject.setParameterList( "ids", idList );
                // get results and push into hashmap.

                ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
                while ( results.next() ) {
                    DesignElementDataVector dedv = ( DesignElementDataVector ) results.get( 0 );
                    Gene g = ( Gene ) results.get( 1 );
                    // if the key exists, push into collection
                    // if the key does not exist, create and put hashset into the map
                    if ( geneMap.containsKey( dedv ) ) {
                        if ( !( ( Collection<Gene> ) geneMap.get( dedv ) ).add( g ) ) {
                            log.debug( "Failed to add " + g.getName() + ";Duplicate" );
                        }
                    } else {
                        Collection<Gene> genes = new HashSet<Gene>();
                        genes.add( g );
                        geneMap.put( dedv, genes );
                    }
                }
                results.close();
            }
            return geneMap;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleRemoveDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    protected void handleRemoveDataForCompositeSequence( final CompositeSequence compositeSequence ) throws Exception {

        String[] probeCoexpTypes = new String[] { "Mouse", "Human", "Rat", "Other" };

        for ( String type : probeCoexpTypes ) {

            final String dedvRemovalQuery = "from DesignElementDataVectorImpl dedv where dedv.designElement = :cs";

            final String ppcRemoveFirstQuery = "from " + type
                    + "ProbeCoExpressionImpl as p where p.firstVector.designElement = :cs)";
            final String ppcRemoveSecondQuery = "from " + type
                    + "ProbeCoExpressionImpl as p where p.secondVector.designElement = :cs)";

            HibernateTemplate templ = this.getHibernateTemplate();
            templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
                public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                    assert session != null;

                    org.hibernate.Query queryObject = session.createQuery( ppcRemoveFirstQuery );
                    queryObject.setParameter( "cs", compositeSequence );

                    Collection os = queryObject.list();
                    for ( Object obj : os ) {
                        session.delete( obj );
                    }

                    queryObject = session.createQuery( ppcRemoveSecondQuery );
                    queryObject.setParameter( "cs", compositeSequence );
                    os = queryObject.list();
                    for ( Object obj : os ) {
                        session.delete( obj );
                    }

                    queryObject = session.createQuery( dedvRemovalQuery );
                    queryObject.setParameter( "cs", compositeSequence );
                    os = queryObject.list();
                    for ( Object obj : os ) {
                        session.delete( obj );
                    }

                    return null;
                }
            }, true );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleRemoveDataFromQuantitationType(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected void handleRemoveDataFromQuantitationType( final ExpressionExperiment expressionExperiment,
            final QuantitationType quantitationType ) throws Exception {
        final String dedvRemovalQuery = "delete from DesignElementDataVectorImpl as dedv where dedv.expressionExperiment = :ee and dedv.quantitationType = :qt";

        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {

                org.hibernate.Query queryObject = session.createQuery( dedvRemovalQuery );
                queryObject.setParameter( "ee", expressionExperiment );
                queryObject.setParameter( "qt", quantitationType );
                int modified = queryObject.executeUpdate();
                log.info( "Deleted " + modified + " data vector elements" );

                return null;
            }
        } );

    }
}

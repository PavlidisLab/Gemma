/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.CommonQueries;

import javax.annotation.Nullable;
import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.*;

/**
 * @author Paul
 */
@Repository
public class ProcessedExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorDao {

    @Autowired
    public ProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( ProcessedExpressionDataVector.class, sessionFactory );
    }


    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<ProcessedExpressionDataVector> result = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee" )
                .setParameter( "ee", ee )
                .list();
        log.info( String.format( "Loading %d %s took %d ms", result.size(), getElementClass().getSimpleName(), timer.getTime() ) );
        return result;
    }

    @Override
    public List<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee, BioAssayDimension dimension, int offset, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee and dedv.bioAssayDimension = :dimension" )
                .setParameter( "ee", ee )
                .setParameter( "dimension", dimension )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public Map<ProcessedExpressionDataVector, Collection<Long>> getProcessedVectorsAndGenes( @Nullable Collection<ExpressionExperiment> ees, Map<Long, Collection<Long>> cs2gene ) {
        if ( ( ees != null && ees.isEmpty() ) || cs2gene.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();

        // Do not do in clause for experiments, as it can't use the indices
        Query queryObject = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv from ProcessedExpressionDataVector dedv "
                                + "where dedv.designElement.id in ( :cs )"
                                + ( ees != null ? " and dedv.expressionExperiment in :ees" : "" ) )
                .setParameterList( "cs", optimizeParameterList( cs2gene.keySet() ) );
        List<ProcessedExpressionDataVector> results;
        if ( ees != null ) {
            results = listByIdentifiableBatch( queryObject, "ees", ees, 2048 );
        } else {
            //noinspection unchecked
            results = queryObject.list();
        }
        Map<ProcessedExpressionDataVector, Collection<Long>> dedv2genes = new HashMap<>();
        for ( ProcessedExpressionDataVector dedv : results ) {
            Collection<Long> associatedGenes = cs2gene.get( dedv.getDesignElement().getId() );
            if ( !dedv2genes.containsKey( dedv ) ) {
                dedv2genes.put( dedv, associatedGenes );
            } else {
                Collection<Long> mappedGenes = dedv2genes.get( dedv );
                mappedGenes.addAll( associatedGenes );
            }
        }

        if ( timer.getTime() > Math.max( 200, 20 * dedv2genes.size() ) ) {
            log.warn( String.format( "Fetched %d vectors for %d probes in %dms",
                    dedv2genes.size(), cs2gene.size(), timer.getTime() ) );

        }

        return dedv2genes;
    }

    /**
     * Obtain a random sample of processed vectors for the given experiment.
     *
     * @param ee    ee
     * @param limit if {@code >0}, you will get a "random" set of vectors for the experiment
     * @return processed data vectors
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getRandomProcessedVectors( ExpressionExperiment ee, int limit ) {
        if ( limit <= 0 ) {
            return getProcessedVectors( ee );
        }

        StopWatch timer = StopWatch.createStarted();

        Integer availableVectorCount = ee.getNumberOfDataVectors();
        if ( availableVectorCount == null || availableVectorCount == 0 ) {
            log.info( "Experiment does not have vector count populated." );
            // cannot fix this here, because we're read-only.
        }

        //noinspection unchecked
        List<ProcessedExpressionDataVector> result = this.getSessionFactory().getCurrentSession()
                .createQuery( " from ProcessedExpressionDataVector dedv "
                        + "where dedv.expressionExperiment = :ee and dedv.rankByMean > 0.5 order by RAND()" ) // order by rand() works?
                .setParameter( "ee", ee )
                .setMaxResults( limit )
                .list();

        // maybe ranks are not set for some reason; can happen e.g. GeneSpring mangled data.
        if ( result.isEmpty() ) {
            //noinspection unchecked
            result = this.getSessionFactory().getCurrentSession()
                    .createQuery( " from ProcessedExpressionDataVector dedv "
                            + "where dedv.expressionExperiment = :ee order by RAND()" )
                    .setParameter( "ee", ee )
                    .setMaxResults( limit )
                    .list();
        }

        thaw( result ); // needed?

        if ( result.isEmpty() ) {
            log.warn( "Experiment does not have any processed data vectors to display? " + ee );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( String.format( "Fetch %d random vectors from %s took %d ms.", result.size(), ee.getShortName(), timer.getTime() ) );
        }

        return result;
    }

    @Override
    public List<CompositeSequence> getProcessedVectorsDesignElements( ExpressionExperiment ee, BioAssayDimension dimension, int offset, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv.designElement from ProcessedExpressionDataVector dedv "
                                + "where dedv.expressionExperiment = :ee and dedv.bioAssayDimension = :dimension" )
                .setParameter( "ee", ee )
                .setParameter( "dimension", dimension )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {

        Collection<ArrayDesign> arrayDesigns = CommonQueries.getArrayDesignsUsed( expressionExperiments, this.getSessionFactory().getCurrentSession() );

        // this could be further improved by getting probes specific to experiments in batches.
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, arrayDesigns, this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.isEmpty() ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }
        Map<ExpressionExperiment, Map<Gene, Collection<Double>>> result = new HashMap<>();

        for ( Collection<CompositeSequence> batch : batchIdentifiableParameterList( cs2gene.keySet(), 512 ) ) {

            //language=HQL
            //noinspection unchecked
            List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery(
                            "select dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVector dedv "
                                    + "where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) "
                                    + "group by dedv.designElement, dedv.expressionExperiment" )
                    .setParameter( "cs", batch )
                    .setParameterList( "ees", optimizeIdentifiableParameterList( expressionExperiments ) )
                    .list();

            for ( Object[] o : qr ) {
                ExpressionExperiment e = ( ExpressionExperiment ) o[0];
                CompositeSequence d = ( CompositeSequence ) o[1];
                Double rMean = o[2] == null ? Double.NaN : ( Double ) o[2];
                Double rMax = o[3] == null ? Double.NaN : ( Double ) o[3];

                if ( !result.containsKey( e ) ) {
                    result.put( e, new HashMap<>() );
                }

                Map<Gene, Collection<Double>> rMap = result.get( e );

                Collection<Gene> genes4probe = cs2gene.get( d );

                this.addToGene( method, rMap, rMean, rMax, genes4probe );
            }
        }
        return result;
    }

    @Override
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, this.getSessionFactory().getCurrentSession() );
        if ( cs2gene.isEmpty() ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }

        //language=HQL
        //noinspection unchecked
        List<Object[]> qr = this.getSessionFactory().getCurrentSession().createQuery(
                        "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVector dedv "
                                + "where dedv.designElement in (:cs) and dedv.expressionExperiment = :ee "
                                + "group by dedv.designElement, dedv.expressionExperiment" )
                .setParameterList( "cs", optimizeIdentifiableParameterList( cs2gene.keySet() ) )
                .setParameter( "ee", expressionExperiment )
                .list();

        Map<Gene, Collection<Double>> result = new HashMap<>();
        for ( Object[] o : qr ) {
            CompositeSequence d = ( CompositeSequence ) o[0];
            Double rMean = o[1] == null ? Double.NaN : ( Double ) o[1];
            Double rMax = o[2] == null ? Double.NaN : ( Double ) o[2];

            Collection<Gene> genes4probe = cs2gene.get( d );

            this.addToGene( method, result, rMean, rMax, genes4probe );
        }
        return result;

    }

    @Override
    public Map<ProcessedExpressionDataVector, Collection<Long>> getGenes( Collection<ProcessedExpressionDataVector> vectors ) {
        Collection<Long> probes = new ArrayList<>();
        for ( ProcessedExpressionDataVector pedv : vectors ) {
            probes.add( pedv.getDesignElement().getId() );
        }

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probes, this.getSessionFactory().getCurrentSession() );

        Map<ProcessedExpressionDataVector, Collection<Long>> vector2gene = new HashMap<>( cs2gene.size() );
        for ( ProcessedExpressionDataVector pedv : vectors ) {
            vector2gene.put( pedv, cs2gene.getOrDefault( pedv.getDesignElement().getId(), Collections.emptySet() ) );
        }

        return vector2gene;
    }

    private void addToGene( RankMethod method, Map<Gene, Collection<Double>> result, Double rMean, Double rMax,
            Collection<Gene> genes4probe ) {
        for ( Gene gene : genes4probe ) {
            if ( !result.containsKey( gene ) ) {
                result.put( gene, new ArrayList<>() );
            }
            switch ( method ) {
                case mean:
                    result.get( gene ).add( rMean );
                    break;
                case max:
                    result.get( gene ).add( rMax );
                    break;
                default:
                    break;
            }
        }
    }
}

/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.AbstractNamedMatrix;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.NamedMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;

/**
 * For each 'preferred' DesignElementDataVector in the experiment, compute the 'rank' of the expression level. For
 * experiments using multiple array designs, ranks are computed on a per-array basis. Support for generating rank
 * matrices (Raymond)
 * 
 * @author pavlidis
 * @author raymond
 * @version $Id$
 * @spring.bean id="dedvRankService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="devService" ref="designElementDataVectorService"
 */
public class DedvRankService {

    private static Log log = LogFactory.getLog( DedvRankService.class.getName() );

    private ExpressionExperimentService eeService = null;

    private DesignElementDataVectorService devService = null;

    /**
     * MAX - rank is based on the maximum value of the vector.
     */
    public enum Method {
        MAX, MIN, MEAN, MEDIAN, VARIANCE
    };

    public void setDevService( DesignElementDataVectorService devService ) {
        this.devService = devService;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    /**
     * @param ee
     * @param method2
     * @return the vectors that were modified.
     */
    @SuppressWarnings("unchecked")
    public Collection<DesignElementDataVector> computeDevRankForExpressionExperiment( ExpressionExperiment ee,
            Method method ) {

        eeService.thawLite( ee );
        Collection<DesignElementDataVector> vectors = eeService
                .getDesignElementDataVectors( ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( ee ) );

        devService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );

        Collection<DesignElementDataVector> result = new HashSet<DesignElementDataVector>();
        for ( ArrayDesign ad : ( Collection<ArrayDesign> ) this.eeService.getArrayDesignsUsed( ee ) ) {
            Collection<DesignElementDataVector> preferredVectors = computeRanks( ad, builder, method );
            if ( preferredVectors == null ) {
                log.info( "Could not get preferred data vectors, not updating ranks data" );
                continue;
            }

            log.info( "Updating ranks data for " + preferredVectors.size() + " vectors" );
            this.devService.update( preferredVectors );
            result.addAll( preferredVectors );
        }
        return result;

    }

    /**
     * @param genes
     * @param ees
     * @return
     */
    @SuppressWarnings("unchecked")
    public AbstractNamedMatrix getSampleRankMatrix( Collection<Gene> genes, Collection<ExpressionExperiment> ees ) {
        throw new UnsupportedOperationException( "Sorry, this isn't implemented yet" );
        // TODO: finish implementation
        // Collection<AbstractNamedMatrix> rankMatrices = new HashSet<AbstractNamedMatrix>();
        // int count = 1;
        // for ( ExpressionExperiment ee : ees ) {
        // log.info( "Processing " + ee.getShortName() + " (" + count++ + " of " + ees.size() + ")" );
        //
        // eeService.thawLite( ee );
        // Collection<DesignElementDataVector> vectors;
        // try {
        // vectors = eeService.getDesignElementDataVectors( ExpressionDataMatrixBuilder
        // .getUsefulQuantitationTypes( ee ) );
        // } catch ( Exception e ) {
        // log.error( e.getMessage() );
        // log.error( ee.getShortName() + ": Unable to retrieve design element data vectors, skipping..." );
        // continue;
        // }
        //
        // Map<Gene, Collection<DesignElementDataVector>> gene2dedvMap = getGene2DedvMap( ee, vectors );
        //
        // ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
        //
        // for ( ArrayDesign ad : ( Collection<ArrayDesign> ) this.eeService.getArrayDesignsUsed( ee ) ) {
        // ExpressionDataDoubleMatrix intensityMatrix = builder.getIntensity( ad );
        // if ( intensityMatrix == null ) {
        // // can't do it for this experiment!.
        // }
        // AbstractNamedMatrix rankMatrix = computeSampleRanks( intensityMatrix );
        // rankMatrices.add( rankMatrix );
        // }
        // }
        // int columns = 0;
        // int rows = genes.size();
        // for ( AbstractNamedMatrix rankMatrix : rankMatrices ) {
        // columns += rankMatrix.columns();
        // }
        // AbstractNamedMatrix rankMatrix = new DenseDoubleMatrix2DNamed( rows, columns );
        //
        // for ( Gene gene : genes ) {
        //
        // }
        //
        // return null;
    }

    /**
     * @param method
     * @return
     */
    private String getMethodName( Method method ) {
        switch ( method ) {
            case MEDIAN:
                return "median";
            case MEAN:
                return "mean";
            case VARIANCE:
                return "variance";
            case MAX:
                return "max";
            case MIN:
                return "min";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public AbstractNamedMatrix getRankMatrix( Collection<Gene> genes, Collection<ExpressionExperiment> ees,
            Method method ) {
        DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed( genes.size(), ees.size() );
        matrix.setRowNames( new ArrayList<Gene>( genes ) );
        matrix.setColumnNames( new ArrayList<ExpressionExperiment>( ees ) );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.set( i, j, Double.NaN );
            }
        }

        int count = 1;
        EE: for ( ExpressionExperiment ee : ees ) {
            log.info( ee.getShortName() + ": processing " + count++ + " of " + ees.size() );
            eeService.thawLite( ee );
            Collection<DesignElementDataVector> vectors;

            try {
                vectors = eeService.getDesignElementDataVectors( ExpressionDataMatrixBuilder
                        .getUsefulQuantitationTypes( ee ) );
            } catch ( Exception e ) {
                log.error( e.getMessage() );
                log.error( ee.getShortName() + ": Unable to retrieve design element data vectors, skipping..." );
                continue;
            }

            Collection<DesignElementDataVector> rankedVectors;
            if ( method != null && method != Method.MAX ) {
                log.info( "Recomputing ranks as " + getMethodName( method ) + "s" );
                rankedVectors = new HashSet<DesignElementDataVector>();
                devService.thaw( vectors );

                ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
                for ( ArrayDesign ad : ( Collection<ArrayDesign> ) this.eeService.getArrayDesignsUsed( ee ) ) {
                    Collection<DesignElementDataVector> preferredVectors = computeRanks( ad, builder, method );
                    if ( preferredVectors == null ) {
                        log.info( ee.getShortName() + ": Unable to re-compute ranks, skipping" );
                        continue EE;
                    }
                    rankedVectors.addAll( preferredVectors );
                }
            } else {
                log.info( "Using stored ranks (maximums)" );
                rankedVectors = vectors;
            }

            Map<Gene, Collection<DesignElementDataVector>> gene2dedvMap = getGene2DedvMap( ee, rankedVectors );
            log.info( "Loaded design element data vectors" );

            // construct the rank matrix
            int rankCount = 0;
            String line = ee.getShortName();
            int col = matrix.getColIndexByName( ee );
            for ( Gene gene : genes ) {
                int row = matrix.getRowIndexByName( gene );
                line += "\t";
                Double rank;
                // get rank for each vector pertaining to this gene
                List<Double> ranks = new ArrayList<Double>();
                Collection<DesignElementDataVector> vs = gene2dedvMap.get( gene );
                if ( vs == null ) continue;
                for ( DesignElementDataVector dedv : vs ) {
                    ranks.add( dedv.getRank() );
                }
                if ( ranks.size() < 1 ) continue;

                // take the median rank
                Collections.sort( ranks );
                rank = ranks.get( ranks.size() / 2 );
                if ( rank == null ) continue;
                matrix.set( row, col, rank );
                rankCount++;
            }
            log.info( "Saved " + rankCount + " gene ranks" );
        }

        return matrix;
    }

    /**
     * @param ee
     * @param vectors
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<Gene, Collection<DesignElementDataVector>> getGene2DedvMap( ExpressionExperiment ee,
            Collection<DesignElementDataVector> vectors ) {
        QuantitationType qt = ( QuantitationType ) eeService.getPreferredQuantitationType( ee ).iterator().next();
        Map<DesignElementDataVector, Collection<Gene>> dedv2geneMap = devService.getDedv2GenesMap( vectors, qt );

        // invert dedv2geneMap
        Map<Gene, Collection<DesignElementDataVector>> gene2dedvMap = new HashMap<Gene, Collection<DesignElementDataVector>>();
        for ( DesignElementDataVector dedv : dedv2geneMap.keySet() ) {
            Collection<Gene> c = dedv2geneMap.get( dedv );
            for ( Gene gene : c ) {
                Collection<DesignElementDataVector> vs = gene2dedvMap.get( dedv );
                if ( vs == null ) {
                    vs = new HashSet<DesignElementDataVector>();
                    gene2dedvMap.put( gene, vs );
                }
                vs.add( dedv );
            }

        }
        return gene2dedvMap;
    }

    /**
     * @param intensities
     * @return
     */
    protected NamedMatrix<ExpressionDataMatrixRowElement, Object> computeSampleRanks(
            ExpressionDataDoubleMatrix intensities ) {
        DenseDoubleMatrix2DNamed<ExpressionDataMatrixRowElement, Object> rankMatrix = new DenseDoubleMatrix2DNamed<ExpressionDataMatrixRowElement, Object>(
                intensities.rows(), intensities.columns() );
        rankMatrix.setRowNames( intensities.getRowElements() );

        for ( int column = 0; column < intensities.columns(); column++ ) {
            DoubleArrayList columnIntensities = new DoubleArrayList( intensities.rows() );
            for ( int row = 0; row < intensities.rows(); row++ ) {
                Double intensity = intensities.get( row, column );
                if ( intensity != null && !Double.isNaN( intensity ) )
                    columnIntensities.add( intensity );
                else
                    columnIntensities.add( Double.MIN_VALUE );
            }
            DoubleArrayList columnRanks = Rank.rankTransform( columnIntensities );
            for ( int row = 0; row < intensities.rows(); row++ ) {
                Double value = columnRanks.get( row ) / columnRanks.size();
                rankMatrix.set( row, column, value );
            }
        }

        return rankMatrix;
    }

    /**
     * @param ad
     * @param builder
     * @param method
     * @return
     */
    private Collection<DesignElementDataVector> computeRanks( ArrayDesign ad, ExpressionDataMatrixBuilder builder,
            Method method ) {
        log.info( "Processing vectors on " + ad );
        ExpressionDataDoubleMatrix intensities;

        intensities = builder.getIntensity( ad );
        if ( intensities == null ) return null;

        // We don't remove missing values for Affymetrix based on absent/present
        // calls.
        if ( ad.getTechnologyType().equals( TechnologyType.TWOCOLOR ) ) {
            builder.maskMissingValues( intensities, ad );
        }

        DoubleArrayList ranks = getRanks( intensities, method );

        Collection<DesignElementDataVector> preferredVectors = builder.getPreferredDataVectors( ad );
        log.debug( preferredVectors.size() + " vectors" );
        for ( DesignElementDataVector vector : preferredVectors ) {
            DesignElement de = vector.getDesignElement();
            if ( intensities.getRow( de ) == null ) {
                log.warn( "No intensity value for " + de + ", rank for vector will be null" );
                vector.setRank( null );
                continue;
            }
            Integer i = intensities.getRowIndex( de );
            assert i != null;
            double rank = ranks.get( i ) / ranks.size();
            vector.setRank( rank );
        }
        return preferredVectors;
    }

    /**
     * @param intensities
     * @return
     */
    private DoubleArrayList getRanks( ExpressionDataDoubleMatrix intensities, Method method ) {
        log.debug( "Getting ranks" );
        DoubleArrayList result = new DoubleArrayList( intensities.rows() );

        for ( ExpressionDataMatrixRowElement de : intensities.getRowElements() ) {
            double[] rowObj = ArrayUtils.toPrimitive( intensities.getRow( de.getDesignElement() ) );
            double valueForRank = Double.MIN_VALUE;
            if ( rowObj != null ) {
                DoubleArrayList row = new DoubleArrayList( rowObj );
                switch ( method ) {
                    case MIN:
                        valueForRank = DescriptiveWithMissing.min( row );
                        break;
                    case MAX:
                        valueForRank = DescriptiveWithMissing.max( row );
                        break;
                    case MEAN:
                        valueForRank = DescriptiveWithMissing.mean( row );
                        break;
                    case MEDIAN:
                        valueForRank = DescriptiveWithMissing.median( row );
                        break;
                    case VARIANCE:
                        valueForRank = DescriptiveWithMissing.variance( row );
                }

            }
            result.add( valueForRank );
        }

        return Rank.rankTransform( result );
    }

}

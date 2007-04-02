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

import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

/**
 * For each 'preferred' DesignElementDataVector in the experiment, compute the 'rank' of the expression level. For
 * experiments using multiple array designs, ranks are computed on a per-array basis.
 * 
 * @author pavlidis
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
        MAX, MIN, MEAN, MEDIAN
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
     * @return
     */
    @SuppressWarnings("unchecked")
    public void computeDevRankForExpressionExperiment( ExpressionExperiment ee, Method method ) {

        eeService.thawLite( ee );
        Collection<DesignElementDataVector> vectors = eeService.getDesignElementDataVectors( ee,
                ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( ee ) );

        devService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
        for ( ArrayDesign ad : ( Collection<ArrayDesign> ) this.eeService.getArrayDesignsUsed( ee ) ) {

            log.info( "Processing vectors on " + ad );
            ExpressionDataDoubleMatrix intensities = builder.getIntensity( ad );

            // We don't remove missing values for Affymetrix based on absent/present calls.
            if ( ad.getTechnologyType().equals( TechnologyType.TWOCOLOR ) ) {
                builder.maskMissingValues( intensities, ad );
            }

            IntArrayList ranks = getRanks( intensities, method );

            Collection<DesignElementDataVector> preferredVectors = builder.getPreferredDataVectors( ad );
            log.debug( preferredVectors.size() + " vectors" );
            for ( DesignElementDataVector vector : preferredVectors ) {
                DesignElement de = vector.getDesignElement();
                Integer i = intensities.getRowIndex( de );
                assert i != null;
                double rank = ( double ) ranks.get( i ) / ranks.size();
                vector.setRank( rank );
            }

            log.info( "Updating ranks data for " + preferredVectors.size() + " vectors" );
            this.devService.update( preferredVectors );
        }

    }

    /**
     * @param intensities
     * @return
     */
    private IntArrayList getRanks( ExpressionDataDoubleMatrix intensities, Method method ) {
        log.debug( "Getting ranks" );
        DoubleArrayList result = new DoubleArrayList( intensities.rows() );

        for ( ExpressionDataMatrixRowElement de : intensities.getRowElements() ) {
            double[] rowObj = ArrayUtils.toPrimitive( intensities.getRow( de.getDesignElement() ) );
            double valueForRank = Double.NaN;
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
                }
            }
            result.add( valueForRank );
        }

        return Rank.rankTransform( result );
    }

}

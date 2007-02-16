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

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.jet.stat.Descriptive;

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

    private ExpressionExperimentService eeService = null;
    private DesignElementDataVectorService devService = null;

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

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
        for ( ArrayDesign ad : ( Collection<ArrayDesign> ) this.eeService.getArrayDesignsUsed( ee ) ) {

            ExpressionDataDoubleMatrix intensities = builder.getIntensity( ad );

            // We don't remove missing values for Affymetrix based on absent/present calls.
            if ( ad.getTechnologyType().equals( TechnologyType.TWOCOLOR ) ) {
                builder.maskMissingValues( intensities, ad );
            }

            IntArrayList ranks = getRanks( intensities, method );

            Collection<DesignElementDataVector> preferredVectors = builder.getPreferredDataVectors( ad );
            for ( DesignElementDataVector vector : preferredVectors ) {
                DesignElement de = vector.getDesignElement();
                int i = intensities.getRowIndex( de );
                double rank = ( double ) ranks.get( i ) / ranks.size();
                vector.setRank( rank );
            }

            this.devService.update( preferredVectors );
        }

    }

    /**
     * @param intensities
     * @return
     */
    private IntArrayList getRanks( ExpressionDataDoubleMatrix intensities, Method method ) {
        DoubleArrayList result = new DoubleArrayList( intensities.rows() );

        for ( DesignElement de : intensities.getRowElements() ) {
            Double[] rowObj = intensities.getRow( de );
            double valueForRank = Double.NaN;
            if ( rowObj != null ) {
                DoubleArrayList row = new DoubleArrayList( rowObj.length );
                for ( int j = 0; j < rowObj.length; j++ ) {
                    double val = rowObj[j].doubleValue();
                    row.add( val );
                }

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

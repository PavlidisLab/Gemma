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
import java.util.HashSet;
import java.util.List;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
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
        this.eeService.thaw( ee );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( ee );
        for ( ArrayDesign ad : ( Collection<ArrayDesign> ) this.eeService.getArrayDesignsUsed( ee ) ) {

            ExpressionDataDoubleMatrix intensities = builder.getIntensity( ad );
            builder.maskMissingValues( intensities, ad );
            IntArrayList ranks = getRanks( intensities, method );

            Collection<DesignElementDataVector> vectors = getVectors( ee, builder, ad );
            for ( DesignElementDataVector vector : vectors ) {
                DesignElement de = vector.getDesignElement();
                int i = intensities.getRowIndex( de );
                double rank = ( double ) ranks.get( i ) / ranks.size();
                vector.setRank( rank );
            }

            this.devService.update( vectors );
        }

    }

    /**
     * @param ee
     * @param builder
     * @param ad
     * @return
     */
    private Collection<DesignElementDataVector> getVectors( ExpressionExperiment ee,
            ExpressionDataMatrixBuilder builder, ArrayDesign ad ) {
        // get the vectors.
        List<QuantitationType> types = builder.getPreferredQTypes( ad );
        assert types.size() == 1;
        QuantitationType preferredType = types.iterator().next();

        Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
        for ( DesignElementDataVector vector : ee.getDesignElementDataVectors() ) {
            if ( !vector.getQuantitationType().equals( preferredType ) ) continue;
            ArrayDesign adUsed = builder.arrayDesignForVector( vector );
            if ( !adUsed.equals( ad ) ) continue;
            vectors.add( vector );
        }
        return vectors;
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
                        valueForRank = Descriptive.max( row );
                        break;
                    case MEAN:
                        valueForRank = Descriptive.mean( row );
                        break;
                    case MEDIAN:
                        valueForRank = Descriptive.median( row );
                        break;
                }
            }
            result.add( valueForRank );
        }

        return Rank.rankTransform( result );
    }

}

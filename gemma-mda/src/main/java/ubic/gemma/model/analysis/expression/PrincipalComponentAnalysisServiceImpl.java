/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.analysis.Eigenvalue;
import ubic.gemma.model.analysis.Eigenvector;
import ubic.gemma.model.analysis.ProbeLoading;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class PrincipalComponentAnalysisServiceImpl implements PrincipalComponentAnalysisService {
    private static Log log = LogFactory.getLog( PrincipalComponentAnalysisServiceImpl.class );

    @Autowired
    private PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Autowired
    public QuantitationTypeDao quantitationTypeDao;

    public PrincipalComponentAnalysisDao getPrincipalComponentAnalysisDao() {
        return principalComponentAnalysisDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.PrincipalComponentAnalysisService#create(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.basecode.dataStructure.matrix.DoubleMatrix, double[],
     * ubic.basecode.dataStructure.matrix.DoubleMatrix, ubic.gemma.model.expression.bioAssayData.BioAssayDimension, int)
     */
    @Override
    public PrincipalComponentAnalysis create( ExpressionExperiment ee, DoubleMatrix<CompositeSequence, Integer> u,
            double[] eigenvalues, DoubleMatrix<Integer, Integer> v, BioAssayDimension bad, int numComponentsToStore,
            int numLoadingsToStore ) {

        PrincipalComponentAnalysis pca = PrincipalComponentAnalysis.Factory.newInstance();
        int actualNumberOfComponentsStored = Math.min( numComponentsToStore, v.columns() );
        pca.setNumComponentsStored( actualNumberOfComponentsStored );
        pca.setBioAssayDimension( bad );
        pca.setMaxNumProbesPerComponent( numLoadingsToStore );
        pca.setExperimentAnalyzed( ee );

        QuantitationType loadingQt = getLoadingQt();

        /*
         * deal with U. We keep only the first N components for the first M genes.
         */
        for ( int i = 0; i < actualNumberOfComponentsStored; i++ ) {
            List<CompositeSequence> inOrder = u.sortByColumnAbsoluteValues( i, true );

            for ( int j = 0; j < Math.min( u.rows(), numLoadingsToStore ) - 1; j++ ) {
                CompositeSequence probe = inOrder.get( j );
                ProbeLoading plr = ProbeLoading.Factory.newInstance( i + 1, u.getRowByName( probe )[i], j, probe,
                        loadingQt );
                pca.getProbeLoadings().add( plr );
            }

        }

        /*
         * deal with V. note we store all of it.
         */
        ByteArrayConverter bac = new ByteArrayConverter();
        for ( int i = 0; i < v.columns(); i++ ) {
            double[] column = v.getColumn( i );
            byte[] eigenVectorBytes = bac.doubleArrayToBytes( column );
            int componentNumber = i + 1;
            log.debug( componentNumber );
            Eigenvector evec = Eigenvector.Factory.newInstance( componentNumber, eigenVectorBytes );
            pca.getEigenVectors().add( evec );
        }

        /*
         * Deal with eigenvalues; note we store all of them.
         */
        double sum = 0.0;
        List<Eigenvalue> eigv = new ArrayList<Eigenvalue>();
        for ( int i = 0; i < eigenvalues.length; i++ ) {
            double d = eigenvalues[i];
            sum += d;
            Eigenvalue ev = Eigenvalue.Factory.newInstance();
            ev.setComponentNumber( i + 1 );
            ev.setValue( d );
            eigv.add( ev );
        }

        for ( int i = 0; i < eigenvalues.length; i++ ) {
            Eigenvalue eigenvalue = eigv.get( i );
            eigenvalue.setVarianceFraction( eigenvalue.getValue() / sum );
            pca.getEigenValues().add( eigenvalue );
        }

        return this.principalComponentAnalysisDao.create( pca );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.analysis.expression.PrincipalComponentAnalysisService#getTopLoadedProbes(ubic.gemma.model.expression.experiment.ExpressionExperiment, int, int)
     */
    @Override
    public List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count ) {
        PrincipalComponentAnalysis pca = loadForExperiment( ee );
        if ( pca == null ) {
            return new ArrayList<ProbeLoading>();
        }
        if ( component < 1 ) {
            throw new IllegalArgumentException( "Component must be greater than zero" );
        }

        return this.getPrincipalComponentAnalysisDao().getTopLoadedProbes( ee, component, count );

    }

    /**
     * Reuse this ...
     * 
     * @return
     */
    private QuantitationType getLoadingQt() {
        QuantitationType loadingQt = QuantitationType.Factory.newInstance( false, PrimitiveType.DOUBLE,
                GeneralType.QUANTITATIVE, StandardQuantitationType.CORRELATION, ScaleType.LINEAR, false, false, false,
                false, false, "Loading", "Loading of a feature on an eigenvector" );
        return quantitationTypeDao.findOrCreate( loadingQt );
    }

    @Override
    public PrincipalComponentAnalysis loadForExperiment( ExpressionExperiment ee ) {
        return this.principalComponentAnalysisDao.findByExperiment( ee );
    }

    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.principalComponentAnalysisDao.remove( this.loadForExperiment( ee ) );
    }

}

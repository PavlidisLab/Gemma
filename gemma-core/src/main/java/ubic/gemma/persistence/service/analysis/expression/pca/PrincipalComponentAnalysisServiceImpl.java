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
package ubic.gemma.persistence.service.analysis.expression.pca;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.Eigenvalue;
import ubic.gemma.model.analysis.expression.pca.Eigenvector;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author paul
 */
@Service
public class PrincipalComponentAnalysisServiceImpl extends AbstractService<PrincipalComponentAnalysis>
        implements PrincipalComponentAnalysisService {

    private final PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Autowired
    public PrincipalComponentAnalysisServiceImpl( PrincipalComponentAnalysisDao principalComponentAnalysisDao ) {
        super( principalComponentAnalysisDao );
        this.principalComponentAnalysisDao = principalComponentAnalysisDao;
    }

    @Override
    @Transactional
    public PrincipalComponentAnalysis create( ExpressionExperiment ee, DoubleMatrix<CompositeSequence, Integer> u,
            double[] eigenvalues, DoubleMatrix<Integer, BioMaterial> v, BioAssayDimension bad, int numComponentsToStore,
            int numLoadingsToStore ) {

        PrincipalComponentAnalysis pca = PrincipalComponentAnalysis.Factory.newInstance();
        int actualNumberOfComponentsStored = Math.min( numComponentsToStore, v.columns() );
        pca.setNumComponentsStored( actualNumberOfComponentsStored );
        pca.setBioAssayDimension( bad );
        pca.setMaxNumProbesPerComponent( numLoadingsToStore );
        pca.setExperimentAnalyzed( ee );

        /*
         * deal with U. We keep only the first numComponentsToStore components for the first numLoadingsToStore genes.
         */
        for ( int i = 0; i < actualNumberOfComponentsStored; i++ ) {
            List<CompositeSequence> inOrder = u.sortByColumnAbsoluteValues( i, true );

            for ( int j = 0; j < Math.min( u.rows(), numLoadingsToStore ) - 1; j++ ) {
                CompositeSequence probe = inOrder.get( j );
                ProbeLoading plr = ProbeLoading.Factory.newInstance( i + 1, u.getRowByName( probe )[i], j, probe );
                pca.getProbeLoadings().add( plr );
            }

        }

        /*
         * deal with V. note we store all of it.
         */
        for ( int i = 0; i < v.columns(); i++ ) {
            double[] column = v.getColumn( i );
            int componentNumber = i + 1;
            log.debug( componentNumber );
            Eigenvector evec = Eigenvector.Factory.newInstance( componentNumber, column );
            pca.getEigenVectors().add( evec );
        }

        /*
         * Deal with eigenvalues; note we store all of them.
         */
        double sum = 0.0;
        List<Eigenvalue> eigv = new ArrayList<>();
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

    @Override
    @Transactional(readOnly = true)
    public List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count ) {
        PrincipalComponentAnalysis pca = loadForExperiment( ee );
        if ( pca == null ) {
            return new ArrayList<>();
        }
        if ( component < 1 ) {
            throw new IllegalArgumentException( "Component must be greater than zero" );
        }

        return this.principalComponentAnalysisDao.getTopLoadedProbes( ee, component, count );

    }

    @Override
    @Transactional(readOnly = true)
    public PrincipalComponentAnalysis loadForExperiment( ExpressionExperiment ee ) {
        Collection<PrincipalComponentAnalysis> pcas = this.principalComponentAnalysisDao.findByExperiment( ee );
        if ( pcas.size() > 1 )
            log.warn( "Multiple PCAs found for " + ee + ", returning arbitrary one" );
        if ( !pcas.isEmpty() ) {
            return pcas.iterator().next();
        }
        return null;
    }

    @Override
    @Transactional
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.principalComponentAnalysisDao.removeForExperiment(ee);
    }
}

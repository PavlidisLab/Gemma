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
package ubic.gemma.persistence.service.analysis.expression.coexpression;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.List;

/**
 * @author paul
 */
@Repository
public class SampleCoexpressionAnalysisDaoImpl extends AbstractDao<SampleCoexpressionAnalysis>
        implements SampleCoexpressionAnalysisDao {

    private static ByteArrayConverter bac = new ByteArrayConverter();

    @Autowired
    public SampleCoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( SampleCoexpressionAnalysis.class, sessionFactory );
    }

    @Override
    public SampleCoexpressionAnalysis create( DoubleMatrix<BioAssay, BioAssay> matrix, BioAssayDimension bad,
            ExpressionExperiment ee ) {
        /*
         * First remove any old ones for the experiment.
         */
        Collection<SampleCoexpressionAnalysis> old = findAnalysesByExperiment( ee );
        remove( old );

        SampleCoexpressionAnalysis sas = SampleCoexpressionAnalysis.Factory.newInstance();
        sas.setExperimentAnalyzed( ee );

        SampleCoexpressionMatrix scm = SampleCoexpressionMatrix.Factory.newInstance();
        scm.setBioAssayDimension( bad );
        byte[] coexpressionMatrix = bac.doubleMatrixToBytes( matrix.getRawMatrix() );
        scm.setCoexpressionMatrix( coexpressionMatrix );
        sas.setSampleCoexpressionMatrix( scm );
        this.getSession().save( sas );

        return sas;
    }

    private Collection<SampleCoexpressionAnalysis> findAnalysesByExperiment( ExpressionExperiment ee ) {
        return this.findByProperty( "experimentAnalyzed", ee );
    }

    private Collection<SampleCoexpressionMatrix> findByExperiment( ExpressionExperiment ee ) {
        //noinspection unchecked
        return this.getSession().createQuery( "select sa.sampleCoexpressionMatrix"
                + " from SampleCoexpressionAnalysisImpl sa where sa.experimentAnalyzed = :ee" ).setParameter( "ee", ee )
                .list();
    }

    @Override
    public boolean hasAnalysis( ExpressionExperiment ee ) {
        return !this.findByProperty( "experimentAnalyzed", ee ).isEmpty();
    }

    @Override
    public DoubleMatrix<BioAssay, BioAssay> load( ExpressionExperiment ee ) {

        Collection<SampleCoexpressionMatrix> r = findByExperiment( ee );

        if ( r.isEmpty() )
            return null;

        if ( r.size() > 1 ) {
            log.warn( "More than one matrix was available, only the first is being returned." );
        }

        SampleCoexpressionMatrix matObj = r.iterator().next();

        byte[] matrixBytes = matObj.getCoexpressionMatrix();

        final List<BioAssay> bioAssays = matObj.getBioAssayDimension().getBioAssays();
        int numBa = bioAssays.size();

        if ( numBa == 0 ) {
            throw new IllegalArgumentException(
                    "No bioassays in the bioassaydimension with id=" + matObj.getBioAssayDimension().getId() );
        }

        double[][] rawMatrix;
        try {
            rawMatrix = bac.byteArrayToDoubleMatrix( matrixBytes, numBa );
        } catch ( IllegalArgumentException e ) {
            log.error( "EE id = " + ee.getId() + ": " + e.getMessage() );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> result = new DenseDoubleMatrix<>( rawMatrix );
        try {
            result.setRowNames( bioAssays );
        } catch ( IllegalArgumentException e ) {
            log.error( "EE id = " + ee.getId() + ": " + e.getLocalizedMessage() );
        }
        try {
            result.setColumnNames( bioAssays );
        } catch ( IllegalArgumentException e ) {
            log.error( "EE id = " + ee.getId() + ": " + e.getLocalizedMessage() );
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.remove( this.findByProperty( "experimentAnalyzed", ee ) );
    }

}

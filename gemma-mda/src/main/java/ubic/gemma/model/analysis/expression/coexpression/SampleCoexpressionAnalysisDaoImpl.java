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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.AbstractDao;

/**
 * @author paul
 * @version $Id$
 */
@Repository
public class SampleCoexpressionAnalysisDaoImpl extends AbstractDao<SampleCoexpressionAnalysis> implements
        SampleCoexpressionAnalysisDao {

    private static ByteArrayConverter bac = new ByteArrayConverter();
    private static Log log = LogFactory.getLog( SampleCoexpressionAnalysisDaoImpl.class );

    /**
     * @param sessionFactory
     */
    @Autowired
    public SampleCoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( SampleCoexpressionAnalysisDaoImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrixDao#create(ubic.basecode.dataStructure
     * .matrix.DoubleMatrix, ubic.gemma.model.expression.bioAssayData.BioAssayDimension,
     * ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void create( DoubleMatrix<BioAssay, BioAssay> matrix, BioAssayDimension bad, ExpressionExperiment ee ) {

        /*
         * First delete any old ones for the experiment.
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
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SampleCoexpressionAnalysis> findAnalysesByExperiment( ExpressionExperiment ee ) {
        List<?> r = this.getHibernateTemplate().findByNamedParam(
                " from SampleCoexpressionAnalysisImpl sa where sa.experimentAnalyzed = :ee", "ee", ee );
        return ( Collection<SampleCoexpressionAnalysis> ) r;
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SampleCoexpressionMatrix> findByExperiment( ExpressionExperiment ee ) {
        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "select sa.sampleCoexpressionMatrix"
                        + " from SampleCoexpressionAnalysisImpl sa where sa.experimentAnalyzed = :ee", "ee", ee );
        return ( Collection<SampleCoexpressionMatrix> ) r;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisDao#hasAnalysis(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @Override
    public boolean hasAnalysis( ExpressionExperiment ee ) {
        return !this
                .getHibernateTemplate()
                .findByNamedParam( " from SampleCoexpressionAnalysisImpl sa where sa.experimentAnalyzed = :ee", "ee",
                        ee ).isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrixDao#load(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public DoubleMatrix<BioAssay, BioAssay> load( ExpressionExperiment ee ) {

        Collection<SampleCoexpressionMatrix> r = findByExperiment( ee );

        if ( r.isEmpty() ) return null;

        if ( r.size() > 1 ) {
            log.warn( "More than one matrix was available, only the first is being returned." );
        }

        SampleCoexpressionMatrix matObj = r.iterator().next();

        byte[] matrixBytes = matObj.getCoexpressionMatrix();

        final List<BioAssay> bioAssays = ( List<BioAssay> ) matObj.getBioAssayDimension().getBioAssays();
        int numBa = bioAssays.size();

        double[][] rawMatrix;
        try {
            rawMatrix = bac.byteArrayToDoubleMatrix( matrixBytes, numBa );
        } catch ( IllegalArgumentException e ) {
            log.error( "EE id = " + ee.getId() + ": " + e.getMessage() );
            return null;
        }

        DoubleMatrix<BioAssay, BioAssay> result = new DenseDoubleMatrix<BioAssay, BioAssay>( rawMatrix );
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisDao#remove(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {

        List<?> r = this.getHibernateTemplate().findByNamedParam(
                "select sa " + " from SampleCoexpressionAnalysisImpl sa where sa.experimentAnalyzed = :ee", "ee", ee );

        if ( r.isEmpty() ) return;

        this.remove( ( Collection<? extends SampleCoexpressionAnalysis> ) r );
    }

}

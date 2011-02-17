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
package ubic.gemma.analysis.preprocess.svd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.Distance;
import ubic.basecode.math.KruskalWallis;
import ubic.basecode.util.FileTools;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

/**
 * Perform SVD on expression data and store the results.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class SVDServiceImpl implements SVDService {

    private static final int MAX_EIGEN_GENES_TO_TEST = 5;

    private static Log log = LogFactory.getLog( SVDServiceImpl.class );

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private AuditTrailService auditTrailService;

    private static String EE_SVD_SUMMARY = "SVDSummary";

    private static String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );

    private static String EE_REPORT_DIR = "ExpressionExperimentReports";

    private static String EE_SVD_DIR = "SVD";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Get the SVD information for experiment with id given.
     * 
     * @param id
     * @return
     */
    public SVDValueObject retrieveSvd( Long id ) {

        File f = new File( getReportPath( id ) );

        if ( !f.exists() ) {
            return null;
        }

        try {

            FileInputStream fis = new FileInputStream( getReportPath( id ) );
            ObjectInputStream ois = new ObjectInputStream( fis );

            SVDValueObject valueObject = ( SVDValueObject ) ois.readObject();

            ois.close();
            fis.close();

            return valueObject;
        } catch ( Exception e ) {
            log.warn( "Unable to read report object for id =" + id + ": " + e.getMessage() );
            return null;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.SVDService#svd(java.util.Collection)
     */
    public void svd( Collection<ExpressionExperiment> ees ) {
        for ( ExpressionExperiment ee : ees ) {
            svd( ee );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.SVDService#svd(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public SVDValueObject svd( ExpressionExperiment ee ) {
        assert ee != null;
        Collection<ProcessedExpressionDataVector> vectos = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( vectos );

        ExpressionDataSVD svd = new ExpressionDataSVD( mat );

        /*
         * Save the results
         */
        DoubleMatrix<Integer, Integer> v = svd.getV();
        Double[] vars = svd.getVarianceFractions();

        List<Long> bioMaterialIds = new ArrayList<Long>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            bioMaterialIds.add( mat.getBioMaterialForColumn( i ).getId() );
        }

        SVDValueObject svo = new SVDValueObject( ee.getId(), bioMaterialIds, vars, v );

        saveValueObject( svo );

        /*
         * Add an audit event.
         */
        auditTrailService.addUpdateEvent( ee, PCAAnalysisEvent.class, null, null );

        return svdFactorAnalysis( ee, svo );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.preprocess.svd.SVDService#svdFactorAnalysis(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, ubic.gemma.analysis.preprocess.svd.SVDValueObject)
     */
    public SVDValueObject svdFactorAnalysis( ExpressionExperiment ee, SVDValueObject svo ) {
        DoubleMatrix<Integer, Integer> vMatrix = svo.getvMatrix();

        if ( vMatrix == null || vMatrix.columns() == 0 ) {
            throw new IllegalArgumentException( "SVD must already be run" );
        }

        /*
         * Get bioassay/biomaterial dates and factor mappings
         */
        ExpressionExperiment tee = this.expressionExperimentService.thawLite( ee );

        Collection<BioAssay> bioAssays = tee.getBioAssays();

        Map<Long, Date> bioMaterialDates = new HashMap<Long, Date>();
        Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap = new HashMap<ExperimentalFactor, Map<Long, Double>>();
        Map<ExperimentalFactor, Boolean> isContinuous = new HashMap<ExperimentalFactor, Boolean>();

        for ( BioAssay bioAssay : bioAssays ) {
            Date processingDate = bioAssay.getProcessingDate();
            for ( BioMaterial bm : bioAssay.getSamplesUsed() ) {
                if ( processingDate != null ) bioMaterialDates.put( bm.getId(), processingDate );

                for ( FactorValue fv : bm.getFactorValues() ) {

                    ExperimentalFactor experimentalFactor = fv.getExperimentalFactor();
                    if ( !bioMaterialFactorMap.containsKey( experimentalFactor ) ) {
                        bioMaterialFactorMap.put( experimentalFactor, new HashMap<Long, Double>() );
                    }

                    double valueToStore;
                    if ( fv.getMeasurement() != null ) {
                        try {
                            valueToStore = Double.parseDouble( fv.getMeasurement().getValue() );
                        } catch ( NumberFormatException e ) {
                            log.warn( "Measurement wasn't a number for " + fv );
                            continue;// FIXME
                        }
                        isContinuous.put( experimentalFactor, true );
                    } else {
                        valueToStore = fv.getId().doubleValue();
                        assert !isContinuous.containsKey( experimentalFactor )
                                || !isContinuous.get( experimentalFactor ) : experimentalFactor

                        + " shouldn't be considered continuous?";
                        isContinuous.put( experimentalFactor, false );
                    }
                    bioMaterialFactorMap.get( experimentalFactor ).put( bm.getId(), valueToStore );
                }

            }
        }

        if ( bioMaterialDates.isEmpty() && bioMaterialFactorMap.isEmpty() ) {
            log.warn( "No factor or date information to compare to the eigengenes" );
            return svo;
        }

        Long[] svdBioMaterials = svo.getBioMaterialIds();

        if ( svdBioMaterials == null || svdBioMaterials.length == 0 ) {
            throw new IllegalStateException( "SVD did not have biomaterial information" );
        }
        // since we use rank correlation/anova, we just use the casted ids or dates as the covariate
        for ( int componentNumber = 0; componentNumber < Math.min( vMatrix.columns(), MAX_EIGEN_GENES_TO_TEST ); componentNumber++ ) {

            DoubleArrayList eigenGene = new DoubleArrayList( vMatrix.getColumn( componentNumber ) );

            if ( !bioMaterialDates.isEmpty() ) {
                /*
                 * Get the dates in order, rounded to the nearest hour.
                 */
                double[] dates = new double[svdBioMaterials.length];
                for ( int j = 0; j < svdBioMaterials.length; j++ ) {

                    if ( bioMaterialDates.get( svdBioMaterials[j] ) == null ) {
                        log.warn( "Incomplete date information" );
                    }

                    dates[j] = 1.0 * DateUtils.round( bioMaterialDates.get( svdBioMaterials[j] ), Calendar.HOUR )
                            .getTime(); // make int, cast to double
                }

                double dateCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( dates ) );

                svo.setPCDateCorrelation( componentNumber, dateCorrelation );
            }

            /*
             * For each factor get values in order
             */
            for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
                Map<Long, Double> bmToFv = bioMaterialFactorMap.get( ef );

                double[] fvs = new double[svdBioMaterials.length];
                assert fvs.length > 0;

                for ( int j = 0; j < svdBioMaterials.length; j++ ) {
                    fvs[j] = bmToFv.get( svdBioMaterials[j] ).doubleValue();
                }

                if ( isContinuous.get( ef ) ) {
                    double factorCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList( fvs ) );

                    svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );

                } else {

                    // Do one-way anova (KW)
                    Collection<Integer> groups = new HashSet<Integer>();
                    IntArrayList groupings = new IntArrayList( fvs.length );
                    int k = 0;
                    for ( double d : fvs ) {
                        groupings.add( ( int ) d );
                        groups.add( ( int ) d );
                        k++;
                    }

                    if ( groups.size() < 2 ) {
                        log.warn( "Factor had less than two groups: " + ef + ", SVD comparison can't be done." );
                        continue;
                    }

                    if ( groups.size() == 2 ) {
                        double factorCorrelation = Distance.spearmanRankCorrelation( eigenGene, new DoubleArrayList(
                                fvs ) );
                        svo.setPCFactorCorrelation( componentNumber, ef, factorCorrelation );
                    } else {
                        double kwpval = KruskalWallis.test( eigenGene, groupings );
                        svo.setPCFactorPvalue( componentNumber, ef, kwpval );
                    }

                }
            }
        }
        saveValueObject( svo );
        return svo;
    }

    public static String getReportPath( long id ) {
        return HOME_DIR + File.separatorChar + EE_REPORT_DIR + File.separatorChar + EE_SVD_DIR + File.separatorChar
                + EE_SVD_SUMMARY + "." + id;
    }

    /**
     * Check to see if the top level SVD storage directory exists. If it doesn't, create it, Check to see if the SVD
     * directory exists. If it doesn't, create it.
     * 
     * @param deleteFiles
     */
    private void initDirectories( boolean deleteFiles ) {

        /*
         * See ExpressionExperimentReportServiceImpl; possibly consolidate EE_REPORT_DIR
         */
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + EE_REPORT_DIR );
        File fsvd = FileTools.createDir( HOME_DIR + File.separatorChar + EE_REPORT_DIR + File.separatorChar
                + EE_SVD_DIR );

        if ( deleteFiles ) {
            Collection<File> files = new ArrayList<File>();
            File[] fileArray = fsvd.listFiles();
            for ( File file : fileArray ) {
                files.add( file );
            }
            FileTools.deleteFiles( files );
        }
    }

    private void saveValueObject( SVDValueObject eeVo ) {
        initDirectories( false );
        try {
            // remove old file first
            File f = new File( getReportPath( eeVo.getId() ) );
            if ( f.exists() ) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream( getReportPath( eeVo.getId() ) );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( eeVo );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            log.warn( e );
        }
    }

}

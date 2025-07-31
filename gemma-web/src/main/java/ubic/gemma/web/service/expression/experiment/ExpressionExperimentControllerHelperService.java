package ubic.gemma.web.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.visualization.SingleCellSparsityHeatmap;
import ubic.gemma.core.visualization.cellbrowser.CellBrowserService;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.web.assets.StaticAssetResolver;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.taglib.expression.experiment.ExperimentQCTag;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectStatistics;
import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectType;

@Service
@CommonsLog
public class ExpressionExperimentControllerHelperService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private CellBrowserService cellBrowserService;

    @Autowired
    private StaticAssetResolver staticAssetResolver;

    @Autowired
    private WebEntityUrlBuilder entityUrlBuilder;

    @Autowired
    private ServletContext servletContext;

    @Nullable
    @Transactional(readOnly = true)
    public ExpressionExperimentDetailsValueObject load( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        List<ExpressionExperimentDetailsValueObject> finalResults = expressionExperimentService.loadDetailsValueObjectsByIdsWithCache( Collections.singleton( ee.getId() ) );
        if ( finalResults.isEmpty() ) {
            return null;
        }
        ExpressionExperimentDetailsValueObject finalResult = finalResults.iterator().next();
        expressionExperimentReportService.populateReportInformation( Collections.singleton( finalResult ) );
        expressionExperimentReportService.getAnnotationInformation( Collections.singleton( finalResult ) );
        expressionExperimentReportService.populateEventInformation( Collections.singleton( finalResult ) );

        // Most of DetailsVO values are set automatically through the constructor.
        // We only need to set the additional values:

        finalResult.setQChtml( this.getQCTagHTML( ee ) );
        finalResult.setExpressionExperimentSets( this.getExpressionExperimentSets( ee ) );

        this.setPreferredAndReprocessed( finalResult, ee );
        this.setTechTypeInfo( finalResult, ee );

        this.setPublicationAndAuthor( finalResult, ee );
        this.setBatchInfo( finalResult, ee );

        Date lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( ee );
        if ( lastArrayDesignUpdate != null ) {
            finalResult.setLastArrayDesignUpdateDate( lastArrayDesignUpdate.toString() );
        }

        finalResult.setSuitableForDEA( expressionExperimentService.isSuitableForDEA( ee ) );

        if ( expressionExperimentService.isSingleCell( ee ) ) {
            finalResult.setIsSingleCell( true );
            SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig config = SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                    .includeBioAssays( false )
                    .includeCtas( true )
                    .includeClcs( true )
                    .includeCharacteristics( true )
                    .includeIndices( false )
                    .includeProtocol( true )
                    .build();
            singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee, config )
                    .ifPresent( scd -> {
                        finalResult.setSingleCellDimension( new SingleCellDimensionValueObject( scd, true, true, true ) );
                    } );
            if ( cellBrowserService.hasBrowser( ee ) ) {
                finalResult.setHasCellBrowser( true );
                finalResult.setCellBrowserUrl( cellBrowserService.getBrowserUrl( ee ) );
            }
        }

        return finalResult;
    }

    /**
     * Sets batch information and related properties
     *
     * @param  ee          ee
     * @param  finalResult result
     */
    private void setBatchInfo( ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {
        boolean hasUsableBatchInformation = expressionExperimentBatchInformationService.checkHasUsableBatchInfo( ee );
        finalResult.setHasBatchInformation( hasUsableBatchInformation );
        if ( hasUsableBatchInformation ) {
            finalResult.setBatchConfound( expressionExperimentBatchInformationService.getBatchConfoundAsHtmlString( ee ) );
        }
        BatchEffectDetails details = expressionExperimentBatchInformationService.getBatchEffectDetails( ee );
        finalResult.setBatchEffect( getBatchEffectType( details ).name() );
        finalResult.setBatchEffectStatistics( getBatchEffectStatistics( details ) );
    }

    /**
     * populates the publication and author information
     *
     * @param  ee          ee
     * @param  finalResult result
     */
    private void setPublicationAndAuthor( ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {

        finalResult.setDescription( ee.getDescription() );

        if ( ee.getPrimaryPublication() != null && ee.getPrimaryPublication().getPubAccession() != null ) {
            finalResult.setPrimaryCitation( CitationValueObject.convert2CitationValueObject( ee.getPrimaryPublication() ) );
            String accession = ee.getPrimaryPublication().getPubAccession().getAccession();

            try {
                finalResult.setPubmedId( Integer.parseInt( accession ) );
            } catch ( NumberFormatException e ) {
                log.warn( "Pubmed id not formatted correctly: " + accession );
            }
        }
    }

    /**
     * Checks and sets multiple technology types and RNA-seq status
     *
     * @param  ee          ee
     * @param  finalResult result
     */
    private void setTechTypeInfo( ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {
        Collection<TechnologyType> techTypes = new HashSet<>();
        for ( ArrayDesign ad : expressionExperimentService.getArrayDesignsUsed( ee ) ) {
            techTypes.add( ad.getTechnologyType() );
        }

        finalResult.setHasMultipleTechnologyTypes( techTypes.size() > 1 );

        finalResult.setIsRNASeq( expressionExperimentService.isRNASeq( ee ) );
    }

    /**
     * Check for multiple "preferred" qts and reprocessing.
     *
     * @param  ee          ee
     * @param  finalResult result
     */
    private void setPreferredAndReprocessed( ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        boolean dataReprocessedFromRaw = false;
        int countPreferred = 0;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                countPreferred++;
            }
            if ( qt.getIsRecomputedFromRawData() ) {
                dataReprocessedFromRaw = true;
            }
        }

        finalResult.setHasMultiplePreferredQuantitationTypes( countPreferred > 1 );
        finalResult.setReprocessedFromRawData( dataReprocessedFromRaw );
    }

    /**
     * Used to include the html for the qc table in an ext panel (without using a tag) (This method should probably be
     * in a service?)
     */
    private String getQCTagHTML( ExpressionExperiment ee ) {
        try ( StringWriter sw = new StringWriter() ) {
            ExperimentQCTag qc = new ExperimentQCTag( true );
            qc.setStaticAssetResolver( staticAssetResolver );
            qc.setEntityUrlBuilder( entityUrlBuilder );
            qc.setExpressionExperiment( ee );
            qc.setEeManagerId( ee.getId() + "-eemanager" );
            if ( sampleCoexpressionAnalysisService.hasAnalysis( ee ) ) {
                qc.setHasCorrMat( true );
                qc.setNumOutliersRemoved( this.numOutliersRemoved( ee ) );
                try {
                    qc.setNumPossibleOutliers( this.numPossibleOutliers( ee ) );
                } catch ( ArrayIndexOutOfBoundsException | IllegalStateException e ) {
                    log.error( "Error while setting the number of possible outliers for " + ee + ".", e );
                }
            } else {
                qc.setHasCorrMat( false );
            }
            if ( svdService.hasPca( ee ) ) {
                qc.setHasPCA( svdService.hasPca( ee ) );
                qc.setNumFactors( 3 );
            } else {
                qc.setHasPCA( false );
            }
            qc.setHasMeanVariance( ee.getMeanVarianceRelation() != null );
            Optional<SingleCellDimension> scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee );
            qc.setHasSingleCellData( scd.isPresent() );
            scd.ifPresent( singleCellDimension -> qc.setSingleCellSparsityHeatmap( getSingleCellSparsityHeatmap( ee, singleCellDimension, false ) ) );
            qc.writeQc( new TagWriter( sw ), servletContext.getContextPath() );
            return sw.toString();
        } catch ( IOException | JspException e ) {
            throw new RuntimeException( e );
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> addQCInfo( ExpressionExperiment expressionExperiment ) {
        Map<String, Object> result = new HashMap<>();
        if ( sampleCoexpressionAnalysisService.hasAnalysis( expressionExperiment ) ) {
            result.put( "hasCorrMat", sampleCoexpressionAnalysisService.hasAnalysis( expressionExperiment ) );
            result.put( "numPossibleOutliers", numPossibleOutliers( expressionExperiment ) );
            result.put( "numOutliersRemoved", numOutliersRemoved( expressionExperiment ) );
        } else {
            result.put( "hasCorrMat", false );
        }
        if ( svdService.hasPca( expressionExperiment ) ) {
            result.put( "hasPCA", true );
            result.put( "numFactors", 3 );
        } else {
            result.put( "hasPCA", false );
        }
        result.put( "hasMeanVariance", expressionExperiment.getMeanVarianceRelation() != null );
        Optional<SingleCellDimension> scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( expressionExperiment );
        result.put( "hasSingleCellData", scd.isPresent() );
        scd.ifPresent( singleCellDimension -> result.put( "singleCellSparsityHeatmap", getSingleCellSparsityHeatmap( expressionExperiment, singleCellDimension, false ) ) );
        return result;
    }


    /**
     * How many possible sample outliers are detected?
     */
    private int numPossibleOutliers( ExpressionExperiment ee ) {
        int count;

        if ( ee == null ) {
            log.warn( " Experiment is null " );
            return 0;
        }

        // identify outliers
        if ( !sampleCoexpressionAnalysisService.hasAnalysis( ee ) ) {
            return 0;
        }

        Collection<OutlierDetails> outliers = outlierDetectionService.getOutlierDetails( ee );

        if ( outliers == null ) {
            log.warn( String.format( "%s does not have analysis performed, will return zero.", ee ) );
            return 0;
        }

        count = outliers.size();

        if ( count > 0 ) log.debug( count + " possible outliers detected." );

        return count;
    }

    /**
     * How many possible sample outliers were removed?
     */
    private int numOutliersRemoved( ExpressionExperiment ee ) {
        int count = 0;

        if ( ee == null ) {
            log.warn( " Experiment is null " );
            return 0;
        }

        ee = expressionExperimentService.thawLite( ee );
        for ( BioAssay assay : ee.getBioAssays() ) {
            if ( assay.getIsOutlier() != null && assay.getIsOutlier() ) {
                count++;
            }

        }

        if ( count > 0 ) log.info( count + " outliers were removed." );

        return count;
    }

    private SingleCellSparsityHeatmap getSingleCellSparsityHeatmap( ExpressionExperiment expressionExperiment, SingleCellDimension singleCellDimension, boolean transpose ) {
        QuantitationType qt;
        BioAssayDimension dimension;
        if ( ( qt = expressionExperimentService.getProcessedQuantitationType( expressionExperiment ).orElse( null ) ) != null ) {
            dimension = expressionExperimentService.getBioAssayDimension( expressionExperiment, qt, ProcessedExpressionDataVector.class );
        } else if ( ( qt = expressionExperimentService.getPreferredQuantitationType( expressionExperiment ).orElse( null ) ) != null ) {
            // try using the preferred raw vectors (i.e. if post-processing failed for instance)
            dimension = expressionExperimentService.getBioAssayDimension( expressionExperiment, qt, RawExpressionDataVector.class );
        } else {
            log.warn( "No processed quantitation type nor preferred raw quantitation type found for " + expressionExperiment.getShortName() + ", will not generate single-cell sparsity heatmaps." );
            return null;
        }
        if ( dimension == null ) {
            throw new EntityNotFoundException( "No dimension found for " + qt + "." );
        }
        Collection<ExpressionExperimentSubSet> subSets = expressionExperimentService.getSubSetsWithBioAssays( expressionExperiment, dimension );
        Map<BioAssay, Long> designElementsPerSample = expressionExperimentService.getNumberOfDesignElementsPerSample( expressionExperiment );
        SingleCellSparsityHeatmap singleCellSparsityHeatmap = new SingleCellSparsityHeatmap( expressionExperiment, singleCellDimension, dimension, subSets, designElementsPerSample, null );
        singleCellSparsityHeatmap.setTranspose( transpose );
        return singleCellSparsityHeatmap;
    }

    private Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets( BioAssaySet ee ) {

        Collection<Long> eeSetIds = expressionExperimentSetService.findIds( ee );

        if ( eeSetIds.isEmpty() ) {
            return new HashSet<>();
        }

        Collection<ExpressionExperimentSetValueObject> vos = expressionExperimentSetService.loadValueObjectsByIds( eeSetIds );
        Collection<ExpressionExperimentSetValueObject> sVos = new ArrayList<>();

        for ( ExpressionExperimentSetValueObject vo : vos ) {
            if ( !expressionExperimentSetService.isAutomaticallyGenerated( vo.getDescription() ) ) {
                sVos.add( vo );
            }
        }

        return sVos;
    }
}

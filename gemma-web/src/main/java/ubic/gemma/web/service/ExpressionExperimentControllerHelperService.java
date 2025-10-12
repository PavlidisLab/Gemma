package ubic.gemma.web.service;

import lombok.extern.apachecommons.CommonsLog;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.core.visualization.SingleCellSparsityHeatmap;
import ubic.gemma.core.visualization.cellbrowser.CellBrowserService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.web.assets.StaticAssetResolver;
import ubic.gemma.web.controller.expression.experiment.SingleCellExpressionDataModel;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.taglib.expression.experiment.ExperimentQCTag;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private StaticAssetResolver staticAssetResolver;

    @Autowired
    private WebEntityUrlBuilder entityUrlBuilder;

    @Autowired
    private BuildInfo buildInfo;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    @Qualifier("userAgentAnalyzer")
    private Future<UserAgentAnalyzer> uaa;

    @Nullable
    @Transactional(readOnly = true)
    public ExpressionExperimentDetailsValueObject load( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLite( id );
        List<ExpressionExperimentDetailsValueObject> finalResults = expressionExperimentService.loadDetailsValueObjectsByIdsWithCache( Collections.singleton( ee.getId() ) );
        if ( finalResults.isEmpty() ) {
            return null;
        }
        ExpressionExperimentDetailsValueObject finalResult = finalResults.iterator().next();
        expressionExperimentReportService.populateReportInformation( Collections.singleton( finalResult ) );
        expressionExperimentReportService.getAnnotationInformation( Collections.singleton( finalResult ) );
        expressionExperimentReportService.populateEventInformation( Collections.singleton( finalResult ) );

        String font = null;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if ( requestAttributes instanceof ServletRequestAttributes ) {
            font = detectFont( ( ( ServletRequestAttributes ) requestAttributes ).getRequest() );
        }

        // Most of DetailsVO values are set automatically through the constructor.
        // We only need to set the additional values:

        finalResult.setQChtml( this.getQCTagHTML( ee, font ) );
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
            finalResult.setNumberOfCells( ee.getNumberOfCells() );
            singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee )
                    .ifPresent( scd -> finalResult.setNumberOfCellIds( scd.getNumberOfCellIds() ) );
            finalResult.setHasCellBrowser( cellBrowserService.hasBrowser( ee ) );
            finalResult.setCellBrowserUrl( cellBrowserService.getBrowserUrl( ee ) );
        }

        finalResult.setFont( font );

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
    private String getQCTagHTML( ExpressionExperiment ee, @Nullable String font ) {
        try ( StringWriter sw = new StringWriter() ) {
            ExperimentQCTag qc = new ExperimentQCTag( true );
            qc.setStaticAssetResolver( staticAssetResolver );
            qc.setEntityUrlBuilder( entityUrlBuilder );
            qc.setBuildInfo( buildInfo );
            qc.setFont( font );
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
            if ( svdService.hasSvd( ee ) ) {
                qc.setHasPCA( true );
                qc.setNumFactors( 3 );
            } else {
                qc.setHasPCA( false );
            }
            qc.setHasMeanVariance( ee.getMeanVarianceRelation() != null );
            SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig initConfig = SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                    .includeBioAssays( true )
                    .build();
            Optional<SingleCellDimension> scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee, initConfig );
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
        if ( svdService.hasSvd( expressionExperiment ) ) {
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

        Optional<Collection<OutlierDetails>> outliers = outlierDetectionService.getOutlierDetails( ee );
        if ( outliers.isPresent() ) {
            count = outliers.get().size();
            if ( count > 0 ) log.debug( count + " possible outliers detected." );
            return count;
        } else {
            log.warn( String.format( "%s does not have analysis performed, will return zero.", ee ) );
            return 0;
        }
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

        for ( BioAssay assay : ee.getBioAssays() ) {
            if ( assay.getIsOutlier() ) {
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

    private Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets( ExpressionExperiment ee ) {

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

    /**
     * Detect the theme to use for rendering plots.
     * <p>
     * Unfortunately, it is not possible to detect the font used by the browser with perfect accuracy, but we can assume
     * that certain fonts are present on some operating systems.
     */
    @Nullable
    public String detectFont( HttpServletRequest request ) {
        String userAgent = request.getHeader( "User-Agent" );
        if ( StringUtils.isBlank( userAgent ) ) {
            return null;
        }
        UserAgent ua = FutureUtils.get( uaa ).parse( userAgent );
        String osName = ua.getValue( UserAgent.OPERATING_SYSTEM_NAME );
        if ( osName.equals( "Mac OS" ) || osName.equals( "iOS" ) ) {
            return "Avenir";
        } else if ( osName.equals( "Windows NT" ) ) {
            return "Helvetica";
        } else {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public SingleCellExpressionDataModel loadSingleCellExpressionData( Long id, @Nullable Long quantitationTypeId, Long designElementId,
            @Nullable Long[] assayIds, @Nullable Long cellTypeAssignmentId, @Nullable Long cellLevelCharacteristicsId,
            @Nullable Long focusedCharacteristicId, HttpServletRequest request ) {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new );
        CompositeSequence designElement = compositeSequenceService.loadOrFail( designElementId, EntityNotFoundException::new );
        QuantitationType qt;
        if ( quantitationTypeId != null ) {
            qt = quantitationTypeService.loadByIdAndVectorType( quantitationTypeId, ee, SingleCellExpressionDataVector.class );
            if ( qt == null ) {
                throw new EntityNotFoundException( ee.getShortName() + " does not have a single-cell quantitation type with ID " + quantitationTypeId + "." );
            }
        } else {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new EntityNotFoundException( ee.getShortName() + " does not have a preferred single-cell quantitation type." ) );
        }
        Gene gene;
        Collection<Gene> genes = compositeSequenceService.getGenes( designElement );
        if ( genes.size() == 1 ) {
            gene = genes.iterator().next();
        } else {
            gene = null;
        }

        if ( assayIds != null ) {
            // TODO: validate and load requested assays
        }

        Collection<CellTypeAssignment> ctas = singleCellExpressionExperimentService.getCellTypeAssignmentsWithoutIndices( ee, qt );
        Collection<CellLevelCharacteristics> clcs = singleCellExpressionExperimentService.getCellLevelCharacteristicsWithoutIndices( ee, qt );
        Characteristic focusedCharacteristic = null;

        CellTypeAssignment cta;
        if ( cellTypeAssignmentId != null ) {
            cta = ctas.stream().filter( cta2 -> cta2.getId().equals( cellTypeAssignmentId ) ).findFirst()
                    .orElseThrow( () -> new EntityNotFoundException( "No CTA with ID " + cellTypeAssignmentId + "." ) );
            if ( focusedCharacteristicId != null ) {
                focusedCharacteristic = cta.getCellTypes().stream()
                        .filter( ct -> ct.getId().equals( focusedCharacteristicId ) )
                        .findFirst()
                        .orElseThrow( () -> new EntityNotFoundException( "" ) );
            }
        } else {
            cta = null;
        }

        CellLevelCharacteristics clc;
        if ( cellLevelCharacteristicsId != null ) {
            clc = clcs.stream().filter( clc2 -> clc2.getId().equals( cellLevelCharacteristicsId ) ).findFirst()
                    .orElseThrow( () -> new EntityNotFoundException( "No CLC with ID " + cellLevelCharacteristicsId + "." ) );
            if ( focusedCharacteristicId != null ) {
                focusedCharacteristic = clc.getCharacteristics().stream()
                        .filter( ct -> ct.getId().equals( focusedCharacteristicId ) )
                        .findFirst()
                        .orElseThrow( () -> new EntityNotFoundException( "" ) );
            }
        } else {
            clc = null;
        }

        return new SingleCellExpressionDataModel( ee, ctas, clcs, qt, designElement, gene, assayIds, cta, clc, focusedCharacteristic, getKeywords( ee ), detectFont( request ) );
    }

    @Transactional(readOnly = true)
    public String getKeywords( ExpressionExperiment ee ) {
        return expressionExperimentService.getAnnotations( ee ).stream()
                .map( AnnotationValueObject::getTermName )
                .collect( Collectors.joining( "," ) );
    }
}

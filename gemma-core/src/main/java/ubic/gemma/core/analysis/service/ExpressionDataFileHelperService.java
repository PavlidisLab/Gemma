package ubic.gemma.core.analysis.service;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This service provides transactional helpers to retrieve data for {@link ExpressionDataFileService}.
 * @author poirigui
 */
@Service
@Transactional(readOnly = true)
@CommonsLog
class ExpressionDataFileHelperService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService;
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private CoexpressionService gene2geneCoexpressionService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    public Collection<BulkExpressionDataVector> getVectors( ExpressionExperiment ee, QuantitationType type ) {
        log.info( "Retrieving bulk expression data vectors for " + type + "..." );
        Collection<BulkExpressionDataVector> vectors = rawAndProcessedExpressionDataVectorService.findAndThaw( type );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors for " + type + " in " + ee + "." );
        }
        return vectors;
    }

    public ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, boolean filtered ) throws FilteringException {
        log.info( "Retrieving processed data matrix for " + ee + "..." );
        ee = expressionExperimentService.thawLite( ee );
        if ( filtered ) {
            FilterConfig filterConfig = new FilterConfig();
            filterConfig.setIgnoreMinimumSampleThreshold( true );
            filterConfig.setIgnoreMinimumRowsThreshold( true );
            return expressionDataMatrixService.getFilteredMatrix( ee, filterConfig );
        } else {
            return expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        }
    }

    public ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, boolean filtered, Map<CompositeSequence, String[]> geneAnnotations ) throws FilteringException {
        ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered );
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        geneAnnotations.putAll( getGeneAnnotationsAsStringsByProbe( ads ) );
        return matrix;
    }

    public ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, QuantitationType qt, Map<CompositeSequence, String[]> geneAnnotations ) {
        log.info( "Retrieving raw data matrix for " + qt + "..." );
        ee = expressionExperimentService.thawLite( ee );
        ExpressionDataDoubleMatrix matrix = expressionDataMatrixService.getRawExpressionDataMatrix( ee, qt );
        Set<ArrayDesign> ads = matrix.getDesignElements().stream()
                .map( CompositeSequence::getArrayDesign )
                .collect( Collectors.toSet() );
        geneAnnotations.putAll( getGeneAnnotationsAsStringsByProbe( ads ) );
        return matrix;
    }

    public Stream<SingleCellExpressionDataVector> getSingleCellVectors( ExpressionExperiment ee, QuantitationType qt, Map<CompositeSequence, Set<Gene>> cs2gene, AtomicLong numVecs1, int fetchSize ) {
        long numVecs = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
        if ( numVecs == 0 ) {
            throw new IllegalStateException( "There are no vectors for " + qt + " in " + ee + "." );
        }
        cs2gene.putAll( getCs2Gene( ee, qt ) );
        numVecs1.set( numVecs );
        return singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, fetchSize );
    }

    public Collection<SingleCellExpressionDataVector> getSingleCellVectors( ExpressionExperiment ee, QuantitationType qt, Map<CompositeSequence, Set<Gene>> cs2gene ) {
        log.info( "Retrieving single-cell data vectors for " + qt + "..." );
        Collection<SingleCellExpressionDataVector> vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "There are no vectors associated to " + qt + " in " + ee + "." );
        }
        cs2gene.putAll( getCs2Gene( ee, qt ) );
        return vectors;
    }

    public Stream<SingleCellExpressionDataVector> getSingleCellVectors( ExpressionExperiment ee, QuantitationType qt, Map<CompositeSequence, Set<Gene>> cs2gene, AtomicLong numVecs1, Map<BioAssay, Long> nnzBySample, int fetchSize ) {
        long numVecs = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
        if ( numVecs == 0 ) {
            throw new IllegalStateException( "There are no vectors for " + qt + " in " + ee + "." );
        }
        cs2gene.putAll( getCs2Gene( ee, qt ) );
        numVecs1.set( numVecs );
        log.info( "Counting the number of non-zeroes per sample for " + qt + "..." );
        nnzBySample.putAll( singleCellExpressionExperimentService.getNumberOfNonZeroesBySample( ee, qt, fetchSize ) );
        log.info( "Streaming vectors for " + qt + " with a fetch size of " + fetchSize + "." );
        return singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, fetchSize );
    }

    public SingleCellExpressionDataMatrix<?> getSingleCellMatrix( ExpressionExperiment ee, QuantitationType qt, Map<CompositeSequence, Set<Gene>> cs2gene ) {
        StopWatch timer = StopWatch.createStarted();
        log.info( "Retrieving single-cell data matrix for " + qt + "..." );
        SingleCellExpressionDataMatrix<?> matrix = singleCellExpressionExperimentService.getSingleCellExpressionDataMatrix( ee, qt );
        cs2gene.putAll( getCs2Gene( ee, qt ) );
        log.info( "Retrieving single-cell data matrix for " + qt + " took " + timer + "." );
        return matrix;
    }

    private Map<CompositeSequence, Set<Gene>> getCs2Gene( ExpressionExperiment ee, QuantitationType qt ) {
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee, qt, SingleCellExpressionDataVector.class );
        ads = arrayDesignService.thawCompositeSequences( ads );
        return arrayDesignService.getGenesByCompositeSequence( ads );
    }

    /**
     * @return Map of composite sequence ids to an array of strings: [probe name, genes symbol(s), gene Name(s), gemma
     * id(s), ncbi id(s)].
     */
    public Map<CompositeSequence, String[]> getGeneAnnotationsAsStrings( BioAssaySet experimentAnalyzed ) {
        Collection<ArrayDesign> ads = this.expressionExperimentService
                .getArrayDesignsUsed( experimentAnalyzed );
        return getGeneAnnotationsAsStringsByProbe( ads );
    }

    public Map<CompositeSequence, String[]> getGeneAnnotationsAsStringsByProbe( Collection<ArrayDesign> ads ) {
        Map<CompositeSequence, String[]> annotations = new HashMap<>();
        ads = arrayDesignService.thaw( ads );
        for ( ArrayDesign arrayDesign : ads ) {
            try {
                annotations.putAll( arrayDesignAnnotationService.readAnnotationFile( arrayDesign ) );
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to read annotations for " + arrayDesign, e );
            }
        }
        return annotations;
    }

    public Collection<CoexpressionValueObject> getGeneLinks( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );
        Taxon tax = expressionExperimentService.getTaxon( ee );
        assert tax != null;
        Collection<CoexpressionValueObject> geneLinks = gene2geneCoexpressionService.getCoexpression( ee, true );
        if ( geneLinks.isEmpty() ) {
            throw new IllegalStateException( "No coexpression links for this experiment, file will not be created: " + ee );
        }
        return geneLinks;
    }

    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment ee ) {
        return differentialExpressionAnalysisService.getAnalyses( ee );
    }

    public DifferentialExpressionAnalysis getAnalysisById( Long analysisId ) {
        return differentialExpressionAnalysisService.loadOrFail( analysisId );
    }

    public DifferentialExpressionAnalysis getAnalysis( BioAssaySet experimentAnalyzed, DifferentialExpressionAnalysis analysis, Map<CompositeSequence, String[]> geneAnnotations, AtomicBoolean hasSignificantBatchConfound ) {
        geneAnnotations.putAll( getGeneAnnotationsAsStrings( experimentAnalyzed ) );
        ExpressionExperiment ee = experimentForBioAssaySet( experimentAnalyzed );
        hasSignificantBatchConfound.set( expressionExperimentBatchInformationService.hasSignificantBatchConfound( ee ) );

        if ( analysis.getExperimentAnalyzed().getId() == null ) {// this can happen when using -nodb
            analysis.getExperimentAnalyzed().setId( experimentAnalyzed.getId() );
        }

        // It might not be a persistent analysis: using -nodb
        if ( analysis.getId() != null ) {
            analysis = differentialExpressionAnalysisService.thawFully( analysis );
        }

        return analysis;
    }

    private ExpressionExperiment experimentForBioAssaySet( BioAssaySet bas ) {
        ExpressionExperiment ee;
        if ( bas instanceof ExpressionExperimentSubSet ) {
            ee = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            ee = ( ExpressionExperiment ) bas;
        }
        return ee;
    }
}

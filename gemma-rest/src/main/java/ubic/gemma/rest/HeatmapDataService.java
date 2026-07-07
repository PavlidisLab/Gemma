/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.convert.RepresentationConversionUtils;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembles {@link HeatmapDataValueObject} payloads for the
 * {@code GET /datasets/{id}/heatmap-data} endpoint. The service stays out of the
 * legacy {@code sortVectorDataByDesign} / {@code prepareFactorsForFrontEndDisplay} path
 * entirely — no reordering, no colour assignment, no factor-display strings on the wire.
 *
 * @author claude
 */
@Service
@Slf4j
public class HeatmapDataService {

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    /**
     * Build a heatmap payload for {@code ee} given a vector-selection mode. Caller has already
     * resolved {@code ee} via ACL. Exactly one of {@code geneIds}, {@code probeIds},
     * {@code resultSetId}, {@code pcaComponent} should be non-null; if all are null the random
     * sample-N path is used.
     *
     * @param ee           dataset entity (ACL-resolved by caller)
     * @param geneIds      query mode: vectors for these gene IDs
     * @param probeIds     query mode: vectors for these probe (composite-sequence) IDs
     * @param resultSetId  query mode: top-hit vectors from this differential analysis result set
     * @param threshold    p-value threshold for the resultSetId mode (default 0.01)
     * @param pcaComponent query mode: top-loaded vectors for this PCA component (1-based)
     * @param pcaCount     how many probes per PCA component (default 20)
     * @param sampleSize   fallback random-N size when no other mode applies (default 20, max 200)
     * @param encoding     {@code "json"} (default) or {@code "base64f32"} for the matrix encoding
     * @param subSetId     optional {@link ExpressionExperimentSubSet} id; when non-null, the response
     *                     is restricted to that subset's sample columns. The subset must belong to
     *                     {@code ee}; otherwise {@link IllegalArgumentException} is raised.
     * @param quantitationType optional {@link QuantitationType} to source the matrix from. When {@code null} or when
     *                     it resolves to the dataset's processed QT, the processed-data path is used (all selection
     *                     modes). For any other (non-processed) QT the raw vectors for that QT are served instead;
     *                     the {@code geneIds}/{@code probeIds} selection modes and the random-sample fallback
     *                     ({@code sampleSize}) are supported in that case, while {@code resultSetId}/{@code pcaComponent}
     *                     raise {@link IllegalArgumentException} (no raw-vector equivalent). Non-{@code DOUBLE}
     *                     representations (e.g. integer read-counts) are coerced to double.
     * @param maskOutliers when {@code true} (default) the values of assays flagged as outliers are masked to
     *                     {@code NaN}. When {@code false} the stored expression values are returned for those assays
     *                     as well, by reading them back from the stored vectors. The row selection is unchanged either
     *                     way; only the emitted values differ. For a non-processed QT (raw vectors) the stored value is
     *                     always present, so this restores it. For the processed QT it restores whatever is on disk:
     *                     today processed data is masked at creation time so this is usually a no-op, but it becomes
     *                     effective if processed-data creation stops masking outliers on disk (or where a reprocess
     *                     after an outlier flag failed). Legitimate missing values remain {@code NaN} regardless.
     */
    @Transactional(readOnly = true)
    public HeatmapDataValueObject buildHeatmapData(
            ExpressionExperiment ee,
            @Nullable Collection<Long> geneIds,
            @Nullable Collection<Long> probeIds,
            @Nullable Long resultSetId,
            double threshold,
            @Nullable Integer pcaComponent,
            int pcaCount,
            int sampleSize,
            String encoding,
            @Nullable Long subSetId,
            @Nullable QuantitationType quantitationType,
            boolean maskOutliers ) {
        // Resolve the subset (ACL-gated) up front and verify it belongs to this EE; null when no
        // subset was requested.
        ExpressionExperimentSubSet subSet = null;
        Set<Long> subSetBaIds = null;
        if ( subSetId != null ) {
            subSet = expressionExperimentSubSetService.loadWithBioAssays( subSetId );
            if ( subSet == null ) {
                throw new IllegalArgumentException( "ExpressionExperimentSubSet " + subSetId + " not found." );
            }
            ExpressionExperiment src = subSet.getSourceExperiment();
            if ( src == null || src.getId() == null || !src.getId().equals( ee.getId() ) ) {
                throw new IllegalArgumentException(
                        "ExpressionExperimentSubSet " + subSetId + " does not belong to dataset " + ee.getId() + "." );
            }
            subSetBaIds = new HashSet<>();
            for ( ubic.gemma.model.expression.bioAssay.BioAssay ba : subSet.getBioAssays() ) {
                if ( ba.getId() != null ) {
                    subSetBaIds.add( ba.getId() );
                }
            }
        }

        Collection<DoubleVectorValueObject> vectors = resolveVectors(
                ee, geneIds, probeIds, resultSetId, threshold, pcaComponent, pcaCount, sampleSize, quantitationType, maskOutliers );

        HeatmapDataValueObject out = new HeatmapDataValueObject();
        out.setDatasetId( ee.getId() );
        out.setDatasetShortName( ee.getShortName() );

        if ( vectors.isEmpty() ) {
            HeatmapDataValueObject.MatrixSection empty = new HeatmapDataValueObject.MatrixSection();
            empty.setValues( "json".equals( encoding ) ? new double[0][0] : "" );
            empty.setEncoding( encoding );
            empty.setRowsCount( 0 );
            empty.setColsCount( 0 );
            out.setMatrix( empty );
            out.setRows( Collections.emptyList() );
            out.setColumns( Collections.emptyList() );
            out.setFactors( Collections.emptyList() );
            return out;
        }

        // All vectors share the same BioAssayDimension; key off the first.
        DoubleVectorValueObject head = vectors.iterator().next();
        List<BioAssayValueObject> fullBioAssays = head.getBioAssays();

        // When a subset is requested, project the column axis down to the subset's bioAssays only.
        // keptIdx[i] holds the source column index in the full dimension for kept column i.
        int[] keptIdx;
        List<BioAssayValueObject> bioAssays;
        if ( subSetBaIds != null ) {
            List<BioAssayValueObject> kept = new ArrayList<>( subSetBaIds.size() );
            int[] tmpIdx = new int[fullBioAssays.size()];
            int k = 0;
            for ( int i = 0; i < fullBioAssays.size(); i++ ) {
                BioAssayValueObject ba = fullBioAssays.get( i );
                if ( ba.getId() != null && subSetBaIds.contains( ba.getId() ) ) {
                    kept.add( ba );
                    tmpIdx[k++] = i;
                }
            }
            bioAssays = kept;
            keptIdx = java.util.Arrays.copyOf( tmpIdx, k );
        } else {
            bioAssays = fullBioAssays;
            keptIdx = null;
        }

        int rowsCount = vectors.size();
        int colsCount = bioAssays.size();

        // Build the matrix.
        double[][] raw = new double[rowsCount][colsCount];
        int r = 0;
        for ( DoubleVectorValueObject v : vectors ) {
            double[] src = v.getData();
            if ( keptIdx != null ) {
                // Subset path: pull only the kept source indices into the output row.
                int srcLen = src.length;
                for ( int j = 0; j < colsCount; j++ ) {
                    int srcIdx = keptIdx[j];
                    raw[r][j] = srcIdx < srcLen ? src[srcIdx] : Double.NaN;
                }
            } else if ( src.length == colsCount ) {
                System.arraycopy( src, 0, raw[r], 0, colsCount );
            } else {
                // Defensive: vectors should match dimension; pad with NaN if not (shouldn't happen here).
                java.util.Arrays.fill( raw[r], Double.NaN );
                int n = Math.min( src.length, colsCount );
                System.arraycopy( src, 0, raw[r], 0, n );
            }
            r++;
        }

        HeatmapDataValueObject.MatrixSection mat = new HeatmapDataValueObject.MatrixSection();
        if ( "base64f32".equalsIgnoreCase( encoding ) ) {
            mat.setValues( encodeBase64Float32( raw, rowsCount, colsCount ) );
            mat.setEncoding( "base64f32" );
        } else {
            mat.setValues( raw );
            mat.setEncoding( "json" );
        }
        mat.setRowsCount( rowsCount );
        mat.setColsCount( colsCount );
        mat.setQuantitationType( head.getQuantitationType() );
        out.setMatrix( mat );
        out.setQuantitationType( head.getQuantitationType() );

        // Row metadata: resolve gene mapping for each probe in one batched lookup.
        out.setRows( buildRowMetas( vectors ) );

        // Column metadata + per-sample factor-value lookup.
        // Thaw the EE in-place so the experimental design + biomaterial factor-value
        // assignments are walkable within this read-only transaction. (getEntity returns
        // an ACL-resolved but un-thawed entity — thawLite avoids a second loadById hit
        // that the previous loadAndThawLite(id) call did.)
        ExpressionExperiment freshEe = expressionExperimentService.thawLite( ee );
        if ( freshEe == null ) {
            freshEe = ee;
        }
        out.setColumns( buildColumnMetas( bioAssays, freshEe ) );

        // Factors[] — full ExperimentalFactorValueObject (with statements via FactorValueValueObject) +
        // measurements map for continuous factors.
        out.setFactors( buildFactorEntries( freshEe, bioAssays ) );

        return out;
    }

    // ---- vector resolution ---------------------------------------------------------------

    private Collection<DoubleVectorValueObject> resolveVectors(
            ExpressionExperiment ee,
            @Nullable Collection<Long> geneIds,
            @Nullable Collection<Long> probeIds,
            @Nullable Long resultSetId,
            double threshold,
            @Nullable Integer pcaComponent,
            int pcaCount,
            int sampleSize,
            @Nullable QuantitationType quantitationType,
            boolean maskOutliers ) {
        // A non-processed QT is served straight from its raw vectors; the processed cache path
        // (and its diffex / PCA / random selection modes) doesn't apply to it.
        if ( quantitationType != null && !isProcessedQuantitationType( ee, quantitationType ) ) {
            return resolveRawVectors( ee, quantitationType, geneIds, probeIds, resultSetId, pcaComponent, sampleSize, maskOutliers );
        }

        Collection<DoubleVectorValueObject> vectors;
        if ( geneIds != null && !geneIds.isEmpty() ) {
            vectors = processedExpressionDataVectorService.getProcessedDataArrays( ee, geneIds );
        } else if ( probeIds != null && !probeIds.isEmpty() ) {
            Collection<CompositeSequence> probes = compositeSequenceService.load( probeIds );
            if ( probes.isEmpty() ) {
                return Collections.emptyList();
            }
            vectors = processedExpressionDataVectorService.getProcessedDataArraysByProbe( ee, probes );
        } else if ( resultSetId != null ) {
            vectors = processedExpressionDataVectorService.getDiffExVectors( resultSetId, threshold, DatasetVisualizationWebService.MAX_SAMPLE_SIZE );
        } else if ( pcaComponent != null ) {
            Map<ProbeLoading, DoubleVectorValueObject> topLoaded = svdService.getTopLoadedVectors( ee, pcaComponent, pcaCount );
            vectors = topLoaded == null ? Collections.emptyList() : topLoaded.values();
        } else {
            vectors = processedExpressionDataVectorService.getRandomProcessedDataArrays( ee, sampleSize );
        }

        // The processed cache serves vectors with outlier assays already masked to NaN (read-time masking in the
        // DoubleVectorValueObject constructor). When the caller opts out, re-fetch the stored entities and restore the
        // outlier columns from their data.
        if ( !maskOutliers ) {
            return unmaskProcessedVectors( ee, vectors );
        }
        return vectors;
    }

    /**
     * True when {@code qt} is the QT backing the dataset's processed data (so the processed cache path can serve it).
     */
    private boolean isProcessedQuantitationType( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentService.getProcessedQuantitationType( ee )
                .map( pq -> Objects.equals( pq.getId(), qt.getId() ) )
                .orElse( false );
    }

    /**
     * Resolve vectors for a non-processed QT from its raw vectors. The gene / probe selection modes and the
     * random-sample fallback ({@code sampleSize}) are supported. Diffex ({@code resultSetId}) and PCA
     * ({@code pcaComponent}) are derived from the processed-data analyses and have no raw-vector equivalent, so
     * they raise {@link IllegalArgumentException} (surfaced as a 400 by the caller). The random sample is drawn
     * DB-side ({@link RawExpressionDataVectorService#getRandomRawVectors}) so it does not load the whole matrix.
     */
    private Collection<DoubleVectorValueObject> resolveRawVectors(
            ExpressionExperiment ee,
            QuantitationType qt,
            @Nullable Collection<Long> geneIds,
            @Nullable Collection<Long> probeIds,
            @Nullable Long resultSetId,
            @Nullable Integer pcaComponent,
            int sampleSize,
            boolean maskOutliers ) {
        if ( resultSetId != null || pcaComponent != null ) {
            throw new IllegalArgumentException( "The resultSet and pcaComponent selection modes are derived from "
                    + "processed-data analyses and cannot be combined with a non-processed quantitationType; "
                    + "select rows with genes or probes instead, or omit both for a random sample." );
        }

        Collection<RawExpressionDataVector> rawVectors;
        if ( ( geneIds != null && !geneIds.isEmpty() ) || ( probeIds != null && !probeIds.isEmpty() ) ) {
            Collection<CompositeSequence> probes;
            if ( geneIds != null && !geneIds.isEmpty() ) {
                Collection<Gene> genes = geneService.loadThawedLiter( geneIds );
                if ( genes.isEmpty() ) {
                    return Collections.emptyList();
                }
                Map<Gene, Collection<CompositeSequence>> byGene = compositeSequenceService.findByGenes( genes, true );
                probes = new HashSet<>();
                for ( Collection<CompositeSequence> css : byGene.values() ) {
                    probes.addAll( css );
                }
            } else {
                probes = compositeSequenceService.load( probeIds );
            }
            if ( probes.isEmpty() ) {
                return Collections.emptyList();
            }
            rawVectors = rawExpressionDataVectorService.find( probes, qt );
        } else {
            // Random-sample fallback: DB-side random pick over this QT's raw vectors (no whole-matrix load).
            rawVectors = rawExpressionDataVectorService.getRandomRawVectors( qt, sampleSize );
        }
        if ( rawVectors.isEmpty() ) {
            return Collections.emptyList();
        }

        // Coerce non-double representations (e.g. integer read-counts) so the heatmap can render them as doubles.
        if ( qt.getRepresentation() != PrimitiveType.DOUBLE ) {
            try {
                rawVectors = RepresentationConversionUtils.convertVectors( rawVectors, PrimitiveType.DOUBLE, RawExpressionDataVector.class );
            } catch ( QuantitationTypeConversionException e ) {
                throw new IllegalArgumentException( "Quantitation type " + qt.getName() + " cannot be rendered as a "
                        + "heatmap: its " + qt.getRepresentation() + " values cannot be converted to numbers.", e );
            }
        }

        return toDoubleVectors( qt, rawVectors, maskOutliers );
    }

    /**
     * Adapt raw vectors (already DOUBLE-represented) into the {@link DoubleVectorValueObject} shape the downstream
     * matrix / row / column builders consume. Only the fields those builders read are populated: the shared column
     * axis (bioassays, in dimension order), the design element, the gene refs and the QT — the outlier NaN masking
     * mirrors the processed path when {@code maskOutliers} is {@code true}.
     */
    private Collection<DoubleVectorValueObject> toDoubleVectors( QuantitationType qt, Collection<RawExpressionDataVector> vectors, boolean maskOutliers ) {
        // Batch gene mapping for the design elements so each row carries its gene refs.
        Set<CompositeSequence> css = new HashSet<>();
        for ( RawExpressionDataVector v : vectors ) {
            css.add( v.getDesignElement() );
        }
        Map<CompositeSequence, Collection<Gene>> csToGenes = compositeSequenceService.getGenes( css, true );

        // All raw vectors of one QT share a BioAssayDimension; build the column axis VO once from the first.
        BioAssayDimension bad = vectors.iterator().next().getBioAssayDimension();
        BioAssayDimensionValueObject badVo = new BioAssayDimensionValueObject( bad.getId() );
        List<BioAssayValueObject> bavos = new ArrayList<>( bad.getBioAssays().size() );
        for ( BioAssay ba : bad.getBioAssays() ) {
            BioAssayValueObject bavo = new BioAssayValueObject( ba.getId() );
            bavo.setName( ba.getName() );
            bavo.setOutlier( ba.getIsOutlier() );
            bavos.add( bavo );
        }
        badVo.addBioAssays( bavos );

        QuantitationTypeValueObject qtVo = new QuantitationTypeValueObject( qt );

        List<DoubleVectorValueObject> out = new ArrayList<>( vectors.size() );
        for ( RawExpressionDataVector v : vectors ) {
            DoubleVectorValueObject dv = new DoubleVectorValueObject();
            dv.setId( v.getId() );
            CompositeSequence cs = v.getDesignElement();
            CompositeSequenceValueObject csvo = new CompositeSequenceValueObject( cs.getId() );
            csvo.setName( cs.getName() );
            dv.setDesignElement( csvo );
            dv.setBioAssayDimension( badVo );
            dv.setQuantitationType( qtVo );
            double[] data = v.getDataAsDoubles();
            // Mask outlier assays to NaN, matching the processed DoubleVectorValueObject construction path.
            // When maskOutliers is false the stored values are returned for outlier assays too.
            if ( maskOutliers ) {
                for ( int j = 0; j < bavos.size() && j < data.length; j++ ) {
                    if ( bavos.get( j ).isOutlier() ) {
                        data[j] = Double.NaN;
                    }
                }
            }
            dv.setData( data );
            Collection<Gene> genes = csToGenes.get( cs );
            if ( genes != null && !genes.isEmpty() ) {
                dv.setGenes( genes.stream().map( Gene::getId ).collect( Collectors.toList() ) );
            }
            out.add( dv );
        }
        return out;
    }

    /**
     * Restore the stored values for outlier assays on processed-path vectors. The vectors handed in were built by the
     * processed cache, which masks outlier columns to {@code NaN} at read time; here we re-fetch the corresponding
     * {@link ProcessedExpressionDataVector} entities and copy their stored values back into the outlier columns. Row
     * selection is untouched — only outlier-masked values change. Each cached VO is copied before mutation so the
     * shared cache is never disturbed. Legitimate missing values (already {@code NaN} on the stored entity) stay
     * {@code NaN}.
     * <p>
     * IMPORTANT: whether this actually changes anything depends on what is on disk. Today the processed-data creation
     * step overwrites outlier columns with {@code NaN} before persisting
     * ({@code ProcessedExpressionDataVectorCreationHelperServiceImpl#maskOutliers}), and
     * {@code OutlierFlaggingService#markAsMissing} regenerates processed data on every outlier change — so in the
     * normal case the stored entity is already {@code NaN} at the outlier column and this is a no-op (it restores
     * {@code NaN} over {@code NaN}). It becomes effective when the stored value survives: if processed-data creation is
     * changed to stop masking outliers on disk (relying on the read-time mask only), or in the current failure mode
     * where an outlier flag is committed but the subsequent reprocess fails. Kept deliberately for those cases — do
     * not delete as "dead".
     */
    private Collection<DoubleVectorValueObject> unmaskProcessedVectors(
            ExpressionExperiment ee, Collection<DoubleVectorValueObject> maskedVectors ) {
        if ( maskedVectors.isEmpty() ) {
            return maskedVectors;
        }
        QuantitationType processedQt = expressionExperimentService.getProcessedQuantitationType( ee ).orElse( null );
        if ( processedQt == null ) {
            // No processed QT to source values from; leave the masked vectors as-is.
            return maskedVectors;
        }
        // Collect the selected design elements and re-fetch their stored processed entities.
        Set<Long> deIds = new HashSet<>();
        for ( DoubleVectorValueObject v : maskedVectors ) {
            if ( v.getDesignElement() != null && v.getDesignElement().getId() != null ) {
                deIds.add( v.getDesignElement().getId() );
            }
        }
        if ( deIds.isEmpty() ) {
            return maskedVectors;
        }
        Collection<CompositeSequence> css = compositeSequenceService.load( deIds );
        if ( css.isEmpty() ) {
            return maskedVectors;
        }
        Collection<ProcessedExpressionDataVector> entities = processedExpressionDataVectorService.find( css, processedQt );

        // designElementId -> (bioAssayId -> stored value), so restoration is robust to column ordering differences.
        Map<Long, Map<Long, Double>> trueValues = new HashMap<>();
        for ( ProcessedExpressionDataVector v : entities ) {
            CompositeSequence de = v.getDesignElement();
            if ( de == null || de.getId() == null ) {
                continue;
            }
            double[] data = v.getDataAsDoubles();
            List<BioAssay> bas = new ArrayList<>( v.getBioAssayDimension().getBioAssays() );
            Map<Long, Double> byBa = trueValues.computeIfAbsent( de.getId(), k -> new HashMap<>() );
            for ( int k = 0; k < bas.size() && k < data.length; k++ ) {
                Long baId = bas.get( k ).getId();
                if ( baId != null ) {
                    byBa.put( baId, data[k] );
                }
            }
        }

        List<DoubleVectorValueObject> out = new ArrayList<>( maskedVectors.size() );
        for ( DoubleVectorValueObject masked : maskedVectors ) {
            DoubleVectorValueObject copy = masked.copy(); // never mutate the shared-cache VO
            Long deId = copy.getDesignElement() != null ? copy.getDesignElement().getId() : null;
            Map<Long, Double> byBa = deId != null ? trueValues.get( deId ) : null;
            if ( byBa != null ) {
                double[] data = copy.getData();
                List<BioAssayValueObject> bas = copy.getBioAssays();
                for ( int j = 0; j < bas.size() && j < data.length; j++ ) {
                    BioAssayValueObject ba = bas.get( j );
                    if ( ba.isOutlier() && ba.getId() != null ) {
                        Double val = byBa.get( ba.getId() );
                        if ( val != null ) {
                            data[j] = val;
                        }
                    }
                }
                copy.setData( data );
            }
            out.add( copy );
        }
        return out;
    }

    // ---- row metadata --------------------------------------------------------------------

    private List<HeatmapDataValueObject.RowMeta> buildRowMetas( Collection<DoubleVectorValueObject> vectors ) {
        // Batch-load gene entities for all gene IDs across all rows so each GeneRef can carry
        // officialSymbol + name without a per-row round-trip.
        Set<Long> allGeneIds = new HashSet<>();
        for ( DoubleVectorValueObject v : vectors ) {
            if ( v.getGenes() != null ) {
                allGeneIds.addAll( v.getGenes() );
            }
        }
        Map<Long, Gene> geneById = new HashMap<>();
        if ( !allGeneIds.isEmpty() ) {
            Collection<Gene> genes = geneService.loadThawedLiter( allGeneIds );
            for ( Gene g : genes ) {
                geneById.put( g.getId(), g );
            }
        }

        List<HeatmapDataValueObject.RowMeta> rows = new ArrayList<>( vectors.size() );
        for ( DoubleVectorValueObject v : vectors ) {
            HeatmapDataValueObject.RowMeta row = new HeatmapDataValueObject.RowMeta();
            if ( v.getDesignElement() != null ) {
                row.setDesignElementId( v.getDesignElement().getId() );
                row.setDesignElementName( v.getDesignElement().getName() );
            }
            if ( v.getGenes() != null && !v.getGenes().isEmpty() ) {
                List<HeatmapDataValueObject.GeneRef> refs = new ArrayList<>( v.getGenes().size() );
                for ( Long gid : v.getGenes() ) {
                    Gene g = geneById.get( gid );
                    if ( g != null ) {
                        refs.add( new HeatmapDataValueObject.GeneRef( g.getId(), g.getOfficialSymbol(), g.getOfficialName() ) );
                    } else {
                        // Fall back to id-only ref when the gene didn't come back from the batch load.
                        refs.add( new HeatmapDataValueObject.GeneRef( gid, null, null ) );
                    }
                }
                row.setGenes( refs );
            }
            row.setPvalue( v.getPvalue() );
            // Validated flag is set by the caller for diffex paths; left null otherwise.
            rows.add( row );
        }
        return rows;
    }

    // ---- column metadata -----------------------------------------------------------------

    private List<HeatmapDataValueObject.ColumnMeta> buildColumnMetas(
            List<BioAssayValueObject> bioAssays,
            ExpressionExperiment ee ) {

        // Build BioAssay-id -> BioMaterial map by walking the EE's bioassays.
        Map<Long, BioMaterial> baIdToBm = new HashMap<>();
        for ( ubic.gemma.model.expression.bioAssay.BioAssay ba : ee.getBioAssays() ) {
            if ( ba.getId() != null ) {
                baIdToBm.put( ba.getId(), ba.getSampleUsed() );
            }
        }

        List<HeatmapDataValueObject.ColumnMeta> cols = new ArrayList<>( bioAssays.size() );
        for ( BioAssayValueObject ba : bioAssays ) {
            HeatmapDataValueObject.ColumnMeta col = new HeatmapDataValueObject.ColumnMeta();
            col.setBioAssayId( ba.getId() );
            col.setName( ba.getName() );
            col.setOutlier( ba.isOutlier() );  // BA VO ships a boolean primitive; we box it implicitly.

            BioMaterial bm = baIdToBm.get( ba.getId() );
            if ( bm != null ) {
                col.setBioMaterialId( bm.getId() );
                Map<Long, Long> fvIds = new HashMap<>();
                // walk allFactorValues so inherited assignments from sourceBioMaterial chain count.
                for ( FactorValue fv : bm.getAllFactorValues() ) {
                    ExperimentalFactor ef = fv.getExperimentalFactor();
                    if ( ef == null || ef.getId() == null || fv.getId() == null ) {
                        continue;
                    }
                    if ( ef.getType() == FactorType.continuous ) {
                        // Continuous FVs are reported per-factor in `factors[].measurements`, not here.
                        continue;
                    }
                    fvIds.put( ef.getId(), fv.getId() );
                }
                col.setFactorValueIds( fvIds );
            } else {
                col.setFactorValueIds( Collections.emptyMap() );
            }
            cols.add( col );
        }
        return cols;
    }

    // ---- factor catalogue ----------------------------------------------------------------

    private List<HeatmapDataValueObject.FactorEntry> buildFactorEntries(
            ExpressionExperiment ee, List<BioAssayValueObject> bioAssays ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null || ed.getExperimentalFactors() == null || ed.getExperimentalFactors().isEmpty() ) {
            return Collections.emptyList();
        }
        // Map bioMaterial id -> bioAssay ids in the heatmap column axis (a BM may back multiple BAs).
        Map<Long, List<Long>> bmIdToBaIds = new HashMap<>();
        // Re-walk the EE to get the BioMaterial reference for each BA so we can index measurements.
        Map<Long, Long> baIdToBmId = new HashMap<>();
        for ( ubic.gemma.model.expression.bioAssay.BioAssay ba : ee.getBioAssays() ) {
            if ( ba.getId() == null || ba.getSampleUsed() == null ) continue;
            baIdToBmId.put( ba.getId(), ba.getSampleUsed().getId() );
        }
        for ( BioAssayValueObject ba : bioAssays ) {
            Long bmId = baIdToBmId.get( ba.getId() );
            if ( bmId == null ) continue;
            bmIdToBaIds.computeIfAbsent( bmId, k -> new ArrayList<>() ).add( ba.getId() );
        }

        // Pre-index FV-id -> BioMaterial-ids in a single pass over the EE's BAs (each BM walks
        // its source-BM chain once, via getAllFactorValues). Replaces the per-FV × per-BA scan
        // that was ~O(numContinuousFVs × numBioAssays) source-chain probes per request.
        Map<Long, Set<Long>> fvIdToBmIds = new HashMap<>();
        for ( ubic.gemma.model.expression.bioAssay.BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm == null || bm.getId() == null ) continue;
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( fv.getId() == null ) continue;
                fvIdToBmIds.computeIfAbsent( fv.getId(), k -> new HashSet<>() ).add( bm.getId() );
            }
        }

        List<HeatmapDataValueObject.FactorEntry> out = new ArrayList<>( ed.getExperimentalFactors().size() );
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            ExperimentalFactorValueObject vo = new ExperimentalFactorValueObject( ef, true );
            LinkedHashMap<Long, Double> measurements = null;
            if ( ef.getType() == FactorType.continuous ) {
                measurements = new LinkedHashMap<>();
                // For each FV in this continuous factor, look up the BMs carrying it (O(1))
                // and emit per-BA measurement entries.
                for ( FactorValue fv : ef.getFactorValues() ) {
                    if ( fv.getMeasurement() == null || fv.getMeasurement().getValue() == null ) continue;
                    if ( fv.getId() == null ) continue;
                    Double val;
                    try {
                        val = Double.parseDouble( fv.getMeasurement().getValue() );
                    } catch ( NumberFormatException e ) {
                        // Non-numeric continuous measurements show up occasionally; skip.
                        continue;
                    }
                    Set<Long> bmIds = fvIdToBmIds.get( fv.getId() );
                    if ( bmIds == null ) continue;
                    for ( Long bmId : bmIds ) {
                        List<Long> baIds = bmIdToBaIds.get( bmId );
                        if ( baIds != null ) {
                            for ( Long baId : baIds ) {
                                measurements.put( baId, val );
                            }
                        }
                    }
                }
            }
            out.add( new HeatmapDataValueObject.FactorEntry( vo, measurements ) );
        }
        return out;
    }

    // ---- encoding ------------------------------------------------------------------------

    private String encodeBase64Float32( double[][] raw, int rowsCount, int colsCount ) {
        int totalCells = rowsCount * colsCount;
        ByteBuffer buf = ByteBuffer.allocate( totalCells * 4 ).order( ByteOrder.LITTLE_ENDIAN );
        for ( int i = 0; i < rowsCount; i++ ) {
            double[] row = raw[i];
            for ( int j = 0; j < colsCount; j++ ) {
                buf.putFloat( ( float ) row[j] );
            }
        }
        return Base64.getEncoder().encodeToString( buf.array() );
    }

    /**
     * Decode a base64f32 string back into a {@code double[][]} of the given shape. Provided for the
     * test path and any server-side debugging; clients do their own decode.
     */
    public static double[][] decodeBase64Float32( String b64, int rowsCount, int colsCount ) {
        byte[] bytes = Base64.getDecoder().decode( b64 );
        ByteBuffer buf = ByteBuffer.wrap( bytes ).order( ByteOrder.LITTLE_ENDIAN );
        double[][] out = new double[rowsCount][colsCount];
        for ( int i = 0; i < rowsCount; i++ ) {
            for ( int j = 0; j < colsCount; j++ ) {
                out[i][j] = buf.getFloat();
            }
        }
        return out;
    }
}

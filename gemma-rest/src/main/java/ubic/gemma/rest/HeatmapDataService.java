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
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
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
     * @param sampleSize   fallback random-N size when no other mode applies (default 20, max 150)
     * @param encoding     {@code "json"} (default) or {@code "base64f32"} for the matrix encoding
     * @param subSetId     optional {@link ExpressionExperimentSubSet} id; when non-null, the response
     *                     is restricted to that subset's sample columns. The subset must belong to
     *                     {@code ee}; otherwise {@link IllegalArgumentException} is raised.
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
            @Nullable Long subSetId ) {
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
                ee, geneIds, probeIds, resultSetId, threshold, pcaComponent, pcaCount, sampleSize );

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
            int sampleSize ) {
        if ( geneIds != null && !geneIds.isEmpty() ) {
            return processedExpressionDataVectorService.getProcessedDataArrays( ee, geneIds );
        }
        if ( probeIds != null && !probeIds.isEmpty() ) {
            Collection<CompositeSequence> probes = compositeSequenceService.load( probeIds );
            if ( probes.isEmpty() ) {
                return Collections.emptyList();
            }
            return processedExpressionDataVectorService.getProcessedDataArraysByProbe( ee, probes );
        }
        if ( resultSetId != null ) {
            return processedExpressionDataVectorService.getDiffExVectors( resultSetId, threshold, 150 );
        }
        if ( pcaComponent != null ) {
            Map<ProbeLoading, DoubleVectorValueObject> topLoaded = svdService.getTopLoadedVectors( ee, pcaComponent, pcaCount );
            return topLoaded == null ? Collections.emptyList() : topLoaded.values();
        }
        return processedExpressionDataVectorService.getRandomProcessedDataArrays( ee, sampleSize );
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

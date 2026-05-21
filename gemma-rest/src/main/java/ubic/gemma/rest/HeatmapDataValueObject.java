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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire payload for {@code GET /datasets/{id}/heatmap-data}.
 * <p>
 * Ships a raw probe×sample matrix plus per-row and per-column metadata, and a flat catalogue of
 * experimental factors with full statement / measurement fidelity (mirrors the curation-ui
 * {@code Factor} shape). NO server-side ordering decisions, NO colour assignment, NO legend —
 * the client owns those.
 * <p>
 * See {@code HEATMAP_REWRITE_RECCE.md} §5 for the locked schema.
 *
 * @author claude
 */
@Getter
@Setter
@Schema(description = "Raw matrix + metadata payload for client-side heatmap rendering. See /datasets/{id}/heatmap-data.")
public class HeatmapDataValueObject {

    private Long datasetId;
    private String datasetShortName;

    /**
     * The numeric matrix; row-major (one row per probe). {@code values} is either a {@code double[][]}
     * (json encoding) or a base64-encoded string holding little-endian float32 cells (base64f32 encoding).
     * The matrix is the only required block — every other field below is optional and may be {@code null}
     * or omitted; the client is designed to render a bare-matrix payload with no row, column or factor
     * metadata, so a producer that has nothing to say for a given block should leave it out.
     */
    private MatrixSection matrix;

    /**
     * Optional. Per-row metadata; when present, same length as {@code matrix.rowsCount}. Index {@code i}
     * corresponds to {@code matrix.values[i]}. May be {@code null} or empty when the producer has
     * nothing to attach to rows (the client renders an unlabelled axis in that case). Row entries
     * themselves carry only the gene-shaped metadata the producer chose to attach — gene IDs/symbols,
     * a numeric statistic (e.g. p-value), a category, a boolean flag — all individually optional.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RowMeta> rows;

    /**
     * Optional. Per-column metadata in original BioAssayDimension order; when present, same length as
     * {@code matrix.colsCount}. May be {@code null} or empty when the producer ships an axis-less
     * matrix (the client falls back to numeric column indices).
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ColumnMeta> columns;

    /**
     * Optional. Full experimental-factor catalogue: factors, factor values, statements,
     * baseline-relevance. For continuous factors, the per-sample measurements map is on
     * {@link FactorEntry#getMeasurements()} keyed by bioAssayId. {@code null} or empty when the
     * matrix is not tied to a Gemma experimental design (e.g. a generic data-table renderer).
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<FactorEntry> factors;

    /**
     * The quantitation type the matrix is in. {@code null} when the matrix is empty or generic.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private QuantitationTypeValueObject quantitationType;

    @Getter
    @Setter
    @Schema(description = "The matrix section: numeric cells + encoding tag.")
    public static class MatrixSection {

        /**
         * Either {@code double[][]} (encoding=json) or a {@code String} holding base64-encoded
         * little-endian Float32 cells (encoding=base64f32). Typed as {@code Object} so Jackson
         * serialises the actual runtime type without polymorphic ceremony; the {@code encoding}
         * field disambiguates.
         */
        @Schema(description = "double[][] when encoding=json, or a base64 string when encoding=base64f32. "
                + "Use the `encoding` field to pick the deserialisation path.")
        private Object values;

        @Schema(allowableValues = { "json", "base64f32" })
        private String encoding;

        private int rowsCount;
        private int colsCount;

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private QuantitationTypeValueObject quantitationType;
    }

    @Getter
    @Setter
    @Schema(description = "Per-row metadata: probe + gene info + arbitrary gene-shaped annotations (numeric statistic, category, boolean flag).")
    public static class RowMeta {

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long designElementId;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String designElementName;

        /**
         * Genes mapped to this probe. Each entry carries the official symbol, full gene name and
         * stable Gemma ID — the client uses the ID to build a deep link to the gene page.
         * {@code null} / empty when there is no gene mapping or the producer didn't attach gene info.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<GeneRef> genes;

        /**
         * Differential-expression p-value, or any single per-row numeric statistic the producer
         * wants the client to know. {@code null} when not applicable.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Double pvalue;

        /**
         * Diffex "validated" highlight flag. {@code null} on non-diffex requests.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean validated;

        /**
         * Free-form, gene-shaped annotation bag. Keys are short, client-visible labels; values are
         * primitives the client will display alongside the row: {@link Number} for numeric stats
         * (e.g. {@code logFC}, {@code FDR}, {@code rank}), {@link String} for categorical labels
         * (e.g. {@code module="purple"}), {@link Boolean} for flags (e.g. {@code isMultifunctional}).
         * The client renders numeric values as small bars, categories as chips, booleans as ticks.
         * Producers should leave the map {@code null} or omit unwanted keys; the client tolerates
         * any subset.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> annotations;
    }

    @Getter
    @Setter
    @Schema(description = "Per-column metadata: BioAssay + BioMaterial IDs, outlier flag, factor-value assignments. Every field is optional — producers omit what isn't applicable.")
    public static class ColumnMeta {

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long bioAssayId;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long bioMaterialId;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String name;

        /** Tri-state outlier flag — boxed so the producer can leave it {@code null} when unknown. */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean outlier;

        /**
         * factorId -> factorValueId. Categorical factors only. Continuous factor values are absent here;
         * look up {@link FactorEntry#getMeasurements()} by bioAssayId instead. {@code null} / empty when
         * the producer is not attaching a factor-design context.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<Long, Long> factorValueIds;
    }

    /**
     * Compact gene reference: official symbol + full name + ID. The ID is what the client
     * uses to build a link to the gene page (e.g. {@code /gene/{geneId}}). Every field is
     * individually optional — producers ship whatever they have.
     */
    @Getter
    @Setter
    @Schema(description = "Gene reference attached to a row.")
    public static class GeneRef {

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long id;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String officialSymbol;
        /** Full gene name, e.g. "breast cancer 1, early onset". */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String name;

        public GeneRef() {
        }

        public GeneRef( @Nullable Long id, @Nullable String officialSymbol, @Nullable String name ) {
            this.id = id;
            this.officialSymbol = officialSymbol;
            this.name = name;
        }
    }

    /**
     * One {@link ExperimentalFactorValueObject} plus, for continuous factors, the per-sample
     * measurement map (keyed by bioAssayId). Kept on this wrapper rather than on the shared
     * {@code ExperimentalFactorValueObject} so the VO's payload elsewhere stays unchanged.
     */
    @Getter
    @Setter
    @Schema(description = "Experimental factor + per-sample measurements for continuous factors.")
    public static class FactorEntry {

        /**
         * Full factor VO: id, name, description, type, category(+Uri), values (with statements +
         * is_baseline + measurement), baseline_relevance, baseline_relevance_reason.
         */
        private ExperimentalFactorValueObject factor;

        /**
         * bioAssayId -> measurement value. Continuous factors only; null/absent on categorical
         * factors. Sample identity is bioAssayId because the column-axis of the heatmap is keyed
         * by BioAssay (a single BioMaterial may have multiple assays).
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<Long, Double> measurements;

        public FactorEntry() {
        }

        public FactorEntry( ExperimentalFactorValueObject factor, @Nullable LinkedHashMap<Long, Double> measurements ) {
            this.factor = factor;
            this.measurements = measurements;
        }
    }
}

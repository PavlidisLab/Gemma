/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Full structural representation of an {@link ExperimentalDesign}: factors, their factor values (with statements
 * carrying stable database IDs), and the assignment of biomaterials to factor values.
 * <p>
 * The shape is intended for client-side editing flows that need to round-trip individual statements/factor values by
 * ID rather than by content. Sample identity is keyed by {@link BioMaterial} ID; clients can join to the result of
 * {@code /datasets/{id}/samples} via {@code BioAssayValueObject.sample.id}.
 *
 * @author ogan
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentalDesignValueObject extends IdentifiableValueObject<ExperimentalDesign> {

    private static final long serialVersionUID = 1L;

    @Nullable
    private String name;
    @Nullable
    private String description;
    @Nullable
    private String replicateDescription;
    @Nullable
    private String qualityControlDescription;
    @Nullable
    private String normalizationDescription;

    private List<ExperimentalFactorEntry> experimentalFactors = new ArrayList<>();

    /**
     * Many-to-many assignment of {@link BioMaterial}s to {@link FactorValue}s, materialized as a flat list.
     */
    private List<BioMaterialFactorValueAssignment> bioMaterialAssignments = new ArrayList<>();

    public ExperimentalDesignValueObject() {
        super();
    }

    /**
     * Build the design VO from an {@link ExperimentalDesign} and the {@link BioAssay}s of the owning experiment.
     * <p>
     * Must be invoked while a Hibernate session is open, since factor-value characteristics and biomaterial factor
     * value collections are lazily loaded.
     */
    public ExperimentalDesignValueObject( ExperimentalDesign ed, Collection<BioAssay> bioAssays ) {
        super( ed );
        this.name = ed.getName();
        this.description = ed.getDescription();
        this.replicateDescription = ed.getReplicateDescription();
        this.qualityControlDescription = ed.getQualityControlDescription();
        this.normalizationDescription = ed.getNormalizationDescription();

        this.experimentalFactors = ed.getExperimentalFactors().stream()
                .sorted( java.util.Comparator.comparing( ExperimentalFactor::getId,
                        java.util.Comparator.nullsLast( java.util.Comparator.naturalOrder() ) ) )
                .map( ExperimentalFactorEntry::new )
                .collect( Collectors.toList() );

        // Deduplicate by BioMaterial id (a BioMaterial may back multiple BioAssays).
        Set<Long> seen = new LinkedHashSet<>();
        for ( BioAssay ba : bioAssays ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm == null || bm.getId() == null || !seen.add( bm.getId() ) ) {
                continue;
            }
            // getAllFactorValues includes assignments inherited from sourceBioMaterial hierarchy.
            Set<FactorValue> fvs = bm.getAllFactorValues();
            List<Long> fvIds = fvs.stream()
                    .map( FactorValue::getId )
                    .filter( java.util.Objects::nonNull )
                    .sorted()
                    .collect( Collectors.toList() );
            this.bioMaterialAssignments.add( new BioMaterialFactorValueAssignment( bm.getId(), bm.getName(), fvIds ) );
        }
    }

    /**
     * One {@link ExperimentalFactor}, with its factor values inlined.
     */
    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperimentalFactorEntry {

        private Long id;
        @Nullable
        private String name;
        @Nullable
        private String description;

        @Schema(allowableValues = { "categorical", "continuous" })
        private String type;

        @Nullable
        private CharacteristicValueObject category;

        private List<FactorValueBasicValueObject> values = new ArrayList<>();

        public ExperimentalFactorEntry() {
        }

        public ExperimentalFactorEntry( ExperimentalFactor ef ) {
            this.id = ef.getId();
            this.name = ef.getName();
            this.description = ef.getDescription();
            this.type = ef.getType() != null && ef.getType().equals( FactorType.CONTINUOUS ) ? "continuous" : "categorical";
            if ( ef.getCategory() != null ) {
                this.category = new CharacteristicValueObject( ef.getCategory() );
            }
            this.values = ef.getFactorValues().stream()
                    .sorted( java.util.Comparator.comparing( FactorValue::getId,
                            java.util.Comparator.nullsLast( java.util.Comparator.naturalOrder() ) ) )
                    .map( fv -> {
                        FactorValueBasicValueObject vo = new FactorValueBasicValueObject( fv );
                        // factor metadata is redundant here — the parent factor entry already carries it
                        vo.setExperimentalFactorId( null );
                        vo.setExperimentalFactorType( null );
                        vo.setExperimentalFactorCategory( null );
                        return vo;
                    } )
                    .collect( Collectors.toList() );
        }
    }

    /**
     * Assignment of a single {@link BioMaterial} (sample) to its assigned {@link FactorValue}s.
     */
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BioMaterialFactorValueAssignment {

        private Long bioMaterialId;
        @Nullable
        private String bioMaterialName;
        private List<Long> factorValueIds = new ArrayList<>();

        public BioMaterialFactorValueAssignment() {
        }

        public BioMaterialFactorValueAssignment( Long bioMaterialId, @Nullable String bioMaterialName, List<Long> factorValueIds ) {
            this.bioMaterialId = bioMaterialId;
            this.bioMaterialName = bioMaterialName;
            this.factorValueIds = factorValueIds;
        }
    }
}
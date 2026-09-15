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
package ubic.gemma.model.expression.experiment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.genome.Taxon;

import org.springframework.lang.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Hibernate Search 7 indexed root.
 * <p>
 * Deep {@code @IndexedEmbedded} paths from this entity drive the entire
 * "free-text-over-EE" experience: bioAssays.sampleUsed.characteristics.value/valueUri,
 * experimentalDesign.experimentalFactors.factorValues.characteristics.value/valueUri,
 * primaryPublication / otherRelevantPublications -> abstractText / title / authorList,
 * etc. See SEARCH_RECCE.md Section 2.1 for the full field inventory.
 *
 * @author paul
 */
@Entity
@DiscriminatorValue("ExpressionExperiment")
@Indexed
@Slf4j
public class ExpressionExperiment extends BioAssaySet implements SecuredNotChild, Curatable {

    public static final class Factory {
        public static ExpressionExperiment newInstance() {
            return new ExpressionExperiment();
        }
    }

    public static final int MAX_NAME_LENGTH = 255;

    public static final int MAX_BATCH_CONFOUND_LENGTH = 65535;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ACCESSION_FK", columnDefinition = "BIGINT", unique = true)
    private DatabaseEntry accession;

    /**
     * Type of batch effect detected or corrected for. See {@link BatchEffectType} enum for possible values.
     */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "BATCH_EFFECT", columnDefinition = "VARCHAR(255)")
    private BatchEffectType batchEffect;
    /**
     * Summary statistics of the batch effect, if present.
     */
    @Nullable
    @Column(name = "BATCH_EFFECT_STATISTICS", columnDefinition = "VARCHAR(255)")
    private String batchEffectStatistics;
    /**
     * A string describing the batch confound if a batch effect is present and confounded with one of the experimental
     * factor.
     */
    @Nullable
    @Lob
    @Column(name = "BATCH_CONFOUND", columnDefinition = "TEXT")
    private String batchConfound;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "CURATION_DETAILS_FK", columnDefinition = "BIGINT", unique = true)
    private CurationDetails curationDetails = new CurationDetails();

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "EXPERIMENTAL_DESIGN_FK", columnDefinition = "BIGINT", unique = true)
    private ExperimentalDesign experimentalDesign;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "GEEQ_FK", columnDefinition = "BIGINT", unique = true)
    private Geeq geeq;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "MEAN_VARIANCE_RELATION_FK", columnDefinition = "BIGINT", unique = true)
    private MeanVarianceRelation meanVarianceRelation;

    @Nullable
    @Lob
    @Column(name = "METADATA", columnDefinition = "text")
    private String metadata;
    /**
     * TODO: allow this to be null in case there are no processed vectors
     */
    @Column(name = "NUMBER_OF_DATA_VECTORS", columnDefinition = "INTEGER")
    private Integer numberOfDataVectors = 0;
    /**
     * TODO: rename this to numberOfAssays and add a numberOfSamples field that truly reflect the number of associated
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterial}.
     */
    @Column(name = "NUMBER_OF_SAMPLES", columnDefinition = "INTEGER")
    private Integer numberOfSamples = 0;
    @Nullable
    @Column(name = "NUMBER_OF_CELLS", columnDefinition = "INTEGER")
    private Integer numberOfCells;
    @Nullable
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TAXON_FK", columnDefinition = "BIGINT")
    private Taxon taxon;

    /**
     * @return the number of samples (bioassays). If there are multiple platforms used,
     * this number may not be the same as the actual number of biological samples.
     * This is a denormalization to speed up queries; the definitive count is always from this.getBioAssays().size()
     */
    public Integer getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples( Integer numberofSamples ) {
        this.numberOfSamples = numberofSamples;
    }

    /**
     * If this experiment was split off of a larger experiment, link to its relatives.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "EXPRESSION_EXPERIMENT_SPLIT_RELATION",
            joinColumns = @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "OTHER_PART_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "EXPRESSION_EXPERIMENT_OTHER_PART_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<ExpressionExperiment> otherParts = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "QUANTITATION_TYPE_EXPRESSION_EXPERIMENT_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<QuantitationType> quantitationTypes = new HashSet<>();

    @OneToMany(mappedBy = "expressionExperiment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<SingleCellExpressionDataVector> singleCellExpressionDataVectors = new HashSet<>();

    @OneToMany(mappedBy = "expressionExperiment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<RawExpressionDataVector> rawExpressionDataVectors = new HashSet<>();

    @OneToMany(mappedBy = "expressionExperiment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ProcessedExpressionDataVector> processedExpressionDataVectors = new HashSet<>();

    @Column(name = "SHORT_NAME", unique = true, columnDefinition = "VARCHAR(255)")
    private String shortName;

    @Column(name = "SOURCE", columnDefinition = "VARCHAR(255)")
    private String source;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "EXPRESSION_EXPERIMENT2CHARACTERISTIC",
            joinColumns = @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", columnDefinition = "BIGINT", insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "ID", columnDefinition = "BIGINT", insertable = false, updatable = false))
    private Set<Characteristic> allCharacteristics;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "BIO_ASSAY_EXPRESSION_EXPERIMENT_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<BioAssay> bioAssays = new HashSet<>();

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @FullTextField
    public String getName() {
        return super.getName();
    }

    @Override
    @FullTextField(projectable = Projectable.YES)
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<BioAssay> getBioAssays() {
        return bioAssays;
    }

    @Override
    public void setBioAssays( Set<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    @Nullable
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public DatabaseEntry getAccession() {
        return accession;
    }

    @Override
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public BibliographicReference getPrimaryPublication() {
        return super.getPrimaryPublication();
    }

    @Override
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<BibliographicReference> getOtherRelevantPublications() {
        return super.getOtherRelevantPublications();
    }

    @Override
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded(includePaths = { "value", "valueUri" })
    public Set<Characteristic> getCharacteristics() {
        return super.getCharacteristics();
    }

    @Nullable
    public String getBatchConfound() {
        return batchConfound;
    }

    @Nullable
    public BatchEffectType getBatchEffect() {
        return batchEffect;
    }

    @Nullable
    public String getBatchEffectStatistics() {
        return batchEffectStatistics;
    }

    @Override
    public CurationDetails getCurationDetails() {
        return this.curationDetails;
    }

    @Nullable
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    @Nullable
    public Geeq getGeeq() {
        return geeq;
    }

    @Nullable
    public MeanVarianceRelation getMeanVarianceRelation() {
        return this.meanVarianceRelation;
    }

    @Nullable
    public String getMetadata() {
        return metadata;
    }

    /**
     * @return The number of ProcessedExpressionDataVectors associated with this.
     */
    public Integer getNumberOfDataVectors() {
        return this.numberOfDataVectors;
    }

    @Nullable
    public Integer getNumberOfCells() {
        return numberOfCells;
    }

    public Set<ExpressionExperiment> getOtherParts() {
        return otherParts;
    }

    public Set<ProcessedExpressionDataVector> getProcessedExpressionDataVectors() {
        return this.processedExpressionDataVectors;
    }

    public Set<QuantitationType> getQuantitationTypes() {
        return this.quantitationTypes;
    }

    public Set<SingleCellExpressionDataVector> getSingleCellExpressionDataVectors() {
        return singleCellExpressionDataVectors;
    }

    public Set<RawExpressionDataVector> getRawExpressionDataVectors() {
        return this.rawExpressionDataVectors;
    }

    /**
     * @return A brief unique (but optional) human-readable name for the expression experiment. For example in the past
     * we often
     * used names like "alizadeh-lymphoma".
     */
    @KeywordField
    public String getShortName() {
        return this.shortName;
    }

    /**
     * @return string describing how the data was obtained (e.g. direct upload)
     * if it was not from a Accesssion in an ExternalDatabase (e.g. GEO)
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Obtain all characteristics associated to this EE.
     * <p>
     * This relationship is not managed by this entity, so you should only query it.
     */
    public Set<Characteristic> getAllCharacteristics() {
        return allCharacteristics;
    }

    public void setAccession( @Nullable DatabaseEntry accession ) {
        this.accession = accession;
    }

    public void setBatchConfound( @Nullable String batchConfound ) { // FIXME don't use a string for this
        this.batchConfound = batchConfound;
    }

    public void setBatchEffect( @Nullable BatchEffectType batchEffect ) { // FIXME don't use a string for this
        this.batchEffect = batchEffect;
    }

    public void setBatchEffectStatistics( @Nullable String batchEffectStatistics ) {
        this.batchEffectStatistics = batchEffectStatistics;
    }

    @Override
    public void setCurationDetails( CurationDetails curationDetails ) {
        this.curationDetails = curationDetails;
    }

    public void setExperimentalDesign( @Nullable ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setGeeq( @Nullable Geeq geeq ) {
        this.geeq = geeq;
    }

    public void setMeanVarianceRelation( @Nullable MeanVarianceRelation meanVarianceRelation ) {
        this.meanVarianceRelation = meanVarianceRelation;
    }

    public void setMetadata( @Nullable String metadata ) {
        this.metadata = metadata;
    }

    public void setNumberOfDataVectors( Integer numberOfDataVectors ) {
        this.numberOfDataVectors = numberOfDataVectors;
    }

    public void setNumberOfCells( @Nullable Integer numberOfCells ) {
        this.numberOfCells = numberOfCells;
    }

    public void setOtherParts( Set<ExpressionExperiment> otherParts ) {
        this.otherParts = otherParts;
    }

    public void setProcessedExpressionDataVectors(
            Set<ProcessedExpressionDataVector> processedExpressionDataVectors ) {
        this.processedExpressionDataVectors = processedExpressionDataVectors;
    }

    public void setQuantitationTypes( Set<QuantitationType> quantitationTypes ) {
        this.quantitationTypes = quantitationTypes;
    }

    public void setSingleCellExpressionDataVectors( Set<SingleCellExpressionDataVector> singleCellExpressionDataVectors ) {
        this.singleCellExpressionDataVectors = singleCellExpressionDataVectors;
    }

    public void setRawExpressionDataVectors( Set<RawExpressionDataVector> rawExpressionDataVectors ) {
        this.rawExpressionDataVectors = rawExpressionDataVectors;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( String source ) {
        this.source = source;
    }


    public void setAllCharacteristics( Set<Characteristic> allCharacteristics ) {
        this.allCharacteristics = allCharacteristics;
    }

    /**
     * Taxon of this dataset.
     * <p>
     * This is a denormalization to speed up queries. For the definitive taxon, look at the
     * {@code bioAssays.sampleUsed.sourceTaxon}. It's possible that more than one distinct taxa can be found that way
     * such experiments should eventually be split by taxon.
     */
    @Nullable
    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( @Nullable Taxon taxon ) {
        this.taxon = taxon;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getShortName() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExpressionExperiment ) )
            return false;
        ExpressionExperiment that = ( ExpressionExperiment ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else if ( this.getShortName() != null && that.getShortName() != null ) {
            return this.getShortName().equals( that.getShortName() );
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString() + ( shortName != null ? " Short Name=" + shortName : "" );
    }
}

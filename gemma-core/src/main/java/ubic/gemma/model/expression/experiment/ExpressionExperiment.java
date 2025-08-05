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

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.search.annotations.*;
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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author paul
 */
@Indexed
@CommonsLog
public class ExpressionExperiment extends BioAssaySet implements SecuredNotChild, Curatable {

    public static final class Factory {
        public static ExpressionExperiment newInstance() {
            return new ExpressionExperiment();
        }
    }

    public static final int MAX_NAME_LENGTH = 255;

    @Nullable
    private DatabaseEntry accession;

    /**
     * Type of batch effect detected or corrected for. See {@link BatchEffectType} enum for possible values.
     */
    @Nullable
    private BatchEffectType batchEffect;
    /**
     * Summary statistics of the batch effect, if present.
     */
    @Nullable
    private String batchEffectStatistics;
    /**
     * A string describing the batch confound if a batch effect is present and confounded with one of the experimental
     * factor.
     */
    @Nullable
    private String batchConfound;
    private CurationDetails curationDetails = new CurationDetails();
    @Nullable
    private ExperimentalDesign experimentalDesign;
    @Nullable
    private Geeq geeq;
    @Nullable
    private MeanVarianceRelation meanVarianceRelation;
    @Nullable
    private String metadata;
    private Integer numberOfDataVectors = 0;
    private Integer numberOfSamples = 0;
    @Nullable
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
    private Set<ExpressionExperiment> otherParts = new HashSet<>();
    private Set<QuantitationType> quantitationTypes = new HashSet<>();

    private Set<SingleCellExpressionDataVector> singleCellExpressionDataVectors = new HashSet<>();
    private Set<RawExpressionDataVector> rawExpressionDataVectors = new HashSet<>();
    private Set<ProcessedExpressionDataVector> processedExpressionDataVectors = new HashSet<>();
    private String shortName;

    private String source;

    private Set<Characteristic> allCharacteristics;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field(store = Store.YES)
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @IndexedEmbedded
    public Set<BioAssay> getBioAssays() {
        return super.getBioAssays();
    }

    @Nullable
    @IndexedEmbedded
    public DatabaseEntry getAccession() {
        return accession;
    }

    @Override
    @IndexedEmbedded
    public BibliographicReference getPrimaryPublication() {
        return super.getPrimaryPublication();
    }

    @Override
    @IndexedEmbedded
    public Set<BibliographicReference> getOtherRelevantPublications() {
        return super.getOtherRelevantPublications();
    }

    @Override
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
    @Field(analyze = Analyze.NO)
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

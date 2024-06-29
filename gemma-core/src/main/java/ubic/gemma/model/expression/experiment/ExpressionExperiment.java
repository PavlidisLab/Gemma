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
import org.hibernate.proxy.HibernateProxyHelper;
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
import ubic.gemma.model.genome.Taxon;

import ubic.gemma.core.lang.Nullable;
import java.util.HashSet;
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

    private static final long serialVersionUID = -1342753625018841735L;
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
    private ExperimentalDesign experimentalDesign;
    private Geeq geeq;
    private MeanVarianceRelation meanVarianceRelation;
    private String metadata;
    private Integer numberOfDataVectors = 0;
    private Integer numberOfSamples = 0;
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
    private Set<ProcessedExpressionDataVector> processedExpressionDataVectors = new HashSet<>();
    private Set<QuantitationType> quantitationTypes = new HashSet<>();
    private Set<RawExpressionDataVector> rawExpressionDataVectors = new HashSet<>();
    private String shortName;

    private String source;

    private Set<Characteristic> allCharacteristics;

    @Override
    public ExpressionExperimentValueObject createValueObject() {
        return new ExpressionExperimentValueObject( this );
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass") // It does check, just not the classic way.
    @Override
    public boolean equals( Object object ) {
        if ( object == null )
            return false;
        Class<?> thisClass = HibernateProxyHelper.getClassWithoutInitializingProxy( this );
        Class<?> thatClass = HibernateProxyHelper.getClassWithoutInitializingProxy( object );
        if ( !thisClass.equals( thatClass ) )
            return false;

        ExpressionExperiment that = ( ExpressionExperiment ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else if ( this.getShortName() != null && that.getShortName() != null ) {
            return this.getShortName().equals( that.getShortName() );
        }
        return false;
    }

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
    @Override
    @IndexedEmbedded
    public DatabaseEntry getAccession() {
        return super.getAccession();
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

    public String getBatchConfound() {
        return batchConfound;
    }

    public BatchEffectType getBatchEffect() {
        return batchEffect;
    }

    public String getBatchEffectStatistics() {
        return batchEffectStatistics;
    }

    @Override
    public CurationDetails getCurationDetails() {
        return this.curationDetails;
    }

    @IndexedEmbedded
    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    @SuppressWarnings("unused")
    public Geeq getGeeq() {
        return geeq;
    }

    public MeanVarianceRelation getMeanVarianceRelation() {
        return this.meanVarianceRelation;
    }

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

    @Override
    public int hashCode() {
        int result = 1;
        if ( this.getId() != null ) {
            return this.getId().hashCode();
        } else if ( this.getShortName() != null ) {
            return this.getShortName().hashCode();
        }
        return result;
    }

    public void setBatchConfound( String batchConfound ) { // FIXME don't use a string for this
        this.batchConfound = batchConfound;
    }

    public void setBatchEffect( BatchEffectType batchEffect ) { // FIXME don't use a string for this
        this.batchEffect = batchEffect;
    }

    public void setBatchEffectStatistics( String batchEffectStatistics ) {
        this.batchEffectStatistics = batchEffectStatistics;
    }

    @Override
    public void setCurationDetails( CurationDetails curationDetails ) {
        this.curationDetails = curationDetails;
    }

    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    @SuppressWarnings("unused")
    public void setGeeq( Geeq geeq ) {
        this.geeq = geeq;
    }

    public void setMeanVarianceRelation( MeanVarianceRelation meanVarianceRelation ) {
        this.meanVarianceRelation = meanVarianceRelation;
    }

    public void setMetadata( String metadata ) {
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
     * This is a denormalization to speed up queries. For the definitive taxon, look at the bioAssays -{@literal >}
     * sampleUsed -{@literal >} sourceTaxon
     *
     * @return the associated taxon
     */
    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    @Override
    public String toString() {
        return super.toString() + ( shortName != null ? " Short Name=" + shortName : "" );
    }

}

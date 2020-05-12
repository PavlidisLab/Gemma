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

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.proxy.HibernateProxyHelper;

import gemma.gsec.model.SecuredNotChild;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 */
public class ExpressionExperiment extends BioAssaySet implements SecuredNotChild, Curatable {

    public static final class Factory {
        public static ExpressionExperiment newInstance() {
            return new ExpressionExperiment();
        }
    }

    private static final long serialVersionUID = -1342753625018841735L;
    private DatabaseEntry accession;
    private String batchConfound;
    private String batchEffect;
    private CurationDetails curationDetails;
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
    private Collection<ExpressionExperiment> otherParts = new HashSet<>();
    private Collection<ProcessedExpressionDataVector> processedExpressionDataVectors = new HashSet<>();
    private Collection<QuantitationType> quantitationTypes = new HashSet<>();
    private Collection<RawExpressionDataVector> rawExpressionDataVectors = new HashSet<>();
    private String shortName;

    private String source;

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

    public DatabaseEntry getAccession() {
        return this.accession;
    }

    public String getBatchConfound() {
        return batchConfound;
    }

    public String getBatchEffect() {
        return batchEffect;
    }

    @Override
    public Collection<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    @Override
    public CurationDetails getCurationDetails() {
        return this.curationDetails;
    }

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

    public Collection<ExpressionExperiment> getOtherParts() {
        return otherParts;
    }

    public Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors() {
        return this.processedExpressionDataVectors;
    }

    public Collection<QuantitationType> getQuantitationTypes() {
        return this.quantitationTypes;
    }

    public Collection<RawExpressionDataVector> getRawExpressionDataVectors() {
        return this.rawExpressionDataVectors;
    }

    /**
     * @return A brief unique (but optional) human-readable name for the expression experiment. For example in the past
     * we often
     * used names like "alizadeh-lymphoma".
     */
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

    public void setAccession( DatabaseEntry accession ) {
        this.accession = accession;
    }

    public void setBatchConfound( String batchConfound ) { // FIXME don't use a string for this
        this.batchConfound = batchConfound;
    }

    public void setBatchEffect( String batchEffect ) { // FIXME don't use a string for this
        this.batchEffect = batchEffect;
    }

    @Override
    public void setBioAssays( Collection<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
        if ( bioAssays != null )
            this.numberOfSamples = bioAssays.size();
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

    public void setOtherParts( Collection<ExpressionExperiment> otherParts ) {
        this.otherParts = otherParts;
    }

    public void setProcessedExpressionDataVectors(
            Collection<ProcessedExpressionDataVector> processedExpressionDataVectors ) {
        this.processedExpressionDataVectors = processedExpressionDataVectors;
    }

    public void setQuantitationTypes( Collection<QuantitationType> quantitationTypes ) {
        this.quantitationTypes = quantitationTypes;
    }

    public void setRawExpressionDataVectors( Collection<RawExpressionDataVector> rawExpressionDataVectors ) {
        this.rawExpressionDataVectors = rawExpressionDataVectors;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    /**
     * This is a denormalization to speed up queries. For the definitive taxon, look at the bioAssays -> sampleUsed -> sourceTaxon
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
        return super.toString() + " (" + this.getShortName() + ")";
    }

}

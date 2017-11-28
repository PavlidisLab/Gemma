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

import gemma.gsec.model.SecuredNotChild;
import org.hibernate.proxy.HibernateProxyHelper;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author paul
 */
public class ExpressionExperiment extends BioAssaySet implements SecuredNotChild, Curatable {

    private static final long serialVersionUID = -1342753625018841735L;
    private DatabaseEntry accession;
    private CurationDetails curationDetails;
    private ExperimentalDesign experimentalDesign;
    private Geeq geeq;
    private MeanVarianceRelation meanVarianceRelation;
    private Integer numberOfDataVectors;
    private Collection<ProcessedExpressionDataVector> processedExpressionDataVectors = new HashSet<>();
    private Collection<QuantitationType> quantitationTypes = new HashSet<>();
    private LocalFile rawDataFile;
    private Collection<RawExpressionDataVector> rawExpressionDataVectors = new HashSet<>();
    private String shortName;
    private String source;
    private String metadata;
    private String batchEffect;
    private String batchConfound;

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
    public int hashCode() {
        int result = 1;
        if ( this.getId() != null ) {
            return this.getId().hashCode();
        } else if ( this.getShortName() != null ) {
            return this.getShortName().hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + this.getShortName() + ")";
    }

    public DatabaseEntry getAccession() {
        return this.accession;
    }

    public void setAccession( DatabaseEntry accession ) {
        this.accession = accession;
    }

    @Override
    public Collection<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    @Override
    public void setBioAssays( Collection<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    @Override
    public ExpressionExperimentValueObject createValueObject() {
        return new ExpressionExperimentValueObject( this );
    }

    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    @SuppressWarnings("unused")
    public Geeq getGeeq() {
        return geeq;
    }

    @SuppressWarnings("unused")
    public void setGeeq( Geeq geeq ) {
        this.geeq = geeq;
    }

    public MeanVarianceRelation getMeanVarianceRelation() {
        return this.meanVarianceRelation;
    }

    public void setMeanVarianceRelation( MeanVarianceRelation meanVarianceRelation ) {
        this.meanVarianceRelation = meanVarianceRelation;
    }

    /**
     * @return The number of ProcessedExpressionDataVectors associated with this.
     */
    public Integer getNumberOfDataVectors() {
        return this.numberOfDataVectors;
    }

    public void setNumberOfDataVectors( Integer numberOfDataVectors ) {
        this.numberOfDataVectors = numberOfDataVectors;
    }

    public Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors() {
        return this.processedExpressionDataVectors;
    }

    public void setProcessedExpressionDataVectors(
            Collection<ProcessedExpressionDataVector> processedExpressionDataVectors ) {
        this.processedExpressionDataVectors = processedExpressionDataVectors;
    }

    public Collection<QuantitationType> getQuantitationTypes() {
        return this.quantitationTypes;
    }

    public void setQuantitationTypes( Collection<QuantitationType> quantitationTypes ) {
        this.quantitationTypes = quantitationTypes;
    }

    public LocalFile getRawDataFile() {
        return this.rawDataFile;
    }

    public void setRawDataFile( LocalFile rawDataFile ) {
        this.rawDataFile = rawDataFile;
    }

    public Collection<RawExpressionDataVector> getRawExpressionDataVectors() {
        return this.rawExpressionDataVectors;
    }

    public void setRawExpressionDataVectors( Collection<RawExpressionDataVector> rawExpressionDataVectors ) {
        this.rawExpressionDataVectors = rawExpressionDataVectors;
    }

    /**
     * @return A brief unique (but optional) human-readable name for the expression experiment. For example in the past we often
     * used names like "alizadeh-lymphoma".
     */
    public String getShortName() {
        return this.shortName;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    /**
     * @return Represents the site where the data was downloaded from.
     */
    public String getSource() {
        return this.source;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata( String metadata ) {
        this.metadata = metadata;
    }

    @Override
    public CurationDetails getCurationDetails() {
        return this.curationDetails;
    }

    @Override
    public void setCurationDetails( CurationDetails curationDetails ) {
        this.curationDetails = curationDetails;
    }

    public String getBatchConfound() {
        return batchConfound;
    }

    public void setBatchConfound( String batchConfound ) {
        this.batchConfound = batchConfound;
    }

    public String getBatchEffect() {
        return batchEffect;
    }

    public void setBatchEffect( String batchEffect ) {
        this.batchEffect = batchEffect;
    }

    public static final class Factory {

        public static ExpressionExperiment newInstance() {
            return new ExpressionExperiment();
        }

    }

}

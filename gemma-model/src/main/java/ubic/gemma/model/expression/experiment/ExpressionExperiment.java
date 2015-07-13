/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.expression.experiment;

import gemma.gsec.model.SecuredNotChild;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;

/**
 * A gene expression study.
 */
public abstract class ExpressionExperiment extends BioAssaySet implements SecuredNotChild {

    /**
     * Constructs new instances of {@link ExpressionExperiment}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ExpressionExperiment}.
         */
        public static ExpressionExperiment newInstance() {
            return new ExpressionExperimentImpl();
        }

    }

    private DatabaseEntry accession;

    private Collection<BioAssay> bioAssays = new HashSet<BioAssay>();

    private ExperimentalDesign experimentalDesign;

    private Geeq geeq;

    private MeanVarianceRelation meanVarianceRelation;

    private Integer numberOfDataVectors;

    private Integer numberOfSamples;

    private Collection<ProcessedExpressionDataVector> processedExpressionDataVectors = new HashSet<>();

    private Collection<QuantitationType> quantitationTypes = new HashSet<>();

    private LocalFile rawDataFile;

    private Collection<RawExpressionDataVector> rawExpressionDataVectors = new HashSet<>();

    private String shortName;

    private String source;

    /**
     * 
     */
    public DatabaseEntry getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    @Override
    public Collection<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    /**
     * 
     */
    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    public Geeq getGeeq() {
        return geeq;
    }

    /**
     * 
     */
    public MeanVarianceRelation getMeanVarianceRelation() {
        return this.meanVarianceRelation;
    }

    /**
     * The number of ProcessedExpressionDataVectors associated with this.
     */
    public Integer getNumberOfDataVectors() {
        return this.numberOfDataVectors;
    }

    /**
     * The number of distinct BioMaterials associated with the experiment.
     */
    public Integer getNumberOfSamples() {
        return this.numberOfSamples;
    }

    /**
     * 
     */
    public Collection<ProcessedExpressionDataVector> getProcessedExpressionDataVectors() {
        return this.processedExpressionDataVectors;
    }

    /**
     * 
     */
    public Collection<QuantitationType> getQuantitationTypes() {
        return this.quantitationTypes;
    }

    /**
     * 
     */
    public LocalFile getRawDataFile() {
        return this.rawDataFile;
    }

    /**
     * 
     */
    public Collection<RawExpressionDataVector> getRawExpressionDataVectors() {
        return this.rawExpressionDataVectors;
    }

    /**
     * A brief unique (but optional) human-readable name for the expression experiment. For example in the past we often
     * used names like "alizadeh-lymphoma".
     */
    public String getShortName() {
        return this.shortName;
    }

    /**
     * Represents the site where the data was downloaded from.
     */
    public String getSource() {
        return this.source;
    }

    public void setAccession( DatabaseEntry accession ) {
        this.accession = accession;
    }

    public void setBioAssays( Collection<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setGeeq( Geeq geeq ) {
        this.geeq = geeq;
    }

    public void setMeanVarianceRelation( MeanVarianceRelation meanVarianceRelation ) {
        this.meanVarianceRelation = meanVarianceRelation;
    }

    public void setNumberOfDataVectors( Integer numberOfDataVectors ) {
        this.numberOfDataVectors = numberOfDataVectors;
    }

    public void setNumberOfSamples( Integer numberOfSamples ) {
        this.numberOfSamples = numberOfSamples;
    }

    public void setProcessedExpressionDataVectors(
            Collection<ProcessedExpressionDataVector> processedExpressionDataVectors ) {
        this.processedExpressionDataVectors = processedExpressionDataVectors;
    }

    public void setQuantitationTypes( Collection<QuantitationType> quantitationTypes ) {
        this.quantitationTypes = quantitationTypes;
    }

    public void setRawDataFile( LocalFile rawDataFile ) {
        this.rawDataFile = rawDataFile;
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

}
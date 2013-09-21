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

import java.util.Collection;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;

/**
 * <p>
 * A gene expression study.
 * </p>
 */
public abstract class ExpressionExperiment extends ubic.gemma.model.expression.experiment.BioAssaySet {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}.
         */
        public static ubic.gemma.model.expression.experiment.ExpressionExperiment newInstance() {
            return new ubic.gemma.model.expression.experiment.ExpressionExperimentImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4493017089352390643L;
    private String source;

    private String shortName;

    private Integer numberOfSamples;

    private Integer numberOfDataVectors;

    private Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> quantitationTypes = new java.util.HashSet<ubic.gemma.model.common.quantitationtype.QuantitationType>();

    private DatabaseEntry accession;

    private ExperimentalDesign experimentalDesign;

    private LocalFile rawDataFile;

    private Collection<ubic.gemma.model.expression.bioAssay.BioAssay> bioAssays = new java.util.HashSet<ubic.gemma.model.expression.bioAssay.BioAssay>();

    private MeanVarianceRelation meanVarianceRelation;

    private Collection<RawExpressionDataVector> rawExpressionDataVectors = new java.util.HashSet<ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector>();

    private Collection<ProcessedExpressionDataVector> processedExpressionDataVectors = new java.util.HashSet<ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ExpressionExperiment() {
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.DatabaseEntry getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    @Override
    public Collection<ubic.gemma.model.expression.bioAssay.BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation getMeanVarianceRelation() {
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
    public Collection<ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector> getProcessedExpressionDataVectors() {
        return this.processedExpressionDataVectors;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> getQuantitationTypes() {
        return this.quantitationTypes;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.LocalFile getRawDataFile() {
        return this.rawDataFile;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector> getRawExpressionDataVectors() {
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

    public void setAccession( ubic.gemma.model.common.description.DatabaseEntry accession ) {
        this.accession = accession;
    }

    public void setBioAssays( Collection<ubic.gemma.model.expression.bioAssay.BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    public void setExperimentalDesign( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setMeanVarianceRelation(
            ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation meanVarianceRelation ) {
        this.meanVarianceRelation = meanVarianceRelation;
    }

    public void setNumberOfDataVectors( Integer numberOfDataVectors ) {
        this.numberOfDataVectors = numberOfDataVectors;
    }

    public void setNumberOfSamples( Integer numberOfSamples ) {
        this.numberOfSamples = numberOfSamples;
    }

    public void setProcessedExpressionDataVectors(
            Collection<ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector> processedExpressionDataVectors ) {
        this.processedExpressionDataVectors = processedExpressionDataVectors;
    }

    public void setQuantitationTypes(
            Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> quantitationTypes ) {
        this.quantitationTypes = quantitationTypes;
    }

    public void setRawDataFile( ubic.gemma.model.common.description.LocalFile rawDataFile ) {
        this.rawDataFile = rawDataFile;
    }

    public void setRawExpressionDataVectors(
            Collection<ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector> rawExpressionDataVectors ) {
        this.rawExpressionDataVectors = rawExpressionDataVectors;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( String source ) {
        this.source = source;
    }

}
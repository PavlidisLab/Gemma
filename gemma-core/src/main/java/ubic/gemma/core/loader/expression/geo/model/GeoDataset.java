/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashSet;

/**
 * A GEO-curated dataset. In many cases this is associated with just one GeoSeries, but for studies that used more than
 * one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class GeoDataset extends GeoData {

    private static final long serialVersionUID = 2659028881509672793L;
    private static final Log log = LogFactory.getLog( GeoDataset.class.getName() );
    public ExperimentType experimentType;
    private String completeness;
    private String datasetType;
    private String description;
    private String featureCount;
    private int numChannels;
    private int numSamples;
    private int numProbes;
    private String order;
    private String organism;
    private GeoPlatform platform;
    private PlatformType platformType;
    private String pubmedId;
    private SampleType sampleType;
    private Collection<GeoSeries> series;
    private Collection<GeoSubset> subsets;
    private String updateDate;
    private ValueType valueType;

    public GeoDataset() {
        this.subsets = new HashSet<>();
        this.series = new HashSet<>();
    }

    /**
     * gene expression array-based, gene expression SAGE-based, gene expression MPSS-based, gene expression
     * RT-PCR-based, protein expression array-based, protein expression MS-based, array CGH, ChIP-chip, SNP
     *
     * Complete list of possibilities according to Nathaniel (6/2018)
     * 
     * Expression profiling by high throughput sequencing
     * Genome binding/occupancy profiling by high throughput sequencing
     * Expression profiling by array
     * Non-coding RNA profiling by array
     * Genome variation profiling by genome tiling array
     * Genome variation profiling by high throughput sequencing
     * Other
     * Non-coding RNA profiling by high throughput sequencing
     * Genome variation profiling by SNP array
     * SNP genotyping by SNP array
     * Expression profiling by genome tiling array
     * Genome variation profiling by array
     * Expression profiling by RT-PCR
     * Methylation profiling by high throughput sequencing
     * Genome binding/occupancy profiling by genome tiling array
     * Methylation profiling by genome tiling array
     * Methylation profiling by array
     * Genome binding/occupancy profiling by array
     * Expression profiling by SAGE
     * Protein profiling by protein array
     * Genome binding/occupancy profiling by SNP array
     * Non-coding RNA profiling by genome tiling array
     * Third-party reanalysis
     * Expression profiling by MPSS
     * Expression profiling by SNP array
     * Methylation profiling by SNP array
     * Protein profiling by Mass Spec
     *
     * 
     * @param string experiment type string
     * @return experiment type object
     */
    public static ExperimentType convertStringToExperimentType( String string ) {
        switch ( string ) {
            case "Expression profiling by array":
            case "gene expression array-based":
            case "Non-coding RNA profiling by array":
                return ExperimentType.geneExpressionArraybased;
            case "gene expression SAGE-based":
            case "Expression profiling by SAGE":
                return ExperimentType.geneExpressionSAGEbased;
            case "Expression profiling by high throughput sequencing":
            case "gene expression MPSS-based":
            case "Expression profiling by MPSS":
            case "Non-coding RNA profiling by high throughput sequencing":
                return ExperimentType.geneExpressionMPSSBased;
            case "gene expression RT-PCR-based":
            case "Expression profiling by RT-PCR":
                return ExperimentType.geneExpressionRTPCRbased;
            case "protein expression array-based":
                return ExperimentType.proteinExpressionArraybased;
            case "protein expression MS-based":
                return ExperimentType.proteinExpressionMSBased;
            case "array CGH":
                return ExperimentType.arrayCGH;
            case "ChIP-chip":
                return ExperimentType.ChIPChip;
            case "SNP":
            case "Genome variation profiling by genome tiling array":
            case "Genome variation profiling by SNP array":
                return ExperimentType.SNP;
            case "dual channel": // legacy term.
                GeoDataset.log
                        .warn( "Experiment Type '" + string + "' is a legacy term. Annotation may be inaccurate." );
                return ExperimentType.geneExpressionArraybased;
            case "single channel": // legacy term
                GeoDataset.log
                        .warn( "Experiment Type '" + string + "' is a legacy term. Annotation may be inaccurate." );
                return ExperimentType.geneExpressionArraybased;
            case "Other":
            case "Genome binding/occupancy profiling by high throughput sequencing":
            case "Genome variation profiling by high throughput sequencing":
            case "Expression profiling by genome tiling array":
            case "Genome variation profiling by array":
            case "Methylation profiling by high throughput sequencing":
            case "Genome binding/occupancy profiling by genome tiling array":
            case "Methylation profiling by genome tiling array":
            case "Methylation profiling by array":
            case "Genome binding/occupancy profiling by array":
            case "Protein profiling by protein array":
            case "Genome binding/occupancy profiling by SNP array":
            case "Non-coding RNA profiling by genome tiling array":
            case "Third-party reanalysis":
            case "Expression profiling by SNP array":
            case "Methylation profiling by SNP array":
            case "Protein profiling by Mass Spec":
                return ExperimentType.Other;
            default:
                throw new IllegalArgumentException( "Unknown experiment type " + string );
        }
    }

    // spotted DNA/cDNA, spotted oligonucleotide, in situ oligonucleotide, oligonucleotide beads, SAGE NlaIII, SAGE
    // Sau3A, SAGE RsaI, SARST, RT-PCR, MPSS, antibody, MS, other
    public static PlatformType convertStringToPlatformType( String string ) {
        if ( string.equals( "single channel" ) ) {
            return PlatformType.singleChannel;
        } else if ( string.equals( "dual channel" ) ) {
            return PlatformType.dualChannel;
        } else if ( string.equals( "single channel genomic" ) ) {
            return PlatformType.singleChannelGenomic;
        } else if ( string.equals( "dual channel genomic" ) ) {
            return PlatformType.dualChannelGenomic;
        } else if ( string.equals( "SAGE" ) ) {
            return PlatformType.SAGE;
        } else if ( string.equals( "MPSS" ) || string.equals( "high-throughput sequencing" ) ) {
            return PlatformType.MPSS;
        } else if ( string.equals( "spotted DNA/cDNA" ) ) {
            return PlatformType.spottedDNAOrcDNA;
        } else if ( string.equals( "spotted oligonucleotide" ) ) {
            return PlatformType.spottedOligonucleotide;
        } else if ( string.equals( "in situ oligonucleotide" ) ) {
            return PlatformType.inSituOligonucleotide;
        } else if ( string.equalsIgnoreCase( "oligonucleotide Beads" ) ) {
            return PlatformType.oligonucleotideBeads;
        } else if ( string.equals( "SAGE NlaIII" ) ) {
            return PlatformType.SAGENlaIII;
        } else if ( string.equals( "SAGE Sau3A" ) ) {
            return PlatformType.SAGESau3A;
        } else if ( string.equals( "SAGE RsaI" ) ) {
            return PlatformType.SAGERsaI;
        } else if ( string.equals( "SARST" ) ) {
            return PlatformType.SARST;
        } else if ( string.equals( "RT-PCR" ) ) {
            return PlatformType.RTPCR;
        } else if ( string.equals( "antibody" ) ) {
            return PlatformType.antibody;
        } else if ( string.equals( "MS" ) ) {
            return PlatformType.MS;
        } else if ( string.equals( "other" ) ) {
            return PlatformType.other;
        } else if ( string.equals( "nucleotide" ) ) { // legacy terminology
            GeoDataset.log.warn( "Platform Type '" + string + "' is a legacy term. Annotation may be inaccurate." );
            return PlatformType.singleChannel;
        } else {
            throw new IllegalArgumentException( "Unknown platform technology type " + string );
        }
    }

    public static SampleType convertStringToSampleType( String string ) {
        switch ( string ) {
            case "cDNA":
                return SampleType.RNA;
            case "RNA":
                return SampleType.RNA;
            case "genomic":
                return SampleType.genomic;
            case "protein":
                return SampleType.protein;
            case "mixed":
                return SampleType.mixed;
            case "SAGE":
                return SampleType.SAGE;
            case "MPSS":
                return SampleType.MPSS;
            case "SARST":
                return SampleType.SARST;
            default:
                throw new IllegalArgumentException( "Unknown sample type " + string );
        }
    }

    /**
     * count, log ratio, log2 ratio, log10 ratio, logE ratio, log e ratio, transformed count
     *
     * @param string value type string
     * @return value type object
     */
    public static ValueType convertStringToValueType( String string ) {
        switch ( string ) {
            case "count":
                return ValueType.count;
            case "log ratio":
                return ValueType.logRatio;
            case "log2 ratio":
                return ValueType.log2Ratio;
            case "log10 ratio":
                return ValueType.log10ratio;
            case "logE ratio":
                return ValueType.logERatio;
            case "log e ratio":
                return ValueType.logERatio;
            case "transformed count":
                return ValueType.transformedCount;
            case "Z-score":
                return ValueType.Zscore;
            default:
                throw new IllegalArgumentException( "Unknown value type " + string );
        }
    }

    /**
     * @param newSeries geo series
     */
    public void addSeries( GeoSeries newSeries ) {
        assert this.series != null;
        this.series.add( newSeries );

    }

    public void addSubset( GeoSubset subset ) {
        this.subsets.add( subset );
    }

    /**
     * This is used when we break a series up into two, along organism lines.
     *
     * @param s geo series
     */
    public void dissociateFromSeries( GeoSeries s ) {
        if ( !this.series.contains( s ) ) {
            throw new IllegalArgumentException( this + " does not have a reference to " + s );
        }
        this.series.remove( s );
    }

    /**
     * @return Returns the completeness.
     */
    public String getCompleteness() {
        return this.completeness;
    }

    /**
     * @param completeness The completeness to set.
     */
    public void setCompleteness( String completeness ) {
        this.completeness = completeness;
    }

    /**
     * @return Returns the datasetType.
     */
    public String getDatasetType() {
        return this.datasetType;
    }

    /**
     * @param datasetType The datasetType to set.
     */
    public void setDatasetType( String datasetType ) {
        this.datasetType = datasetType;
        this.experimentType = GeoDataset.convertStringToExperimentType( datasetType );
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return Returns the experimentType.
     */
    public GeoDataset.ExperimentType getExperimentType() {
        return this.experimentType;
    }

    /**
     * @param experimentType The experimentType to set.
     */
    public void setExperimentType( GeoDataset.ExperimentType experimentType ) {
        this.experimentType = experimentType;
    }

    /**
     * @return Returns the featureCount.
     */
    public String getFeatureCount() {
        return this.featureCount;
    }

    /**
     * @param featureCount The featureCount to set.
     */
    public void setFeatureCount( String featureCount ) {
        this.featureCount = featureCount;
    }

    /**
     * @return Returns the numChannels.
     */
    public int getNumChannels() {
        return this.numChannels;
    }

    /**
     * @param numChannels The numChannels to set.
     */
    public void setNumChannels( int numChannels ) {
        this.numChannels = numChannels;
    }

    /**
     * @return Returns the numSamples.
     */
    public int getNumSamples() {
        return this.numSamples;
    }

    /**
     * @param numSamples The numSamples to set.
     */
    public void setNumSamples( int numSamples ) {
        this.numSamples = numSamples;
    }

    public int getNumProbes() {
        return this.numProbes;
    }

    public void setNumProbes( int numProbes ) {
        this.numProbes = numProbes;
    }

    /**
     * @return Returns the order.
     */
    public String getOrder() {
        return this.order;
    }

    /**
     * @param order The order to set.
     */
    public void setOrder( String order ) {
        this.order = order;
    }

    /**
     * @return Returns the organism.
     */
    public String getOrganism() {
        return this.organism;
    }

    /**
     * @param organism The organism to set.
     */
    public void setOrganism( String organism ) {
        this.organism = organism;
    }

    /**
     * @return Returns the platform.
     */
    public GeoPlatform getPlatform() {
        return this.platform;
    }

    /**
     * @param platform The platform to set.
     */
    public void setPlatform( GeoPlatform platform ) {
        this.platform = platform;
    }

    /**
     * @return Returns the probeType.
     */
    public PlatformType getPlatformType() {
        return this.platformType;
    }

    public void setPlatformType( PlatformType platformType ) {
        this.platformType = platformType;
    }

    /**
     * @return Returns the pubmedId.
     */
    public String getPubmedId() {
        return this.pubmedId;
    }

    /**
     * @param pubmedId The pubmedId to set.
     */
    public void setPubmedId( String pubmedId ) {
        this.pubmedId = pubmedId;
    }

    /**
     * @return Returns the sampleType.
     */
    public SampleType getSampleType() {
        return this.sampleType;
    }

    /**
     * @param sampleType The sampleType to set.
     */
    public void setSampleType( SampleType sampleType ) {
        this.sampleType = sampleType;
    }

    /**
     * @return Returns the series.
     */
    public Collection<GeoSeries> getSeries() {
        return this.series;
    }

    /**
     * @param series The series to set.
     */
    public void setSeries( Collection<GeoSeries> series ) {
        this.series = series;
    }

    /**
     * @return Returns the subsets.
     */
    public Collection<GeoSubset> getSubsets() {
        return this.subsets;
    }

    /**
     * @param subsets The subsets to set.
     */
    public void setSubsets( Collection<GeoSubset> subsets ) {
        this.subsets = subsets;
    }

    /**
     * @return Returns the updateDate.
     */
    public String getUpdateDate() {
        return this.updateDate;
    }

    /**
     * @param updateDate The updateDate to set.
     */
    public void setUpdateDate( String updateDate ) {
        this.updateDate = updateDate;
    }

    /**
     * @return Returns the valueType.
     */
    public ValueType getValueType() {
        return this.valueType;
    }

    /**
     * @param valueType The valueType to set.
     */
    public void setValueType( ValueType valueType ) {
        this.valueType = valueType;
    }

    public enum ExperimentType {
        geneExpressionArraybased, geneExpressionSAGEbased, geneExpressionMPSSBased, geneExpressionRTPCRbased, proteinExpressionArraybased, proteinExpressionMSBased, arrayCGH, ChIPChip, SNP, Other
    }

    public enum PlatformType {
        dualChannel, dualChannelGenomic, SAGE, singleChannel, singleChannelGenomic, spottedDNAOrcDNA, spottedOligonucleotide, inSituOligonucleotide, oligonucleotideBeads, SAGENlaIII, SAGESau3A, SAGERsaI, SARST, RTPCR, MPSS, antibody, MS, other
    }

    public enum SampleType {
        RNA, genomic, protein, mixed, SAGE, MPSS, SARST
    }

    public enum ValueType {
        count, logRatio, log2Ratio, log10ratio, logERatio, transformedCount, Zscore
    }

}

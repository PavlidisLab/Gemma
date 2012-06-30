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
package ubic.gemma.loader.expression.geo.model;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A GEO-curated dataset. In many cases this is associated with just one GeoSeries, but for studies that used more than
 * one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDataset extends GeoData {

    public enum ExperimentType {
        geneExpressionArraybased, geneExpressionSAGEbased, geneExpressionMPSSBased, geneExpressionRTPCRbased, proteinExpressionArraybased, proteinExpressionMSBased, arrayCGH, ChIPChip, SNP
    }

    public enum PlatformType {
        dualChannel, dualChannelGenomic, SAGE, singleChannel, singleChannelGenomic, spottedDNAOrcDNA, spottedOligonucleotide, inSituOligonucleotide, oligonucleotideBeads, SAGENlaIII, SAGESau3A, SAGERsaI, SARST, RTPCR, MPSS, antibody, MS, other;
    }

    public enum SampleType {
        RNA, genomic, protein, mixed, SAGE, MPSS, SARST
    }

    public enum ValueType {
        count, logRatio, log2Ratio, log10ratio, logERatio, transformedCount
    }

    private static final long serialVersionUID = 2659028881509672793L;

    private static Log log = LogFactory.getLog( GeoDataset.class.getName() );

    /**
     * gene expression array-based, gene expression SAGE-based, gene expression MPSS-based, gene expression
     * RT-PCR-based, protein expression array-based, protein expression MS-based, array CGH, ChIP-chip, SNP
     * 
     * @param string
     * @return
     */
    public static ExperimentType convertStringToExperimentType( String string ) {
        if ( string.equals( "Expression profiling by array" ) ) {
            return ExperimentType.geneExpressionArraybased;
        } else if ( string.equals( "gene expression array-based" ) ) {
            return ExperimentType.geneExpressionArraybased;
        } else if ( string.equals( "gene expression SAGE-based" ) ) {
            return ExperimentType.geneExpressionSAGEbased;
        } else if ( string.equals( "gene expression MPSS-based" ) ) {
            return ExperimentType.geneExpressionMPSSBased;
        } else if ( string.equals( "gene expression RT-PCR-based" ) ) {
            return ExperimentType.geneExpressionRTPCRbased;
        } else if ( string.equals( "protein expression array-based" ) ) {
            return ExperimentType.proteinExpressionArraybased;
        } else if ( string.equals( "protein expression MS-based" ) ) {
            return ExperimentType.proteinExpressionMSBased;
        } else if ( string.equals( "array CGH" ) ) {
            return ExperimentType.arrayCGH;
        } else if ( string.equals( "ChIP-chip" ) ) {
            return ExperimentType.ChIPChip;
        } else if ( string.equals( "SNP" ) ) {
            return ExperimentType.SNP;
        } else if ( string.equals( "dual channel" ) ) { // legacy term.
            log.warn( "Experiment Type '" + string + "' is a legacy term. Annotation may be inaccurate." );
            return ExperimentType.geneExpressionArraybased;
        } else if ( string.equals( "single channel" ) ) { // legacy term
            log.warn( "Experiment Type '" + string + "' is a legacy term. Annotation may be inaccurate." );
            return ExperimentType.geneExpressionArraybased;
        } else {
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
            log.warn( "Platform Type '" + string + "' is a legacy term. Annotation may be inaccurate." );
            return PlatformType.singleChannel;
        } else {
            throw new IllegalArgumentException( "Unknown platform technology type " + string );
        }
    }

    /**
     * @param string
     * @return
     */
    public static SampleType convertStringToSampleType( String string ) {
        if ( string.equals( "cDNA" ) ) {
            return SampleType.RNA;
        } else if ( string.equals( "RNA" ) ) {
            return SampleType.RNA;
        } else if ( string.equals( "genomic" ) ) {
            return SampleType.genomic;
        } else if ( string.equals( "protein" ) ) {
            return SampleType.protein;
        } else if ( string.equals( "mixed" ) ) {
            return SampleType.mixed;
        } else if ( string.equals( "SAGE" ) ) {
            return SampleType.SAGE;
        } else if ( string.equals( "MPSS" ) ) {
            return SampleType.MPSS;
        } else if ( string.equals( "SARST" ) ) {
            return SampleType.SARST;
        } else {
            throw new IllegalArgumentException( "Unknown sample type " + string );
        }
    }

    /**
     * count, log ratio, log2 ratio, log10 ratio, logE ratio, log e ratio, transformed count
     * 
     * @param string
     * @return
     */
    public static ValueType convertStringToValueType( String string ) {
        if ( string.equals( "count" ) ) {
            return ValueType.count;
        } else if ( string.equals( "log ratio" ) ) {
            return ValueType.logRatio;
        } else if ( string.equals( "log2 ratio" ) ) {
            return ValueType.log2Ratio;
        } else if ( string.equals( "log10 ratio" ) ) {
            return ValueType.log10ratio;
        } else if ( string.equals( "logE ratio" ) ) {
            return ValueType.logERatio;
        } else if ( string.equals( "log e ratio" ) ) {
            return ValueType.logERatio;
        } else if ( string.equals( "transformed count" ) ) {
            return ValueType.transformedCount;
        } else {
            throw new IllegalArgumentException( "Unknown value type " + string );
        }
    }

    public ExperimentType experimentType;

    private String completeness;
    private String datasetType;
    private String description;
    private String featureCount;
    private int numChannels;
    private int numSamples;
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
        this.subsets = new HashSet<GeoSubset>();
        this.series = new HashSet<GeoSeries>();
    }

    /**
     * @param newSeries
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
     * @param s
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
     * @return Returns the datasetType.
     */
    public String getDatasetType() {
        return this.datasetType;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return Returns the experimentType.
     */
    public GeoDataset.ExperimentType getExperimentType() {
        return this.experimentType;
    }

    /**
     * @return Returns the featureCount.
     */
    public String getFeatureCount() {
        return this.featureCount;
    }

    /**
     * @return Returns the numChannels.
     */
    public int getNumChannels() {
        return this.numChannels;
    }

    /**
     * @return Returns the numSamples.
     */
    public int getNumSamples() {
        return this.numSamples;
    }

    /**
     * @return Returns the order.
     */
    public String getOrder() {
        return this.order;
    }

    /**
     * @return Returns the organism.
     */
    public String getOrganism() {
        return this.organism;
    }

    /**
     * @return Returns the platform.
     */
    public GeoPlatform getPlatform() {
        return this.platform;
    }

    /**
     * @return Returns the probeType.
     */
    public PlatformType getPlatformType() {
        return this.platformType;
    }

    /**
     * @return Returns the pubmedId.
     */
    public String getPubmedId() {
        return this.pubmedId;
    }

    /**
     * @return Returns the sampleType.
     */
    public SampleType getSampleType() {
        return this.sampleType;
    }

    /**
     * @return Returns the series.
     */
    public Collection<GeoSeries> getSeries() {
        return this.series;
    }

    /**
     * @return Returns the subsets.
     */
    public Collection<GeoSubset> getSubsets() {
        return this.subsets;
    }

    /**
     * @return Returns the updateDate.
     */
    public String getUpdateDate() {
        return this.updateDate;
    }

    /**
     * @return Returns the valueType.
     */
    public ValueType getValueType() {
        return this.valueType;
    }

    /**
     * @param completeness The completeness to set.
     */
    public void setCompleteness( String completeness ) {
        this.completeness = completeness;
    }

    /**
     * @param datasetType The datasetType to set.
     */
    public void setDatasetType( String datasetType ) {
        this.datasetType = datasetType;
        // FIXME - this is underhanded, there is some confusion about how to treat datasetType vs experimentType
        this.experimentType = convertStringToExperimentType( datasetType );
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param experimentType The experimentType to set.
     */
    public void setExperimentType( GeoDataset.ExperimentType experimentType ) {
        this.experimentType = experimentType;
    }

    /**
     * @param featureCount The featureCount to set.
     */
    public void setFeatureCount( String featureCount ) {
        this.featureCount = featureCount;
    }

    /**
     * @param numChannels The numChannels to set.
     */
    public void setNumChannels( int numChannels ) {
        this.numChannels = numChannels;
    }

    /**
     * @param numSamples The numSamples to set.
     */
    public void setNumSamples( int numSamples ) {
        this.numSamples = numSamples;
    }

    /**
     * @param order The order to set.
     */
    public void setOrder( String order ) {
        this.order = order;
    }

    /**
     * @param organism The organism to set.
     */
    public void setOrganism( String organism ) {
        this.organism = organism;
    }

    /**
     * @param platform The platform to set.
     */
    public void setPlatform( GeoPlatform platform ) {
        this.platform = platform;
    }

    /**
     * @param probeType The probeType to set.
     */
    public void setPlatformType( PlatformType platformType ) {
        this.platformType = platformType;
    }

    /**
     * @param pubmedId The pubmedId to set.
     */
    public void setPubmedId( String pubmedId ) {
        this.pubmedId = pubmedId;
    }

    /**
     * @param sampleType The sampleType to set.
     */
    public void setSampleType( SampleType sampleType ) {
        this.sampleType = sampleType;
    }

    /**
     * @param series The series to set.
     */
    public void setSeries( Collection<GeoSeries> series ) {
        this.series = series;
    }

    /**
     * @param subsets The subsets to set.
     */
    public void setSubsets( Collection<GeoSubset> subsets ) {
        this.subsets = subsets;
    }

    /**
     * @param updateDate The updateDate to set.
     */
    public void setUpdateDate( String updateDate ) {
        this.updateDate = updateDate;
    }

    /**
     * @param valueType The valueType to set.
     */
    public void setValueType( ValueType valueType ) {
        this.valueType = valueType;
    }

}

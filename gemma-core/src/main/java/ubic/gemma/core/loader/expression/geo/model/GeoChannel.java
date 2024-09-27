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

import ubic.gemma.model.common.description.Characteristic;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents data for one channel on a microarray in GEO. Corresponds (roughly) to a BioMaterial in Gemma.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class GeoChannel {

    private String sourceName = "";
    private final Collection<String> characteristics = new HashSet<>();
    private String bioMaterialProvider = "";
    private String growthProtocol = "";
    private String treatmentProtocol = "";
    private String extractProtocol = "";
    private String label = "";
    private String labelProtocol = "";
    private int channelNumber = -1;
    private String organism = null;
    private ChannelMolecule molecule;

    public static ChannelMolecule convertStringToMolecule( String string ) {
        switch ( string ) {
            case "total RNA":
                return ChannelMolecule.totalRNA;
            case "polyA RNA":
                return ChannelMolecule.polyARNA;
            case "cytoplasmic RNA":
                return ChannelMolecule.cytoplasmicRNA;
            case "nuclear RNA":
                return ChannelMolecule.nuclearRNA;
            case "genomic DNA":
                return ChannelMolecule.genomicDNA;
            case "protein":
                return ChannelMolecule.protein;
            case "other":
                return ChannelMolecule.other;
            default:
                throw new IllegalArgumentException( "Unknown channel molecule " + string );
        }
    }

    public void addToExtractProtocol( String s ) {
        this.extractProtocol = this.extractProtocol + " " + s;
    }

    public void addToGrowthProtocol( String s ) {
        this.growthProtocol = this.growthProtocol + " " + s;
    }

    public void addCharacteristic( String characteristic ) {
        characteristics.add( characteristic );
    }

    public void addToTreatmentProtocol( String s ) {
        this.treatmentProtocol = this.treatmentProtocol + " " + s;
    }

    /**
     * @return Returns the bioMaterialProvider.
     */
    public String getBioMaterialProvider() {
        return this.bioMaterialProvider;
    }

    /**
     * @param bioMaterialProvider The bioMaterialProvider to set.
     */
    public void setBioMaterialProvider( String bioMaterialProvider ) {
        this.bioMaterialProvider = bioMaterialProvider;
    }

    /**
     * @return Returns the characteristic.
     */
    public Collection<String> getCharacteristic() {
        return this.characteristics;
    }

    /**
     * @return Returns the extractProtocol.
     */
    public String getExtractProtocol() {
        return this.extractProtocol;
    }

    /**
     * @param extractProtocol The extractProtocol to set.
     */
    public void setExtractProtocol( String extractProtocol ) {
        this.extractProtocol = extractProtocol;
    }

    /**
     * @return Returns the label.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * @param label The label to set.
     */
    public void setLabel( String label ) {
        this.label = label;
    }

    /**
     * @return Returns the labelProtocol.
     */
    public String getLabelProtocol() {
        return this.labelProtocol;
    }

    /**
     * @param labelProtocol The labelProtocol to set.
     */
    public void setLabelProtocol( String labelProtocol ) {
        this.labelProtocol = labelProtocol;
    }

    /**
     * @return Returns the molecule.
     */
    public ChannelMolecule getMolecule() {
        return this.molecule;
    }

    /**
     * @param molecule The molecule to set.
     */
    public void setMolecule( ChannelMolecule molecule ) {
        this.molecule = molecule;
    }

    /**
     * Convert the molecule into a MGED Ontology-based MaterialType Characteristic. If "other" we just return a
     * plain text value. URIs checked 8/2024
     *
     * @return characteristic
     */
    public Characteristic getMoleculeAsCharacteristic() {

        Characteristic result = Characteristic.Factory.newInstance();
        result.setDescription( "Material Type" );
        result.setCategory( "molecular entity" );
        result.setCategoryUri( "http://purl.obolibrary.org/obo/CHEBI_23367" );

        switch ( this.molecule ) {
            case cytoplasmicRNA:
                result.setValue( "cytoplasmic RNA extract" );
                result.setValueUri( "http://purl.obolibrary.org/obo/OBI_0000876" );
                break;
            case polyARNA:
                result.setValue( "polyA RNA extract" );
                result.setValueUri( "http://purl.obolibrary.org/obo/OBI_0000869" );
                break;
            case genomicDNA: // as per https://github.com/The-Sequence-Ontology/SO-Ontologies/blob/master/Ontology_Files/so-simple.owl
                result.setValue( "genomic_DNA" );
                result.setValueUri( "http://purl.obolibrary.org/obo/SO_0000991" ); // corrected 8/2024.
                break;
            case totalRNA:
                result.setValue( "total RNA" );
                result.setValueUri( "http://www.ebi.ac.uk/efo/EFO_0004964" );
                break;
            case nuclearRNA:
                result.setValue( "nuclear RNA extract" );
                result.setValueUri( "http://purl.obolibrary.org/obo/OBI_0000862" );
                break;
            case protein:
                result.setValue( "protein" );
                result.setValueUri( "http://purl.obolibrary.org/obo/CHEBI_36080" );
                break;
            case other:
                result.setValue( "Other material type" );
                break;
            default:
                break;
        }

        return result;
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
     * @return Returns the sourceName.
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * @param sourceName The sourceName to set.
     */
    public void setSourceName( String sourceName ) {
        this.sourceName = sourceName;
    }

    /**
     * @return Returns the treatmentProtocol.
     */
    public String getTreatmentProtocol() {
        return this.treatmentProtocol;
    }

    /**
     * @param treatmentProtocol The treatmentProtocol to set.
     */
    public void setTreatmentProtocol( String treatmentProtocol ) {
        this.treatmentProtocol = treatmentProtocol;
    }

    /**
     * @return Returns the channelNumber.
     */
    public int getChannelNumber() {
        return this.channelNumber;
    }

    /**
     * @param channelNumber The channelNumber to set.
     */
    public void setChannelNumber( int channelNumber ) {
        this.channelNumber = channelNumber;
    }

    /**
     * @return Returns the characteristics.
     */
    public Collection<String> getCharacteristics() {
        return this.characteristics;
    }

    public String getGrowthProtocol() {
        return growthProtocol;
    }

    public void setGrowthProtocol( String growthProtocol ) {
        this.growthProtocol = growthProtocol;
    }

    public enum ChannelMolecule {
        totalRNA, polyARNA, cytoplasmicRNA, nuclearRNA, genomicDNA, protein, other
    }

}

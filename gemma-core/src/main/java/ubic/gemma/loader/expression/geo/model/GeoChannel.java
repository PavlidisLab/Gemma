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

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.ontology.providers.MgedOntologyService;

/**
 * Represents data for one channel on a microarray in GEO. Corresponds (roughly) to a BioMaterial in Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoChannel {

    private int channelNumber = -1;

    private String organism = null;

    private ChannelMolecule molecule;

    public enum ChannelMolecule {
        totalRNA, polyARNA, cytoplasmicRNA, nuclearRNA, genomicDNA, protein, other
    }

    public static ChannelMolecule convertStringToMolecule( String string ) {
        if ( string.equals( "total RNA" ) ) {
            return ChannelMolecule.totalRNA;
        } else if ( string.equals( "polyA RNA" ) ) {
            return ChannelMolecule.polyARNA;
        } else if ( string.equals( "cytoplasmic RNA" ) ) {
            return ChannelMolecule.cytoplasmicRNA;
        } else if ( string.equals( "nuclear RNA" ) ) {
            return ChannelMolecule.nuclearRNA;
        } else if ( string.equals( "genomic DNA" ) ) {
            return ChannelMolecule.genomicDNA;
        } else if ( string.equals( "protein" ) ) {
            return ChannelMolecule.protein;
        } else if ( string.equals( "other" ) ) {
            return ChannelMolecule.other;
        } else {
            throw new IllegalArgumentException( "Unknown channel molecule " + string );
        }
    }

    String sourceName = "";
    Collection<String> characteristics = new HashSet<String>();
    String bioMaterialProvider = "";
    String growthProtocol = "";
    String treatmentProtocol = "";
    String extractProtocol = "";
    String label = "";
    String labelProtocol = "";

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
     * @param characteristic The characteristic to set.
     */
    public void setCharacteristic( Collection<String> characteristics ) {
        this.characteristics = characteristics;
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
     * Convert the molecule into a MGED Ontology-based MaterialType VocabCharacteristic. If "other" we just return a
     * plain text value.
     * 
     * @return
     */
    public Characteristic getMoleculeAsCharacteristic() {

        VocabCharacteristic result = VocabCharacteristic.Factory.newInstance();
        result.setDescription( "MaterialType" );
        result.setCategory( "MaterialType" );
        result.setCategoryUri( MgedOntologyService.MGED_ONTO_BASE_URL + "MaterialType" );

        switch ( this.molecule ) {
            case cytoplasmicRNA:
                result.setValue( "cytoplasmic_RNA" );
                result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "cytoplasmic_RNA" );
                break;
            case polyARNA:
                result.setValue( "polyA_RNA" );
                result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "polyA_RNA" );
                break;
            case genomicDNA:
                result.setValue( "genomic_DNA" );
                result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "genomic_DNA" );
                break;
            case totalRNA:
                result.setValue( "total_RNA" );
                result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "total_RNA" );
                break;
            case nuclearRNA:
                result.setValue( "nuclear_RNA" );
                result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "nuclear_RNA" );
                break;
            case protein:
                result.setValue( "protein" );
                result.setValueUri( MgedOntologyService.MGED_ONTO_BASE_URL + "protein" );
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
     * @param molecule The molecule to set.
     */
    public void setMolecule( ChannelMolecule molecule ) {
        this.molecule = molecule;
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

    /**
     * @param characteristics The characteristics to set.
     */
    public void setCharacteristics( Collection<String> characteristics ) {
        this.characteristics = characteristics;
    }

    public String getGrowthProtocol() {
        return growthProtocol;
    }

    public void setGrowthProtocol( String growthProtocol ) {
        this.growthProtocol = growthProtocol;
    }

}

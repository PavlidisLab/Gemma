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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents data for one channel on a microarray in GEO. Corresponds (roughly) to a BioMaterial in Gemma.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoChannel {

    private int channelNumber = -1;

    private String organism = null;

    private channelMolecule molecule;

    public enum channelMolecule {
        totalRNA, polyARNA, cytoplasmicRNA, nuclearRNA, genomicDNA, protein, other
    };

    public static channelMolecule convertStringToMolecule( String string ) {
        if ( string.equals( "total RNA" ) ) {
            return channelMolecule.totalRNA;
        } else if ( string.equals( "polyA RNA" ) ) {
            return channelMolecule.polyARNA;
        } else if ( string.equals( "cytoplasmic RNA" ) ) {
            return channelMolecule.cytoplasmicRNA;
        } else if ( string.equals( "nuclear RNA" ) ) {
            return channelMolecule.nuclearRNA;
        } else if ( string.equals( "genomic DNA" ) ) {
            return channelMolecule.genomicDNA;
        } else if ( string.equals( "protein" ) ) {
            return channelMolecule.protein;
        } else if ( string.equals( "other" ) ) {
            return channelMolecule.other;
        } else {
            throw new IllegalArgumentException( "Unknown channel molecule " + string );
        }
    }

    String sourceName = null;
    Collection<String> characteristics = new HashSet<String>();;
    String bioMaterialProvider = null;
    String treatmentProtocol = null;
    String extractProtocol = null;
    String label = null;
    String labelProtocol = null;

    public void addToExtractProtocol( String s ) {
        this.extractProtocol = this.extractProtocol + " " + s;
    }

    public void addCharacteristic( String characteristic ) {
        characteristics.add( characteristic );
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
    public channelMolecule getMolecule() {
        return this.molecule;
    }

    /**
     * @param molecule The molecule to set.
     */
    public void setMolecule( channelMolecule molecule ) {
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

}

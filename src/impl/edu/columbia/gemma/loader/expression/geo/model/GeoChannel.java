package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;
import java.util.HashSet;

public class GeoChannel {

    int channelNumber;

    String organism;
    String molecule;
    String sourceName;
    Collection<String> characteristics;
    String bioMaterialProvider;
    String treatmentProtocol;
    String extractProtocol;
    String label;
    String labelProtocol;

    public void addToExtractProtocol( String s ) {
        this.extractProtocol = this.extractProtocol + " " + s;
    }

    public GeoChannel() {
        characteristics = new HashSet<String>();
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
    public String getMolecule() {
        return this.molecule;
    }

    /**
     * @param molecule The molecule to set.
     */
    public void setMolecule( String molecule ) {
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

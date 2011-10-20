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
package ubic.gemma.association.phenotype.fileUpload.experimentalEvidence;

import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

/**
 * This represents one line from the tsv file in a simple object
 * 
 * @version $Id$
 * @author nicolas
 */
public class ExpEvidenceLineInfo {

    // used the mgedOntologyService to get those values (defined structure)
    public final static String DEVELOPMENTAL_STAGE = "DevelopmentalStage";
    public final static String BIOSOURCE = "BioSource";
    public final static String ORGANISM_PART = "OrganismPart";
    public final static String EXPERIMENT_DESIGN = "ExperimentDesign";
    public final static String TREATMENT = "Treatment";
    public final static String EXPERIMENT = "Experiment";
    public final static String PHENOTYPE = "Phenotype";

    public final static String DEVELOPMENTAL_STAGE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#DevelopmentalStage";
    public final static String BIOSOURCE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioSource";
    public final static String ORGANISM_PART_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#OrganismPart";
    public final static String EXPERIMENT_DESIGN_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ExperimentDesign";
    public final static String TREATMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Treatment";
    public final static String EXPERIMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Experiment";
    public final static String PHENOTYPE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Phenotype";

    private String geneName = "";
    private String geneID = "";
    private String primaryReferencePubmed = null;
    private String reviewReferencePubmed = null;
    private String evidenceCode = "";
    private String comment = "";
    private String associationType = null;
    private Boolean isEdivenceNegative = false;

    // All characteristics taken from file
    private String[] developmentStage = null;
    private String[] bioSource = null;
    private String[] organismPart = null;
    private String[] experimentDesign = null;
    private String[] treatment = null;
    private String[] experimentOBI = null;
    private String[] phenotype = null;

    // What will populate the Evidences
    private Set<CharacteristicValueObject> experimentCharacteristics = new HashSet<CharacteristicValueObject>();
    private Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

    public ExpEvidenceLineInfo( String line ) {

        String[] tokens = line.split( "\t" );
        this.geneName = tokens[0].trim();
        this.geneID = tokens[1].trim();
        this.primaryReferencePubmed = tokens[2].trim();
        this.reviewReferencePubmed = tokens[3].trim();
        this.evidenceCode = tokens[4].trim();
        this.comment = tokens[5].trim();
        this.associationType = tokens[6].trim();

        if ( tokens[7].trim().equals( "1" ) ) {
            this.isEdivenceNegative = true;
        }

        this.developmentStage = trimArray( tokens[8].split( ";" ) );
        this.bioSource = trimArray( tokens[9].split( ";" ) );
        this.organismPart = trimArray( tokens[10].split( ";" ) );
        this.experimentDesign = trimArray( tokens[11].split( ";" ) );
        this.treatment = trimArray( tokens[12].split( ";" ) );
        this.experimentOBI = trimArray( tokens[13].split( ";" ) );
        this.phenotype = trimArray( tokens[14].split( ";" ) );
    }

    private String[] trimArray( String[] array ) {

        String[] trimmedArray = new String[array.length];

        for ( int i = 0; i < trimmedArray.length; i++ ) {
            trimmedArray[i] = array[i].trim();
        }

        return trimmedArray;
    }

    public String getGeneID() {
        return geneID;
    }

    public void setGeneID( String geneID ) {
        this.geneID = geneID;
    }

    public String getPrimaryReferencePubmed() {
        return primaryReferencePubmed;
    }

    public void setPrimaryReferencePubmed( String primaryReferencePubmed ) {
        this.primaryReferencePubmed = primaryReferencePubmed;
    }

    public String getReviewReferencePubmed() {
        return reviewReferencePubmed;
    }

    public void setReviewReferencePubmed( String reviewReferencePubmed ) {
        this.reviewReferencePubmed = reviewReferencePubmed;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment( String comment ) {
        this.comment = comment;
    }

    public String[] getPhenotype() {
        return phenotype;
    }

    public void setPhenotype( String[] phenotype ) {
        this.phenotype = phenotype;
    }

    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType( String associationType ) {
        this.associationType = associationType;
    }

    public String[] getDevelopmentStage() {
        return developmentStage;
    }

    public void setDevelopmentStage( String[] developmentStage ) {
        this.developmentStage = developmentStage;
    }

    public String[] getBioSource() {
        return bioSource;
    }

    public void setBioSource( String[] bioSource ) {
        this.bioSource = bioSource;
    }

    public String[] getOrganismPart() {
        return organismPart;
    }

    public void setOrganismPart( String[] organismPart ) {
        this.organismPart = organismPart;
    }

    public String[] getExperimentDesign() {
        return experimentDesign;
    }

    public void setExperimentDesign( String[] experimentDesign ) {
        this.experimentDesign = experimentDesign;
    }

    public String[] getTreatment() {
        return treatment;
    }

    public void setTreatment( String[] treatment ) {
        this.treatment = treatment;
    }

    public String[] getExperimentOBI() {
        return experimentOBI;
    }

    public void setExperimentOBI( String[] experimentOBI ) {
        this.experimentOBI = experimentOBI;
    }

    public Set<CharacteristicValueObject> getExperimentCharacteristics() {
        return experimentCharacteristics;
    }

    public void setExperimentCharacteristics( Set<CharacteristicValueObject> experimentCharacteristics ) {
        this.experimentCharacteristics = experimentCharacteristics;
    }

    public void addExperimentCharacteristic( CharacteristicValueObject experimentCharacteristic ) {
        experimentCharacteristics.add( experimentCharacteristic );
    }

    public Set<CharacteristicValueObject> getPhenotypes() {
        return phenotypes;
    }

    public void setPhenotypes( Set<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void addPhenotype( CharacteristicValueObject phenotype ) {
        phenotypes.add( phenotype );
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public Boolean getIsEdivenceNegative() {
        return isEdivenceNegative;
    }

    public void setIsEdivenceNegative( Boolean isEdivenceNegative ) {
        this.isEdivenceNegative = isEdivenceNegative;
    }

}

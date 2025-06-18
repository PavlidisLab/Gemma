/*
 * The gemma-web project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.web.controller.expression.arrayDesign;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Extended value object to carry more data to client.
 *
 * @author paul
 */
public class ArrayDesignValueObjectExt extends ArrayDesignValueObject {

    private static final long serialVersionUID = 218696698777199533L;
    private Set<String> additionalTaxa;
    private String allParentsAnnotationLink;
    private Collection<String> alternateNames;
    private ArrayDesignValueObject alternative; // e.g. for Affymetrix CDF versions
    private String bioProcessAnnotationLink;
    private String colorString;
    private Collection<ArrayDesignValueObject> mergees;
    private ArrayDesignValueObject merger;
    private String noParentsAnnotationLink;
    private Collection<ArrayDesignValueObject> subsumees;
    private ArrayDesignValueObject subsumer;

    public ArrayDesignValueObjectExt( ArrayDesignValueObject vo ) {
        super( vo );
        addAnnotationFileLinks();
        formatTechnologyType();

        addAnnotationFileLinks();
    }

    public Set<String> getAdditionalTaxa() {
        return additionalTaxa;
    }

    public void setAdditionalTaxa( Set<String> additionalTaxa ) {
        this.additionalTaxa = additionalTaxa;
    }

    public String getAllParentsAnnotationLink() {
        return allParentsAnnotationLink;
    }

    public Collection<String> getAlternateNames() {
        return alternateNames;
    }

    public ArrayDesignValueObject getAlternative() {
        return alternative;
    }

    public String getBioProcessAnnotationLink() {
        return bioProcessAnnotationLink;
    }

    public String getColorString() {
        return colorString;
    }

    public Collection<ArrayDesignValueObject> getMergees() {
        return mergees;
    }

    public ArrayDesignValueObject getMerger() {
        return merger;
    }

    public String getNoParentsAnnotationLink() {
        return noParentsAnnotationLink;
    }

    public Collection<ArrayDesignValueObject> getSubsumees() {
        return subsumees;
    }

    public ArrayDesignValueObject getSubsumer() {
        return subsumer;
    }

    /**
     * Method to format taxon list for display.
     *
     * @param t Collection of taxon used to create array/platform
     */
    public void setAdditionalTaxa( Collection<Taxon> t ) {

        this.additionalTaxa = new TreeSet<>();

        for ( Taxon taxon : t ) {
            this.additionalTaxa.add( taxon.getScientificName() );
        }

    }

    public void setAllParentsAnnotationLink( String allParentsAnnotationLink ) {
        this.allParentsAnnotationLink = allParentsAnnotationLink;
    }

    public void setAlternateNames( Collection<String> alternateNames ) {
        this.alternateNames = alternateNames;
    }

    public void setAlternative( ArrayDesignValueObject alternative ) {
        this.alternative = alternative;
    }

    public void setBioProcessAnnotationLink( String bioProcessAnnotationLink ) {
        this.bioProcessAnnotationLink = bioProcessAnnotationLink;
    }

    public void setColorString( String colorString ) {
        this.colorString = colorString;
    }

    public void setMergees( Collection<ArrayDesignValueObject> mergees ) {
        this.mergees = mergees;
        this.setIsMerged( !mergees.isEmpty() );
    }

    public void setMerger( ArrayDesignValueObject arrayDesignValueObject ) {
        this.merger = arrayDesignValueObject;
        this.setIsMergee( arrayDesignValueObject != null );
    }

    public void setNoParentsAnnotationLink( String noParentsAnnotationLink ) {
        this.noParentsAnnotationLink = noParentsAnnotationLink;
    }

    public void setSubsumees( Collection<ArrayDesignValueObject> subsumees ) {
        this.subsumees = subsumees;
        this.setIsSubsumer( !subsumees.isEmpty() );
    }

    public void setSubsumer( ArrayDesignValueObject arrayDesignValueObject ) {
        this.subsumer = arrayDesignValueObject;
        this.setIsSubsumed( arrayDesignValueObject != null );
    }

    private void addAnnotationFileLinks() {

        this.noParentsAnnotationLink = "downloadAnnotationFile.html?id=" + getId() + "&fileType=noParents";

        this.allParentsAnnotationLink = "downloadAnnotationFile.html?id=" + getId() + "&fileType=allParents";

        this.bioProcessAnnotationLink = "downloadAnnotationFile.html?id=" + getId() + "&fileType=bioProcess";

    }

    private void formatTechnologyType() {

        this.colorString = "Not specified";
        if ( this.getTechnologyType() == null ) {
            return;
        }

        if ( this.getTechnologyType().equals( TechnologyType.ONECOLOR.name() ) ) {
            colorString = "one-color";
        } else if ( this.getTechnologyType().equals( TechnologyType.TWOCOLOR.name() ) ) {
            colorString = "two-color";
        } else if ( this.getTechnologyType().equals( TechnologyType.DUALMODE.name() ) ) {
            colorString = "dual mode";
        } else if ( this.getTechnologyType().equals( TechnologyType.SEQUENCING.name() ) ) {
            colorString = "sequencing-based";
        } else if ( this.getTechnologyType().equals( TechnologyType.GENELIST.name() ) ) {
            colorString = "gene list";
        } else if ( this.getTechnologyType().equals( TechnologyType.OTHER.name() ) ) {
            colorString = "other";
        }
    }

}

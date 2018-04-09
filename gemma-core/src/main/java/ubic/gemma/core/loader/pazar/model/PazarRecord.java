/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.loader.pazar.model;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public class PazarRecord {

    private PazarMethod method;
    private String pazarTargetGeneId;
    private String pazarTfId;
    private String project;
    private String pubMedId;
    private String species;
    private String targetGeneAcc;
    private String tfAcc;

    public PazarMethod getMethod() {
        return method;
    }

    public void setMethod( PazarMethod method ) {
        this.method = method;
    }

    public String getPazarTargetGeneId() {
        return pazarTargetGeneId;
    }

    public void setPazarTargetGeneId( String pazarTargetGeneId ) {
        this.pazarTargetGeneId = pazarTargetGeneId;
    }

    public String getPazarTfId() {
        return pazarTfId;
    }

    public void setPazarTfId( String pazarTfId ) {
        this.pazarTfId = pazarTfId;
    }

    public String getProject() {
        return project;
    }

    public void setProject( String project ) {
        this.project = project;
    }

    public String getPubMedId() {
        return pubMedId;
    }

    public void setPubMedId( String pubMedId ) {
        this.pubMedId = pubMedId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies( String species ) {
        this.species = species;
    }

    public String getTargetGeneAcc() {
        return targetGeneAcc;
    }

    public void setTargetGeneAcc( String targetGeneAcc ) {
        this.targetGeneAcc = targetGeneAcc;
    }

    public String getTfAcc() {
        return tfAcc;
    }

    public void setTfAcc( String tfAcc ) {
        this.tfAcc = tfAcc;
    }

    enum PazarMethod {
        CHIP, CONSERVATION_FOUND_BY_ALIGNMENT, DIRECT_GEL_SHIFT, DNA_PROTEIN_PRECIPITATION_ASSAY, DNASE_FOOTPRINTING, METHYLATION_INTERFERENCE, SUPERSHFT
    }
}

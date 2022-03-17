/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.loader.util.biomart;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Value object that represents a file record line from BioMart as configured with query parameters. Which follows the
 * structure: ensembleGeneId enemblTranscriptId entrez gene ensembl_peptide_id and optionally for humans hgnc_id. There
 * is a many to many relationship between Ensembl genes ids and entrez gene. As such this value object holds a
 * collection of entrez gene strings.
 *
 * @author ldonnison
 */
public class Ensembl2NcbiValueObject implements Serializable {

    private static final long serialVersionUID = -859220901359582113L;
    private final Set<String> ncbiGenes = new HashSet<>();
    private String enemblTranscriptId = "";
    private String ensembleGeneId = "";
    private String ensemblPeptideId = "";
    private String hgcnId = "";
    private Integer ncbiTaxonId = 0;

    public String getEnsemblGeneId() {
        return ensembleGeneId;
    }

    public void setEnsemblGeneId( String ensembleGeneId ) {
        this.ensembleGeneId = ensembleGeneId;
    }

    public String getEnsemblPeptideId() {
        return ensemblPeptideId;
    }

    public void setEnsemblPeptideId( String ensemblPeptideId ) {
        this.ensemblPeptideId = ensemblPeptideId;
    }

    public String getEnsemblTranscriptId() {
        return enemblTranscriptId;
    }

    public void setEnsemblTranscriptId( String enemblTranscriptId ) {
        this.enemblTranscriptId = enemblTranscriptId;
    }

    /**
     * @return Collection of strings representing entrez genes mapping to the ensemble id.
     */
    public Collection<String> getEntrezgenes() {

        return ncbiGenes;
    }

    public String getHgnc_id() {
        return hgcnId;
    }

    public void setHgnc_id( String hgcnId ) {
        this.hgcnId = hgcnId;
    }

    public Integer getNcbiTaxonId() {
        return ncbiTaxonId;
    }

    public void setNcbiTaxonId( Integer ncbiTaxonId ) {
        this.ncbiTaxonId = ncbiTaxonId;
    }

    @Override
    public String toString() {
        return "Ensembl2NcbiValueObject [ncbiTaxonId=" + ncbiTaxonId + ", ensembleGeneId=" + ensembleGeneId + "]";
    }

}

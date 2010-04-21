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
package ubic.gemma.loader.protein.biomart.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Value object that represents a file record line from BioMart as configured with query parameters.
 * Which follows the structure:
 * ensembl_gene_id  ensembl_transcript_id entrezgene  ensembl_peptide_id
 * and optionally for humans hgnc_id.
 * 
 * There is a many to many relationship between Ensembl genes ids and entrezgene.
 * As such this value object holds a collection of entrezgene strings.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class BioMartEnsembleNcbi implements Serializable{
   
    
 private static final long serialVersionUID = -859220901359582113L;
    
    
    private Integer species_ncbi_taxon_id =0;   
    private String ensembl_gene_id = "";       
    private String ensembl_transcript_id ="";       
    private Collection<String> entrezgenes;
    
    /**
     * 
     * @return Collection of strings representing entrez genes mapping to the ensemble id.
     */
    public Collection<String> getEntrezgenes() {
        if(this.entrezgenes ==null){
            entrezgenes = new ArrayList<String>();
        }
        return entrezgenes;
    }
    
   
    String ensembl_peptide_id ="";    
    String hgnc_id="";
    
    public String getHgnc_id() {
        return hgnc_id;
    }
    public void setHgnc_id( String hgnc_id ) {
        this.hgnc_id = hgnc_id;
    }
    public Integer getSpecies_ncbi_taxon_id() {
        return species_ncbi_taxon_id;
    }
    public void setSpecies_ncbi_taxon_id( Integer species_ncbi_taxon_id ) {
        this.species_ncbi_taxon_id = species_ncbi_taxon_id;
    }
    public String getEnsembl_gene_id() {
        return ensembl_gene_id;
    }
    public void setEnsembl_gene_id( String ensembl_gene_id ) {
        this.ensembl_gene_id = ensembl_gene_id;
    }
    public String getEnsembl_transcript_id() {
        return ensembl_transcript_id;
    }
    public void setEnsembl_transcript_id( String ensembl_transcript_id ) {
        this.ensembl_transcript_id = ensembl_transcript_id;
    }   
    public String getEnsembl_peptide_id() {
        return ensembl_peptide_id;
    }
    public void setEnsembl_peptide_id( String ensembl_peptide_id ) {
        this.ensembl_peptide_id = ensembl_peptide_id;
    }
   

}

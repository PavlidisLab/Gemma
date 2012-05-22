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
package ubic.gemma.model.genome.gene.phenotype;

/** Used to filter values received depending on taxon and privacy chosen */
public class EvidenceFilter {

    private String taxonCommonName = "";
    private boolean showOnlyEditable = false;

    public EvidenceFilter() {
        super();
    }

    public EvidenceFilter( String taxonCommonName, boolean showOnlyEditable ) {
        super();
        this.taxonCommonName = taxonCommonName;
        this.showOnlyEditable = showOnlyEditable;
    }

    public String getTaxonCommonName() {
        return this.taxonCommonName;
    }

    public void setTaxonCommonName( String taxonCommonName ) {
        this.taxonCommonName = taxonCommonName;
    }

    public boolean isShowOnlyEditable() {
        return this.showOnlyEditable;
    }

    public void setShowOnlyEditable( boolean showOnlyEditable ) {
        this.showOnlyEditable = showOnlyEditable;
    }
}

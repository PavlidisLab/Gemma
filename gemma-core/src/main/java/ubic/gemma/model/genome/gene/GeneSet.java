/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.genome.gene;

import gemma.gsec.model.SecuredNotChild;
import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.HashSet;

/**
 * A grouping of genes that share a common relationship
 */
public class GeneSet extends AbstractAuditable implements SecuredNotChild {

    private static final long serialVersionUID = 4357218100681569138L;
    private Collection<Characteristic> characteristics = new HashSet<Characteristic>();
    private DatabaseEntry sourceAccession;
    private Collection<BibliographicReference> literatureSources = new HashSet<BibliographicReference>();
    private Collection<GeneSetMember> members = new HashSet<GeneSetMember>();

    static public GeneSetMember containsGene( Gene g, GeneSet gs ) {
        for ( GeneSetMember gm : gs.getMembers() ) {
            if ( gm.getGene().equals( g ) )
                return gm;
        }
        return null;
    }

    public Collection<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Collection<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    public Collection<BibliographicReference> getLiteratureSources() {
        return this.literatureSources;
    }

    public void setLiteratureSources( Collection<BibliographicReference> literatureSources ) {
        this.literatureSources = literatureSources;
    }

    public Collection<GeneSetMember> getMembers() {
        return this.members;
    }

    public void setMembers( Collection<GeneSetMember> members ) {
        this.members = members;
    }

    public DatabaseEntry getSourceAccession() {
        return this.sourceAccession;
    }

    public void setSourceAccession( DatabaseEntry sourceAccession ) {
        this.sourceAccession = sourceAccession;
    }

    public static final class Factory {
        public static GeneSet newInstance() {
            return new GeneSet();
        }

    }

}
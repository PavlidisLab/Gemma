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

import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

import java.util.HashSet;
import java.util.Set;

/**
 * A grouping of genes that share a common relationship
 */
public class GeneSet extends AbstractAuditable implements SecuredNotChild {

    private Set<Characteristic> characteristics = new HashSet<>();
    private DatabaseEntry sourceAccession;
    private Set<BibliographicReference> literatureSources = new HashSet<>();
    private Set<GeneSetMember> members = new HashSet<>();

    static public GeneSetMember containsGene( Gene g, GeneSet gs ) {
        for ( GeneSetMember gm : gs.getMembers() ) {
            if ( gm.getGene().equals( g ) )
                return gm;
        }
        return null;
    }

    @Override
    public Long getId() {
        return super.getId();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    public Set<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }


    public Set<BibliographicReference> getLiteratureSources() {
        return this.literatureSources;
    }

    public void setLiteratureSources( Set<BibliographicReference> literatureSources ) {
        this.literatureSources = literatureSources;
    }

    public Set<GeneSetMember> getMembers() {
        return this.members;
    }

    public void setMembers( Set<GeneSetMember> members ) {
        this.members = members;
    }

    public DatabaseEntry getSourceAccession() {
        return this.sourceAccession;
    }

    public void setSourceAccession( DatabaseEntry sourceAccession ) {
        this.sourceAccession = sourceAccession;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof GeneSet ) )
            return false;
        GeneSet that = ( GeneSet ) object;
        if ( getId() != null && that.getId() != null )
            return getId().equals( that.getId() );
        return false;
    }

    public static final class Factory {
        public static GeneSet newInstance() {
            return new GeneSet();
        }

    }

}
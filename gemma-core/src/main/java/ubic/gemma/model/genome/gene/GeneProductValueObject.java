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
package ubic.gemma.model.genome.gene;

import ubic.gemma.model.common.IdentifiableValueObject;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class GeneProductValueObject extends IdentifiableValueObject<GeneProduct> {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1156628868995566223L;

    private String ncbiId;
    private String name;
    private Long geneId;
    private String chromosome;
    private String strand;
    private Long nucleotideStart;

    @Deprecated
    private Long nucleotideEnd;

    /**
     * Required when using the class as a spring bean.
     */
    public GeneProductValueObject() {
        super();
    }

    public GeneProductValueObject( Long id ) {
        super( id );
    }

    /**
     * Populates the VO properties with values from the given entity. Checks that physicalLocation is not-null
     * before accessing its properties.
     *
     * @param entity the GeneProduct to load the values from.
     */
    public GeneProductValueObject( GeneProduct entity ) {
        super( entity );
        this.name = entity.getName();
        this.ncbiId = entity.getNcbiGi();
        if ( entity.getPhysicalLocation() != null ) {
            if ( entity.getPhysicalLocation().getChromosome() != null ) {
                this.chromosome = entity.getPhysicalLocation().getChromosome().getName();
            }
            this.strand = entity.getPhysicalLocation().getStrand();
        }
    }

    public String getChromosome() {
        return this.chromosome;
    }

    public void setChromosome( String chromosome ) {
        this.chromosome = chromosome;
    }

    public Long getGeneId() {
        return this.geneId;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getNcbiId() {
        return this.ncbiId;
    }

    public void setNcbiId( String ncbiId ) {
        this.ncbiId = ncbiId;
    }

    @Deprecated
    public Long getNucleotideEnd() {
        return this.nucleotideEnd;
    }

    @Deprecated
    public void setNucleotideEnd( Long nucleotideEnd ) {
        this.nucleotideEnd = nucleotideEnd;
    }

    public Long getNucleotideStart() {
        return this.nucleotideStart;
    }

    public void setNucleotideStart( Long nucleotideStart ) {
        this.nucleotideStart = nucleotideStart;
    }

    public String getStrand() {
        return this.strand;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

}
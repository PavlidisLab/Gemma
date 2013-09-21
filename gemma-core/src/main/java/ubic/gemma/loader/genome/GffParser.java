/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.genome;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.util.SequenceBinUtils;

/**
 * Simple parser for GFF format (version 2). See http://www.sanger.ac.uk/Software/formats/GFF/GFF_Spec.shtml.
 * <p>
 * Fields are: &lt;seqname&gt; &lt;source&gt; &lt;feature&gt; &lt;start&lt; &lt;end&gt; &lt;score&gt; &lt;strand&gt;
 * &lt;frame&gt; [attributes] [comments]. A header can be present.
 * <p>
 * Implementation note: Currently each line is converted to a Gene with an associated GeneProduct; this will have to be
 * expanded/changed.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GffParser extends BasicLineParser<Gene> {
    Collection<Gene> results = new HashSet<Gene>();

    Taxon taxon;

    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    @Override
    protected void addResult( Gene obj ) {
        if ( results.contains( obj ) ) {
            log.warn( "Already have " + obj );
        }
        results.add( obj );
    }

    @Override
    public Collection<Gene> getResults() {
        return results;
    }

    @Override
    public Gene parseOneLine( String line ) {

        if ( this.taxon == null ) {
            throw new IllegalStateException( "You must set the taxon first" );
        }

        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        Gene newGene = Gene.Factory.newInstance();
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();

        String seqName = fields[0]; // chromosome
        // String source = fields[1];
        String featureType = fields[2];
        long start = Long.parseLong( fields[3] );
        long end = Long.parseLong( fields[4] );
        int length = ( int ) ( end - start );
        String strand = fields[6];

        String attributes = fields[8];

        newGene.setDescription( featureType );
        geneProduct.setDescription( featureType );

        String[] attFields = StringUtils.splitPreserveAllTokens( attributes, ';' );
        for ( int i = 0; i < attFields.length; i++ ) {

            String f = attFields[i];

            if ( f == null || f.length() == 0 ) {
                continue;
            }

            f = StringUtils.strip( f );
            log.debug( f );
            String[] subf = StringUtils.split( f, '=' );

            if ( subf.length != 2 ) {
                throw new IllegalArgumentException( "Couldn't parse '" + f + "'" );
            }

            String ti = subf[0];
            String val = subf[1];

            if ( ti.equals( "ID" ) ) {
                val = val.replaceAll( "\"", "" );
                newGene.setName( val );
                newGene.setOfficialSymbol( val );
                geneProduct.setName( val );
            } else if ( ti.equals( "ACC" ) ) {
                // don't know what database!
            }
        }

        // String comments = fields[9];

        Chromosome chromosome = Chromosome.Factory.newInstance(seqName, taxon); 

        PhysicalLocation location = PhysicalLocation.Factory.newInstance();
        location.setChromosome( chromosome );
        location.setNucleotide( start );
        location.setNucleotideLength( length );
        location.setBin( SequenceBinUtils.binFromRange( location.getNucleotide().intValue(), location.getNucleotide()
                .intValue() + location.getNucleotideLength().intValue() ) );
        location.setStrand( strand );

        geneProduct.setPhysicalLocation( location );
        geneProduct.setGene( newGene );

        newGene.setTaxon( taxon );
        newGene.setPhysicalLocation( location );
        newGene.getProducts().add( geneProduct );

        return newGene;

    }

}

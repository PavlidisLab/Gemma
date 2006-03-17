/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.genome.gene.ncbi;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneAlias;

/**
 * Convert NCBIGene2Accession objects into Gemma Gene objects.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneConverter implements Converter {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    public Collection<Object> convert( Collection<Object> sourceDomainObjects ) {
        Collection<Object> results = new HashSet<Object>();
        for ( Object object : sourceDomainObjects ) {
            results.add( this.convert( object ) );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Object convert( NCBIGeneInfo info ) {
        Gene gene = Gene.Factory.newInstance();

        gene.setNcbiId( info.getGeneId() );
        gene.setOfficialSymbol( info.getDefaultSymbol() );
        gene.setOfficialName( info.getDefaultSymbol() );
        gene.setDescription( info.getDescription() );

        Taxon t = Taxon.Factory.newInstance();
        t.setNcbiId( new Integer( info.getTaxId() ) );
        gene.setTaxon( t );

        // gene.setPhysicalLocation();
        // gene.setCytogenicLocation();
        // gene.setGeneticLocation();
        // gene.setProducts();
        // gene.setAccessions();

        // System.out.println("synonyms: "+info.getSynonyms());
        // System.out.println("aliases: "+gene.getGeneAliasses());
        // System.out.println("aliases null: "+(gene.getGeneAliasses()==null));
        Collection<GeneAlias> aliases = gene.getGeneAlias();
        for ( String alias : info.getSynonyms() ) {
            GeneAlias newAlias = GeneAlias.Factory.newInstance();
            newAlias.setGene( gene );
            newAlias.setSymbol( gene.getOfficialSymbol() );
            newAlias.setAlias( alias ); // added by AS - non-nullable, won't work w/o it!
            aliases.add( newAlias );
        }

        System.out.println( "added aliases" );
        return gene;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Object convert( Object sourceDomainObject ) {
        assert sourceDomainObject instanceof NCBIGene2Accession;
        NCBIGene2Accession ncbiGene = ( NCBIGene2Accession ) sourceDomainObject;
        return convert( ncbiGene.getInfo() );

    }
}

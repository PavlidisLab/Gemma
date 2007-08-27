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
package ubic.gemma.apps;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Class for CLIs that manipulate a list of genes
 * 
 * @author Raymond
 * @version $Id$
 */
public abstract class AbstractGeneCoexpressionManipulatingCLI extends AbstractGeneExpressionExperimentManipulatingCLI {
    protected GeneService geneService;

    private String[] queryGeneSymbols;
    private String queryGeneFile;

    private String[] targetGeneSymbols;
    private String targetGeneFile;

    protected Taxon taxon;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option queryGeneFileOption = OptionBuilder.hasArg().withDescription(
                "Query file containing list of gene official symbols" ).withArgName( "queryGeneFile" ).withLongOpt(
                "queryGeneFile" ).create();
        addOption( queryGeneFileOption );
        Option queryGeneOption = OptionBuilder.hasArgs().withArgName( "queryGeneSymbols" ).withDescription(
                "Query gene official symbol(s)" ).withLongOpt( "queryGene" ).create();
        addOption( queryGeneOption );

        Option targetFileOption = OptionBuilder.hasArg().withArgName( "targetGeneFile" ).withDescription(
                "File containing list of target gene official symbols" ).withLongOpt( "targetGeneFile" ).create();
        addOption( targetFileOption );
        Option targetGeneOption = OptionBuilder.hasArgs().withArgName( "targetGeneSymbols" ).withDescription(
                "Target gene official symbol(s)" ).withLongOpt( "targetGene" ).create();
        addOption( targetGeneOption );

        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "taxon" ).withDescription(
                "The taxon of the genes" ).withLongOpt( "taxon" ).create( 't' );
        addOption( taxonOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( "queryGeneFile" ) ) queryGeneFile = getOptionValue( "queryGeneFile" );
        if ( hasOption( "queryGene" ) ) queryGeneSymbols = getOptionValues( "queryGene" );

        if ( hasOption( "targetGeneFile" ) ) targetGeneFile = getOptionValue( "targetGeneFile" );
        if ( hasOption( "targetGene" ) ) targetGeneSymbols = getOptionValues( "targetGene" );

        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        String taxonName = getOptionValue( 't' );
        taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        taxon = taxonService.find( taxon );

        geneService = ( GeneService ) getBean( "geneService" );
    }

    public Taxon getTaxon() {
        return taxon;
    }

    public Collection<Gene> getTargetGenes() throws IOException {
        Collection<Gene> genes = new HashSet<Gene>();
        if ( targetGeneFile != null ) genes.addAll( readGeneListFile( targetGeneFile, taxon ) );
        if ( targetGeneSymbols != null ) {
            for ( int i = 0; i < targetGeneSymbols.length; i++ ) {
                genes.add( findGeneByOfficialSymbol( targetGeneSymbols[i], taxon ) );
            }
        }
        return genes;
    }

    public Collection<Gene> getQueryGenes() throws IOException {
        Collection<Gene> genes = new HashSet<Gene>();
        if ( queryGeneFile != null ) genes.addAll( readGeneListFile( queryGeneFile, taxon ) );
        if ( queryGeneSymbols != null ) {
            for ( int i = 0; i < queryGeneSymbols.length; i++ ) {
                genes.add( findGeneByOfficialSymbol( queryGeneSymbols[i], taxon ) );
            }
        }

        return genes;
    }

    protected Map<String, String> getGeneIdPair2NameMap( Collection<Gene> queryGenes, Collection<Gene> targetGenes ) {
        Map<String, String> map = new HashMap<String, String>();
        for ( Gene qGene : queryGenes ) {
            String qName = ( qGene.getOfficialSymbol() != null ) ? qGene.getOfficialSymbol() : qGene.getId().toString();
            for ( Gene tGene : targetGenes ) {
                String tName = ( tGene.getOfficialSymbol() != null ) ? tGene.getOfficialSymbol() : tGene.getId()
                        .toString();
                map.put( qGene.getId() + ":" + tGene.getId(), qName + ":" + tName );
            }
        }
        return map;
    }

}

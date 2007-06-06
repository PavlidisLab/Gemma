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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.analysis.linkAnalysis.FrequentLinkSetFinder;
import ubic.gemma.analysis.linkAnalysis.LinkGraphClustering;
import ubic.gemma.analysis.linkAnalysis.MetaLinkFinder;
import ubic.gemma.analysis.linkAnalysis.TreeNode;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.visualization.GraphViewer;
import cern.colt.list.ObjectArrayList;

/**
 * @author xwan
 * @version $Id$
 */
public class MetaLinkFinderCli extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    private boolean writeClusteringTree = false;
    private boolean writeLinkMatrix = false;
    private String matrixFile = null, eeMapFile = null, treeFile = null;
    private Taxon taxon = null;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option writeLinkMatrix = OptionBuilder.withDescription(
                "Giving this option will generate the new link matrix, Otherwise reading the link matrix from file" )
                .withLongOpt( "linkMatrix" ).create( 'l' );
        addOption( writeLinkMatrix );
        Option writeTree = OptionBuilder.withDescription(
                "Giving this option will generate the new clustering tree, Otherwise reading the tree from file" )
                .withLongOpt( "clusteringTree" ).create( 'c' );
        addOption( writeTree );
        Option matrixFile = OptionBuilder.hasArg().withArgName( "Bit Matrixfile" ).isRequired().withDescription(
                "The file for saving bit matrix" ).withLongOpt( "matrixfile" ).create( 'm' );
        addOption( matrixFile );

        Option mapFile = OptionBuilder.hasArg().withArgName( "Expression Experiment Map File" ).isRequired()
                .withDescription( "The File for Saving the Expression Experiment Mapping" ).withLongOpt( "mapfile" )
                .create( 'e' );
        addOption( mapFile );

        Option treeFile = OptionBuilder.hasArg().withArgName( "Clustering Tree File" ).isRequired().withDescription(
                "The file for saving clustering tree" ).withLongOpt( "treefile" ).create( 't' );
        addOption( treeFile );

        Option species = OptionBuilder.hasArg().withArgName( "The name of the species" ).isRequired().withDescription(
                "The name of the species" ).withLongOpt( "species" ).create( 's' );
        addOption( species );

    }

    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'l' ) ) {
            this.writeLinkMatrix = true;
        }

        if ( hasOption( 'c' ) ) {
            this.writeClusteringTree = true;
        }

        if ( hasOption( 'm' ) ) {
            this.matrixFile = getOptionValue( 'm' );
        }

        if ( hasOption( 'e' ) ) {
            this.eeMapFile = getOptionValue( 'e' );
        }

        if ( hasOption( 't' ) ) {
            this.treeFile = getOptionValue( 't' );
        }
        if ( hasOption( 's' ) ) {
            String speciesName = getOptionValue( 's' );
            taxon = this.getTaxon( speciesName );
        }
    }

    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        return taxon;
    }

    private void test() {
        CompressedNamedBitMatrix matrix = new CompressedNamedBitMatrix( 21, 11, 125 );
        for ( int i = 0; i < 21; i++ )
            matrix.addRowName( new Long( i ) );
        for ( int i = 0; i < 11; i++ )
            matrix.addColumnName( new Long( i ) );
        matrix.set( 0, 0, 0 );
        matrix.set( 0, 0, 12 );
        matrix.set( 0, 0, 24 );
        matrix.set( 20, 0, 0 );
        matrix.set( 20, 0, 12 );
        matrix.set( 20, 0, 24 );
        matrix.set( 0, 10, 0 );
        matrix.set( 0, 10, 12 );
        matrix.set( 0, 10, 24 );
        matrix.set( 20, 10, 0 );
        matrix.set( 20, 10, 12 );
        matrix.set( 20, 10, 24 );
        try {
            matrix.toFile( "test.File" );
        } catch ( IOException e ) {
            System.out.println( e.getMessage() );
        }
    }

    void interactiveQuery() {
        BufferedReader bfr = new BufferedReader( new InputStreamReader( System.in ) );
        String geneName;
        int count = 0;
        try {
            // Hit CTRL-Z on PC's to send EOF, CTRL-D on Unix
            while ( true ) {
                // Read a character from keyboard
                System.out.println( "The Gene ID: (Press CTRL-Z or CTRL-D to Stop)" );
                System.out.print( ">" );
                geneName = bfr.readLine();
                if ( geneName == null ) break;
                System.out.print( "The Stringency:" );
                String tmp = bfr.readLine();
                if ( tmp == null ) break;
                count = Integer.valueOf( tmp.trim() ).intValue();
                // Gene gene = geneService.load(Long.valueOf(geneName).longValue());
                Gene gene = MetaLinkFinder.getGene( geneName, taxon );
                if ( gene != null ) {
                    System.out.println( "Got " + geneName + " " + count );
                    MetaLinkFinder.output( gene, count );
                } else
                    System.out.println( "Gene doesn't exist" );
            }
        } catch ( IOException ioe ) {
            System.out.println( "IO error:" + ioe );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        // GraphVisualization graphVisualization1 = new GraphVisualization(new Object[0]);
        // graphVisualization1.run();
        // if(1== 1) return null;
        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }
        try {
            ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                    .getBean( "expressionExperimentService" );
            GeneService geneService = ( GeneService ) this.getBean( "geneService" );
            GeneOntologyService geneOntologyService = ( GeneOntologyService ) this.getBean( "geneOntologyService" );
            Gene2GOAssociationService gene2GoAssociationService = ( Gene2GOAssociationService ) this
                    .getBean( "gene2GOAssociationService" );

            MetaLinkFinder linkFinder = new MetaLinkFinder();
            linkFinder.setEeService( eeService );
            linkFinder.setGeneService( geneService );
            linkFinder.setGeneOntologyService( geneOntologyService );
            linkFinder.setGene2GoAssociationService( gene2GoAssociationService );
            if ( taxon == null ) {
                return new Exception( "The input species couldn't be found" );
            }
            StopWatch watch = new StopWatch();

            // load the link matrix
            if ( this.writeLinkMatrix ) {
                watch.start();
                linkFinder.find( taxon );
                try {
                    linkFinder.toFile( this.matrixFile, this.eeMapFile );
                } catch ( IOException e ) {
                    log.info( "Couldn't save the results into the files " );
                    return e;
                }
                log.info( "Spend " + watch.getTime() / 1000 + " to generate link matrix" );
            } else {
                watch.start();
                try {
                    linkFinder.fromFile( this.matrixFile, this.eeMapFile );
                } catch ( IOException e ) {
                    log.info( "Couldn't load the data from the files " );
                    return e;
                }
                watch.stop();
                log.info( "Spend " + watch.getTime() / 1000 + " to load the data matrix" );
            }
            System.err.println( "Finish Loading!" );
            watch.reset();
            watch.start();

            LinkGraphClustering clustering = new LinkGraphClustering( 6 );
            // clustering.testSerilizable();
            if ( this.writeClusteringTree ) {
                clustering.run();
                clustering.saveToFile( this.treeFile );
            } else {
                clustering.readTreeFromFile( this.treeFile );
            }
            ObjectArrayList savedClusters = clustering.selectClustersToSave( 20 );
            GraphViewer gviewer = new GraphViewer( savedClusters, true );
            gviewer.run();

            ObjectArrayList leafNodes = new ObjectArrayList();
            // for(int i = 0; i < savedClusters.size(); i++){
            // //if(i < 3) continue;
            // TreeNode clusterRoot = (TreeNode) savedClusters.getQuick(i);
            // leafNodes.clear();
            // LinkGraphClustering.collectTreeNodes(leafNodes, new ObjectArrayList(), clusterRoot);
            // System.err.println("Cluster"+(i+1)+"_" + leafNodes.size() +" :");
            // GraphVisualization graphVisualization = new GraphVisualization(leafNodes.toList().toArray());
            // if(i == 6 || i == 7)
            // graphVisualization.run();
            // }

            // Select clusters for frequent linkset finder
            TreeNode testNode = clustering.selectClusterWithMaximalBits( 6 );
            leafNodes.clear();
            // testNode = clustering.selectMaximalCluster();
            LinkGraphClustering.collectTreeNodes( leafNodes, new ObjectArrayList(), testNode );
            GraphViewer gviewer1 = new GraphViewer( leafNodes, false );
            gviewer1.run();
            FrequentLinkSetFinder freFinder = new FrequentLinkSetFinder( 6 );
            freFinder.find( leafNodes );
            watch.stop();
            log.info( "Spend " + watch.getTime() / 1000 + " to Generated " + FrequentLinkSetFinder.nodeNum + " nodes" );
            /*
             * MetaLinkFinder.saveLinkMatrix("linkMatrix.txt", 6); System.err.println( "Output some stats" );
             * MetaLinkFinder.outputStat(); interactiveQuery();
             */
        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        MetaLinkFinderCli linkFinderCli = new MetaLinkFinderCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = linkFinderCli.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}

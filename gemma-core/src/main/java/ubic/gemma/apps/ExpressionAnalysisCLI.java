/**
 * 
 */
package ubic.gemma.apps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Create a relative expression level (dedv rank) matrix for a list of genes
 * 
 * @author raymond
 */
public class ExpressionAnalysisCLI extends AbstractGeneManipulatingCLI {
    private String outFile;

    private String inFile;

    private Taxon taxon;

    private ExpressionExperimentService eeService;

    private ArrayDesignService adService;

    private double filterThreshold;

    public static final double DEFAULT_FILTER_THRESHOLD = 0.8;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        Option inFileOption = OptionBuilder.hasArg().withArgName( "inFile" ).withDescription(
                "File containing list of genes in offical symbols" ).withLongOpt( "inFile" ).create( 'i' );
        addOption( inFileOption );

        Option outFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File to save rank matrix to" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outFileOption );

        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );

        Option filterOption = OptionBuilder.hasArg().withArgName( "filterThreshold" ).withDescription(
                "Fraction of data sets with ranks threshold" ).withLongOpt( "filterThreshold" ).create( 'f' );
        addOption( filterOption );
    }

    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'i' ) ) {
            inFile = getOptionValue( 'i' );
        }

        if ( hasOption( 'o' ) ) {
            outFile = getOptionValue( 'o' );
        }
        if ( hasOption( 'f' ) ) {
            filterThreshold = Double.parseDouble( getOptionValue( 'f' ) );
        } else {
            filterThreshold = DEFAULT_FILTER_THRESHOLD;
        }

        String taxonName = getOptionValue( 't' );
        taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( taxonName );
        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        initBeans();
    }

    protected void initBeans() {
        super.initBeans();
        eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        adService = ( ArrayDesignService ) getBean( "arrayDesignService" );
    }

    private DenseDoubleMatrix2DNamed getRankMatrix( Collection<Gene> genes, Collection<ExpressionExperiment> EEs ) {
        DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed( genes.size(), EEs.size() );
        for ( Gene gene : genes ) {
            String name = gene.getId().toString();
            matrix.addRowName( name );
        }
        for ( ExpressionExperiment EE : EEs ) {
            String name = EE.getShortName();
            matrix.addColumnName( name );
        }

        int eeCount = 1;
        for ( ExpressionExperiment EE : EEs ) {
            int col = matrix.getColIndexByName( EE.getShortName() );
            log.info( "Processing " + EE.getShortName() + " (" + eeCount++ + " of " + EEs.size() + ")" );
            Collection<ArrayDesign> ADs = eeService.getArrayDesignsUsed( EE );
            Collection<Long> csIDs = new HashSet<Long>();
            for ( ArrayDesign ad : ADs ) {
                for ( CompositeSequence cs : ( Collection<CompositeSequence> ) adService.loadCompositeSequences( ad ) ) {
                    csIDs.add( cs.getId() );
                }
            }
            Map<Long, Collection<Long>> cs2geneMap = geneService.getCS2GeneMap( csIDs );
            QuantitationType qt = ( QuantitationType ) eeService.getPreferredQuantitationType( EE ).iterator().next();
            Map<DesignElementDataVector, Collection<Long>> dedv2geneMap = eeService.getDesignElementDataVectors(
                    cs2geneMap, qt );

            // invert dedv2geneMap
            Map<Long, Collection<DesignElementDataVector>> gene2dedvMap = new HashMap<Long, Collection<DesignElementDataVector>>();
            for ( DesignElementDataVector dedv : dedv2geneMap.keySet() ) {
                Collection<Long> geneIds = dedv2geneMap.get( dedv );
                for ( Long geneId : geneIds ) {
                    Collection<DesignElementDataVector> dedvs = gene2dedvMap.get( dedv );
                    if ( dedvs == null ) {
                        dedvs = new HashSet<DesignElementDataVector>();
                        gene2dedvMap.put( geneId, dedvs );
                    }
                    dedvs.add( dedv );
                }

            }
            log.info( "Loaded design element data vectors" );

            int rankCount = 0;
            String line = EE.getShortName();
            for ( Gene gene : genes ) {
                int row = matrix.getRowIndexByName( gene.getId() );
                line += "\t";
                Double rank;
                List<Double> ranks = new ArrayList<Double>();
                Collection<DesignElementDataVector> dedvs = gene2dedvMap.get( gene.getId() );
                if ( dedvs == null ) continue;
                for ( DesignElementDataVector dedv : dedvs ) {
                    ranks.add( dedv.getRank() );
                }
                if ( ranks.size() < 1 ) continue;

                // take the median rank
                Collections.sort( ranks );
                rank = ranks.get( ranks.size() / 2 );
                if ( rank == null ) continue;
                matrix.set( row, col, rank );
                rankCount++;
            }
            log.info( "Saved " + rankCount + " gene ranks" );
        }

        return matrix;
    }

    private DenseDoubleMatrix2DNamed filterRankmatrix( DenseDoubleMatrix2DNamed matrix ) {
        // filter out genes with less than filterThreshold fraction of ranks
        List fRowNames = new ArrayList();
        for ( Object rowName : matrix.getRowNames() ) {
            int row = matrix.getRowIndexByName( rowName );
            int count = 0;
            int total = matrix.columns();
            for ( Object colName : matrix.getColNames() ) {
                int col = matrix.getColIndexByName( colName );
                if ( !Double.isNaN( matrix.get( row, col ) ) ) count++;
            }
            if ( count / total > filterThreshold ) fRowNames.add( rowName );
        }

        // filter out data sets with no ranks
        List fColNames = new ArrayList();
        for ( Object colName : matrix.getColNames() ) {
            int col = matrix.getColIndexByName( colName );
            boolean found = false;
            for ( Object rowName : fRowNames ) {
                int row = matrix.getRowIndexByName( rowName );
                if ( !Double.isNaN( matrix.get( row, col ) ) ) {
                    found = true;
                    break;
                }
            }
            if ( found ) fColNames.add( colName );
        }

        // fill matrix
        DenseDoubleMatrix2DNamed fMatrix = new DenseDoubleMatrix2DNamed( fRowNames.size(), fColNames.size() );
        fMatrix.setRowNames( fRowNames );
        fMatrix.setColumnNames( fColNames );
        for ( Object rowName : fRowNames ) {
            int fRow = fMatrix.getRowIndexByName( rowName );
            int row = matrix.getRowIndexByName( rowName );
            for ( Object colName : fColNames ) {
                int fCol = fMatrix.getColIndexByName( colName );
                int col = matrix.getColIndexByName( colName );
                double val = matrix.get( row, col );
                fMatrix.set( fRow, fCol, val );
            }
        }

        return fMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "ExpressionAnalysis", args );
        if ( e != null ) return e;

        Collection<Gene> genes;
        if ( inFile != null ) {
            try {
                genes = readGeneListFile( inFile, taxon );
            } catch ( IOException exc ) {
                return exc;
            }
        } else {
            genes = geneService.getGenesByTaxon( taxon );
        }

        Collection<ExpressionExperiment> EEs = eeService.findByTaxon( taxon );

        DenseDoubleMatrix2DNamed rankMatrix = getRankMatrix( genes, EEs );
        rankMatrix = filterRankmatrix( rankMatrix );
        try {
            MatrixWriter out = new MatrixWriter( outFile, new DecimalFormat( "0.0000" ) );
            out.writeMatrix( rankMatrix, "Gene" );
        } catch ( IOException exc ) {
            return exc;
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExpressionAnalysisCLI analysis = new ExpressionAnalysisCLI();
        StopWatch watch = new StopWatch();
        watch.start();

        log.info( "Starting expression analysis" );
        Exception e = analysis.doWork( args );
        if ( e != null ) log.error( e.getMessage() );
        watch.stop();
        log.info( "Finished expression analysis in " + watch.getTime() / 1000 + " seconds" );
    }

}

/**
 * 
 */
package ubic.gemma.apps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

/**
 * Create a relative expression level (dedv rank) matrix for a list of genes
 * 
 * @author raymond
 */
public class ExpressionAnalysisCLI extends AbstractGeneCoexpressionManipulatingCLI {
    private String outFilePrefix;

    private ArrayDesignService adService;

    private DesignElementDataVectorService dedvService;

    private double filterThreshold;

    public static final double DEFAULT_FILTER_THRESHOLD = 0.8;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option outFileOption = OptionBuilder.create( 'o' );
        addOption( outFileOption );

        Option filterOption = OptionBuilder.create( "threshold" );
        addOption( filterOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'o' ) ) {
            outFilePrefix = getOptionValue( 'o' );
        }
        if ( hasOption( "threshold" ) ) {
            filterThreshold = Double.parseDouble( getOptionValue( "threshold" ) );
        } else {
            filterThreshold = DEFAULT_FILTER_THRESHOLD;
        }

        initBeans();
    }

    /**
     * 
     */
    protected void initBeans() {
        eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        adService = ( ArrayDesignService ) getBean( "arrayDesignService" );
        dedvService = ( DesignElementDataVectorService ) getBean( "designElementDataVectorService" );
    }

    /**
     * @param genes
     * @param ees
     * @return
     */
    @SuppressWarnings("unchecked")
    private DenseDoubleMatrix2DNamed getRankMatrix( Collection<Gene> genes, Collection<ExpressionExperiment> ees ) {
        DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed( genes.size(), ees.size() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.set( i, j, Double.NaN );
            }
        }
        // name rows + cols
        for ( Gene gene : genes ) {
            matrix.addRowName( gene );
        }
        for ( ExpressionExperiment ee : ees ) {
            matrix.addColumnName( ee );
        }

        int eeCount = 1;
        for ( ExpressionExperiment ee : ees ) {
            int col = matrix.getColIndexByName( ee );
            log.info( "Processing " + ee.getShortName() + " (" + eeCount++ + " of " + ees.size() + ")" );
            Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed( ee );
            Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
            for ( ArrayDesign ad : ads ) {
                css.addAll( adService.loadCompositeSequences( ad ) );
            }
            Collection<QuantitationType> qts = eeService.getPreferredQuantitationType( ee );
            if ( qts.size() == 0 ) {
                log.info( "No preferred quantitation type" );
                continue;
            }
            QuantitationType qt = qts.iterator().next();
            Collection<DesignElementDataVector> dedvs = eeService.getDesignElementDataVectors( qts );
            Map<DesignElementDataVector, Collection<Gene>> dedv2geneMap = dedvService.getDedv2GenesMap( dedvs, qt );

            // invert dedv2geneMap
            Map<Gene, Collection<DesignElementDataVector>> gene2dedvMap = new HashMap<Gene, Collection<DesignElementDataVector>>();
            for ( DesignElementDataVector dedv : dedv2geneMap.keySet() ) {
                Collection<Gene> c = dedv2geneMap.get( dedv );
                for ( Gene gene : c ) {
                    Collection<DesignElementDataVector> vs = gene2dedvMap.get( dedv );
                    if ( vs == null ) {
                        vs = new HashSet<DesignElementDataVector>();
                        gene2dedvMap.put( gene, vs );
                    }
                    vs.add( dedv );
                }

            }
            log.info( "Loaded design element data vectors" );

            // construct the rank matrix
            int rankCount = 0;
            String line = ee.getShortName();
            for ( Gene gene : genes ) {
                int row = matrix.getRowIndexByName( gene );
                line += "\t";
                Double rank;
                List<Double> ranks = new ArrayList<Double>();
                Collection<DesignElementDataVector> vs = gene2dedvMap.get( gene );
                if ( vs == null ) continue;
                for ( DesignElementDataVector dedv : vs ) {
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

    /**
     * @param matrix
     * @return
     */
    private DenseDoubleMatrix2DNamed filterRankmatrix( DenseDoubleMatrix2DNamed matrix ) {
        // filter out genes with less than filterThreshold fraction of ranks
        List<Object> fRowNames = new ArrayList<Object>();
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
        List<Object> fColNames = new ArrayList<Object>();
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
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "ExpressionAnalysis", args );
        if ( e != null ) return e;

        Collection<Gene> genes;

        log.info( "Getting genes" );
        genes = geneService.loadKnownGenes( taxon );
        log.info( "Loaded " + genes.size() + " genes" );

        DenseDoubleMatrix2DNamed rankMatrix = getRankMatrix( genes, expressionExperiments );
        // rankMatrix = filterRankmatrix(rankMatrix);

        // gene names
        Collection<Gene> rowGenes = rankMatrix.getRowNames();
        try {
            PrintWriter out = new PrintWriter( new FileWriter( outFilePrefix + ".row_names.txt" ) );
            for ( Gene gene : rowGenes ) {
                String s = gene.getOfficialSymbol();
                if ( s == null ) s = gene.getId().toString();
                out.println( s );
            }
            out.close();
        } catch ( IOException exc ) {
            return exc;
        }

        // expression experiment names
        Collection<ExpressionExperiment> colEes = rankMatrix.getColNames();
        try {
            PrintWriter out = new PrintWriter( new FileWriter( outFilePrefix + ".col_names.txt" ) );
            for ( ExpressionExperiment ee : colEes ) {
                out.println( ee.getShortName() );
            }
            out.close();
        } catch ( IOException exc ) {
            return exc;
        }

        DecimalFormat formatter = ( DecimalFormat ) NumberFormat.getNumberInstance( Locale.US );
        formatter.applyPattern( "0.0000" );
        formatter.getDecimalFormatSymbols().setNaN( "NaN" );
        try {
            MatrixWriter out = new MatrixWriter( outFilePrefix + ".txt", formatter );
            out.writeMatrix( rankMatrix, false );
            out.close();
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
        log.info( "Finished expression analysis in " + watch );
    }

}

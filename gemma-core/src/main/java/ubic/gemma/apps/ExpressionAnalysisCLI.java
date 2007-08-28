/**
 * 
 */
package ubic.gemma.apps;

import java.io.IOException;
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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;

/**
 * Create a relative expression level (dedv rank) matrix for a list of genes
 * 
 * @author raymond
 */
public class ExpressionAnalysisCLI extends AbstractGeneCoexpressionManipulatingCLI {
    private String outFile;

    private Taxon taxon;

    private ExpressionExperimentService eeService;

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

        Option outFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File to save rank matrix to" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outFileOption );
        
        Option filterOption = OptionBuilder.hasArg().withArgName( "filterThreshold" ).withDescription(
                "Fraction of data sets with ranks threshold" ).withLongOpt( "filterThreshold" ).create( 'f' );
        addOption( filterOption );
    }

    protected void processOptions() {
        super.processOptions();
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
        eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        adService = ( ArrayDesignService ) getBean( "arrayDesignService" );
        dedvService = (DesignElementDataVectorService) getBean("designElementDataVectorService");
    }

    private DenseDoubleMatrix2DNamed getRankMatrix( Collection<Gene> genes, Collection<ExpressionExperiment> ees ) {
        DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed( genes.size(), ees.size() );
        // name rows + cols
        for ( Gene gene : genes ) {
            matrix.addRowName( gene.getId() );
        }
        for ( ExpressionExperiment ee : ees ) {
            matrix.addColumnName( ee.getId() );
        }

        int eeCount = 1;
        for ( ExpressionExperiment ee : ees ) {
            int col = matrix.getColIndexByName( ee.getId() );
            log.info( "Processing " + ee.getShortName() + " (" + eeCount++ + " of " + ees.size() + ")" );
            Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed( ee );
            Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
            for ( ArrayDesign ad : ads ) {
                css.addAll( adService.loadCompositeSequences( ad ) );
            }
            Map<CompositeSequence, Collection<Gene>> cs2geneMap = geneService.getCS2GeneMap( css );
            Collection<QuantitationType> qts =  eeService.getPreferredQuantitationType( ee );
            QuantitationType qt = qts.iterator().next();
            Collection<DesignElementDataVector> dedvs = eeService.getDesignElementDataVectors( ee, qts );
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
                int row = matrix.getRowIndexByName( gene.getId() );
                line += "\t";
                Double rank;
                List<Double> ranks = new ArrayList<Double>();
                Collection<DesignElementDataVector> vs = gene2dedvMap.get( gene.getId() );
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
        Collection<ExpressionExperiment> ees;
        try {
            ees = getExpressionExperiments( taxon );
            genes = getTargetGenes();
        } catch ( IOException exc ) {
            return exc;
        }

        // create row/col name maps for matrix output
        Map<Long, String> geneId2nameMap = new HashMap<Long, String>();
        for ( Gene gene : genes ) {
            if (gene != null && gene.getOfficialSymbol() != null)
                geneId2nameMap.put( gene.getId(), gene.getOfficialSymbol() );
        }
        Map<Long, String> eeId2nameMap = new HashMap<Long, String>();
        for ( ExpressionExperiment ee : ees ) {
            eeId2nameMap.put( ee.getId(), ee.getShortName() );
        }

        DenseDoubleMatrix2DNamed rankMatrix = getRankMatrix( genes, ees );
        rankMatrix = filterRankmatrix( rankMatrix );
        try {
            MatrixWriter out = new MatrixWriter( outFile, new DecimalFormat( "0.0000" ) );
            out.setRowNameMap( geneId2nameMap );
            out.setColNameMap( eeId2nameMap );
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

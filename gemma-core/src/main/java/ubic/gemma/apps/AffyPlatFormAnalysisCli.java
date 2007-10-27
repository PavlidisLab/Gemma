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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.AbstractSpringAwareCLI;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author xwan
 * @version $Id$
 */
public class AffyPlatFormAnalysisCli extends AbstractSpringAwareCLI {

    private class SortedElement implements Comparable<SortedElement> {
        private Double mean, min, max, median, std, presentAbsentCall;
        private DesignElement de;

        public SortedElement( DesignElement de, double min, double max, double mean, double median, double std,
                double presentAbsentCall ) {
            this.de = de;
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.median = median;
            this.std = std;
            this.presentAbsentCall = presentAbsentCall;
        }

        public int compareTo( SortedElement o ) {
            return median.compareTo( o.median );
        }

        public Double getMax() {
            return max;
        }

        public Double getMean() {
            return mean;
        }

        public Double getMedian() {
            return median;
        }

        public Double getMin() {
            return min;
        }

        public Double getStd() {
            return std;
        }

        public DesignElement getDE() {
            return de;
        }

        public Double getPresentAbsentCall() {
            return presentAbsentCall;
        }
    }

    public static final int MIN = 1;
    public static final int MAX = 2;
    public static final int MEDIAN = 3;
    public static final int MEAN = 4;
    public static final int STD = 5;

    private String arrayDesignName = null;
    private String outFileName = null;
    private Map<DesignElement, DoubleArrayList> rankData = new HashMap<DesignElement, DoubleArrayList>();
    private Map<DesignElement, DoubleArrayList> presentAbsentData = new HashMap<DesignElement, DoubleArrayList>();
    private DesignElementDataVectorService devService = null;
    private ExpressionExperimentService eeService = null;
    private Map<Object, Set> probeToGeneAssociation = null;

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'a' ) ) {
            this.arrayDesignName = getOptionValue( 'a' );
        }
        if ( hasOption( 'o' ) ) {
            this.outFileName = getOptionValue( 'o' );
        }
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option ADOption = OptionBuilder.hasArg().isRequired().withArgName( "arrayDesign" ).withDescription(
                "Array Design Short Name (GPLXXX) " ).withLongOpt( "arrayDesign" ).create( 'a' );
        addOption( ADOption );
        Option OutOption = OptionBuilder.hasArg().isRequired().withArgName( "outputFile" ).withDescription(
                "The name of the file to save the output " ).withLongOpt( "outputFile" ).create( 'o' );
        addOption( OutOption );
    }

    @SuppressWarnings("unchecked")
    private QuantitationType getQuantitationType( ExpressionExperiment ee, StandardQuantitationType requiredQT,
            boolean isPreferedQT ) {
        QuantitationType qtf = null;
        Collection<QuantitationType> eeQT = this.eeService.getQuantitationTypes( ee );
        for ( QuantitationType qt : eeQT ) {
            if ( isPreferedQT ) {
                if ( qt.getIsPreferred() ) {
                    qtf = qt;
                    StandardQuantitationType tmpQT = qt.getType();
                    if ( tmpQT != StandardQuantitationType.AMOUNT ) {
                        log.warn( "Preferred Quantitation Type may not be correct." + ee.getShortName() + ":"
                                + tmpQT.toString() );
                    }
                    break;
                }
            } else {
                if ( qt.getType().equals( requiredQT ) ) {
                    qtf = qt;
                    break;
                }
            }
        }
        if ( qtf == null ) {
            log.info( "Expression Experiment " + ee.getShortName() + " doesn't have required QT " );
        }
        return qtf;
    }

    @SuppressWarnings("unchecked")
    String processEEForPercentage( ExpressionExperiment ee ) {
        // eeService.thaw( ee );
        QuantitationType qt = this.getQuantitationType( ee, StandardQuantitationType.PRESENTABSENT, false );
        if ( qt == null ) return ( "No usable quantitation type in " + ee.getShortName() );
        log.info( "Load Data for  " + ee.getShortName() );

        Collection<DesignElementDataVector> dataVectors = devService.find( qt );
        if ( dataVectors == null ) return ( "No data vector " + ee.getShortName() );
        ByteArrayConverter bac = new ByteArrayConverter();

        for ( DesignElementDataVector vector : dataVectors ) {
            DesignElement de = vector.getDesignElement();
            DoubleArrayList presentAbsentList = this.presentAbsentData.get( de );
            if ( presentAbsentList == null ) {
                // return (" EE data vectors don't match array design for probe " + de.getName());
                continue;
            }
            byte[] bytes = vector.getData();
            // String vals = bac.byteArrayToAsciiString( bytes );
            char[] chars = bac.byteArrayToChars( bytes );
            double presents = 0;
            double total = 0;
            for ( int i = 0; i < chars.length; i++ ) {
                if ( chars[i] == 'P' ) presents++;
                if ( chars[i] != '\t' ) total++;
            }
            presentAbsentList.add( presents / total );
        }
        return null;
    }

    // From start to
    // end-1
    @SuppressWarnings("unchecked")
    private Map<Object, Set> getDevToGeneAssociation( Object[] allVectors, int start, int end ) {
        Collection<DesignElementDataVector> someVectors = new HashSet<DesignElementDataVector>();
        Map<Object, Set> returnAssocation = null;
        for ( int i = start; i < end; i++ ) {
            someVectors.add( ( DesignElementDataVector ) allVectors[i] );
        }
        returnAssocation = this.devService.getGenes( someVectors );
        return returnAssocation;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Set> getProbeToGeneAssociation( Collection<DesignElementDataVector> dataVectors ) {
        Map<Object, Set> association = new HashMap<Object, Set>();
        Object[] allVectors = dataVectors.toArray();
        int ChunkNum = 1000;
        int end = allVectors.length > ChunkNum ? ChunkNum : allVectors.length;
        association.putAll( this.getDevToGeneAssociation( allVectors, 0, end ) );

        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting the query to get the mapping between probe and gene" );
        for ( int i = 0; i < allVectors.length; i++ ) {
            if ( i >= end ) {
                int start = end;
                end = allVectors.length > end + ChunkNum ? end + ChunkNum : allVectors.length;
                System.err.println( start + " " + end );
                association.putAll( this.getDevToGeneAssociation( allVectors, start, end ) );
            }
        }
        Map<Object, Set> probeToGeneAssociation = new HashMap<Object, Set>();

        for ( Object dev : association.keySet() ) {
            Set<Gene> mappedGenes = association.get( dev );
            probeToGeneAssociation.put( ( ( DesignElementDataVector ) dev ).getDesignElement(), mappedGenes );
        }

        return probeToGeneAssociation;
    }

    @SuppressWarnings("unchecked")
    String processEE( ExpressionExperiment ee ) {
        // eeService.thaw( ee );
        QuantitationType qt = this.getQuantitationType( ee, null, true );
        if ( qt == null ) return ( "No usable quantitation type in " + ee.getShortName() );
        log.info( "Load Data for  " + ee.getShortName() );

        Collection<DesignElementDataVector> dataVectors = devService.find( qt );
        if ( dataVectors == null ) return ( "No data vector " + ee.getShortName() );
        if ( this.probeToGeneAssociation == null ) {
            this.probeToGeneAssociation = getProbeToGeneAssociation( dataVectors );
        }

        for ( DesignElementDataVector vector : dataVectors ) {
            DesignElement de = vector.getDesignElement();
            DoubleArrayList rankList = this.rankData.get( de );
            if ( rankList == null ) {
                return ( " EE data vectors don't match array design for probe " + de.getName() );
            }
            Double rank = vector.getRank();
            if ( rank != null ) {
                rankList.add( rank.doubleValue() );
            }
        }
        return null;
    }

    private double getStatValue( DoubleArrayList valList, int method ) {
        double value = 0.0;
        switch ( method ) {
            case MIN:
                value = DescriptiveWithMissing.min( valList );
                break;
            case MAX:
                value = DescriptiveWithMissing.max( valList );
                break;
            case MEAN:
                value = DescriptiveWithMissing.mean( valList );
                break;
            case MEDIAN:
                value = DescriptiveWithMissing.median( valList );
                break;
            case STD:
                int N = valList.size();
                double sum = DescriptiveWithMissing.sum( valList );
                double ss = DescriptiveWithMissing.sumOfSquares( valList );
                value = Descriptive.standardDeviation( DescriptiveWithMissing.variance( N, sum, ss ) );
                break;
        }
        if ( Double.isNaN( value ) ) value = 0.0;
        return value;
    }

    int getNumberofArraysinEE( ExpressionExperiment ee, ArrayDesign ad ) {
        int numberofArrays = 0;
        eeService.thaw( ee );
        Collection<BioAssay> bioAssays = ee.getBioAssays();
        for ( BioAssay assay : bioAssays ) {
            ArrayDesign design = assay.getArrayDesignUsed();
            if ( ad.equals( design ) ) {
                numberofArrays++;
            }
        }
        System.err.println( "Got " + numberofArrays );
        return numberofArrays;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "AffYPlatFormAnalysisCli ", args );
        if ( err != null ) {
            return err;
        }
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        this.eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        this.devService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );

        ArrayDesign arrayDesign = adService.findByShortName( this.arrayDesignName );
        if ( arrayDesign == null ) {
            System.err.println( " Array Design " + this.arrayDesignName + " doesn't exist" );
            return null;
        }

        // adService.thaw(arrayDesign);
        Collection<CompositeSequence> allCSs = adService.loadCompositeSequences( arrayDesign );
        for ( CompositeSequence cs : allCSs ) {
            this.rankData.put( cs, new DoubleArrayList() );
            this.presentAbsentData.put( cs, new DoubleArrayList() );
        }

        Collection<ExpressionExperiment> relatedEEs = adService.getExpressionExperiments( arrayDesign );

        int numberofAllArrays = 0;
        for ( ExpressionExperiment ee : relatedEEs ) {
            System.err.println( ee.getName() );
            if ( this.processEEForPercentage( ee ) != null ) continue;

            if ( this.processEE( ee ) != null ) continue;
            numberofAllArrays = numberofAllArrays + this.getNumberofArraysinEE( ee, arrayDesign );
        }
        log.info( "The total number of all arrays is " + numberofAllArrays );
        ObjectArrayList sortedList = new ObjectArrayList();
        for ( DesignElement de : this.rankData.keySet() ) {
            DoubleArrayList rankList = this.rankData.get( de );
            DoubleArrayList presentAbsentList = this.presentAbsentData.get( de );
            if ( rankList.size() > 0 ) {
                SortedElement oneElement = new SortedElement( de, getStatValue( rankList, MIN ), getStatValue(
                        rankList, MAX ), getStatValue( rankList, MEAN ), getStatValue( rankList, MEDIAN ),
                        getStatValue( rankList, STD ), getStatValue( presentAbsentList, MEAN ) );
                sortedList.add( oneElement );
            } else {
                System.err.print( de.getName() );
                System.err.println( " Empty " );
            }
        }
        sortedList.sort();
        try {
            PrintStream output = new PrintStream( new FileOutputStream( new File( this.outFileName ) ) );
            for ( int i = 0; i < sortedList.size(); i++ ) {
                SortedElement oneElement = ( SortedElement ) sortedList.get( i );
                if ( oneElement.getDE().getName().toUpperCase().contains( "AFFY" ) ) continue;
                output.print( oneElement.getDE().getName() );
                output.print( "\t" + oneElement.getMax() );
                double lower_threshould = 0.001;
                if ( oneElement.getPresentAbsentCall() < lower_threshould )
                    output.print( "\t" + lower_threshould );
                else
                    output.print( "\t" + oneElement.getPresentAbsentCall() );
                Set<Gene> mappedGenes = this.probeToGeneAssociation.get( oneElement.getDE() );
                if ( mappedGenes != null )
                    output.println( "\t" + mappedGenes.size() );
                else
                    output.println( "\t0" );
            }
            output.close();
        } catch ( Exception e ) {
            return e;
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        AffyPlatFormAnalysisCli analysis = new AffyPlatFormAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysis.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}

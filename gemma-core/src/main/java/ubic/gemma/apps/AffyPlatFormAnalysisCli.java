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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author unknown
 * @version $Id$
 */
public class AffyPlatFormAnalysisCli extends AbstractSpringAwareCLI {

    private class SortedElement implements Comparable<SortedElement> {
        private Double mean, min, max, median, std, presentAbsentCall;
        private DesignElement de;

        public SortedElement( DesignElement de, double min, double max, double mean, double median, double std, double presentAbsentCall ) {
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
        
        public Double getPresentAbsentCall(){
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
    private HashMap<DesignElement, DoubleArrayList> rankData = new HashMap<DesignElement, DoubleArrayList>();
    private HashMap<DesignElement, DoubleArrayList> presentAbsentData = new HashMap<DesignElement, DoubleArrayList>();
    private DesignElementDataVectorService devService = null;
    private ExpressionExperimentService eeService = null;

    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'a' ) ) {
            this.arrayDesignName = getOptionValue( 'a' );
        }
        if ( hasOption( 'o' ) ) {
            this.outFileName = getOptionValue( 'o' );
        }
    }

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
        Option ADOption = OptionBuilder.hasArg().isRequired().withArgName( "arrayDesign" ).withDescription(
                "Array Design Short Name (GPLXXX) " ).withLongOpt( "arrayDesign" ).create( 'a' );
        addOption( ADOption );
        Option OutOption = OptionBuilder.hasArg().isRequired().withArgName( "outputFile" ).withDescription(
                "The name of the file to save the output " ).withLongOpt( "outputFile" ).create( 'o' );
        addOption( OutOption );
    }

    private QuantitationType getQuantitationType( ExpressionExperiment ee, boolean PRESENTABSENT ) {
        QuantitationType qtf = null;
        Collection<QuantitationType> eeQT = this.eeService.getQuantitationTypes( ee );
        if(PRESENTABSENT){
        	for ( QuantitationType qt : eeQT ) 
        		if(qt.getType() == StandardQuantitationType.PRESENTABSENT){
        			qtf = qt;
        			break;
        		}
        	if(qtf == null){
        		log.info( "Expression Experiment " + ee.getShortName() + " doesn't have a presentabsent call" );
        	}
        	return qtf;
        }
        for ( QuantitationType qt : eeQT ) {
            if ( qt.getIsPreferred() ) {
                qtf = qt;
                StandardQuantitationType tmpQT = qt.getType();
                if ( tmpQT != StandardQuantitationType.DERIVEDSIGNAL && tmpQT != StandardQuantitationType.RATIO ) {
                    log.info( "Preferred Quantitation Type may not be correct." + ee.getShortName() + ":"
                            + tmpQT.toString() );
                }
                break;
            }
        }
        if ( qtf == null ) {
            log.info( "Expression Experiment " + ee.getShortName() + " doesn't have a preferred quantitation type" );
        }
        return qtf;
    }
    String processEEForPercentage(ExpressionExperiment ee){
        //eeService.thaw( ee );
        QuantitationType qt = this.getQuantitationType( ee, true);
        if ( qt == null ) return ( "No usable quantitation type in " + ee.getShortName() );
        log.info( "Load Data for  " + ee.getShortName() );

        Collection<DesignElementDataVector> dataVectors = devService.find( ee, qt );
        if ( dataVectors == null ) return ( "No data vector " + ee.getShortName() );
        ByteArrayConverter bac = new ByteArrayConverter();
        
        for(DesignElementDataVector vector:dataVectors){
        	DesignElement de = vector.getDesignElement();
        	DoubleArrayList presentAbsentList = this.presentAbsentData.get(de);
        	if(presentAbsentList == null){
        		//return (" EE data vectors don't match array design for probe " + de.getName());
        		continue;
        	}
            byte[] bytes = vector.getData();
            //String vals = bac.byteArrayToAsciiString( bytes );
            char [] chars = bac.byteArrayToChars(bytes);
            double presents = 0;
            double total = 0;
            for(int i = 0; i < chars.length; i++){
            	if(chars[i] == 'P') presents++;
            	if(chars[i] != '\t') total++;
            }
            presentAbsentList.add(presents/total);
        }
        return null;
	}
    @SuppressWarnings("unchecked")
    String processEE( ExpressionExperiment ee ) {
        //eeService.thaw( ee );
        QuantitationType qt = this.getQuantitationType( ee,false );
        if ( qt == null ) return ( "No usable quantitation type in " + ee.getShortName() );
        log.info( "Load Data for  " + ee.getShortName() );

        Collection<DesignElementDataVector> dataVectors = devService.find( ee, qt );
        if ( dataVectors == null ) return ( "No data vector " + ee.getShortName() );

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
                value = Descriptive.min( valList );
                break;
            case MAX:
                value = Descriptive.max( valList );
                break;
            case MEAN:
                value = Descriptive.mean( valList );
                break;
            case MEDIAN:
                value = Descriptive.median( valList );
                break;
            case STD:
                int N = valList.size();
                double sum = Descriptive.sum( valList );
                double ss = Descriptive.sumOfSquares( valList );
                value = Descriptive.standardDeviation( Descriptive.variance( N, sum, ss ) );
                break;
        }
        if ( Double.isNaN( value ) ) value = 0.0;
        return value;
    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        Exception err = processCommandLine( "ReOrderRankMatrix ", args );
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
            this.rankData.put( ( DesignElement ) cs, new DoubleArrayList() );
            this.presentAbsentData.put( ( DesignElement ) cs, new DoubleArrayList() );
        }

        Collection<ExpressionExperiment> relatedEEs = adService.getExpressionExperiments( arrayDesign );

        for ( ExpressionExperiment ee : relatedEEs ) {
        	System.err.println(ee.getName());
        		this.processEEForPercentage(ee);
        		this.processEE(ee);
        }
        ObjectArrayList sortedList = new ObjectArrayList();
        for ( DesignElement de : this.rankData.keySet() ) {
            DoubleArrayList rankList = this.rankData.get( de );
            DoubleArrayList presentAbsentList = this.presentAbsentData.get( de );
            if ( rankList.size() > 0 ) {
                SortedElement oneElement = new SortedElement( de, getStatValue( rankList, MIN ), getStatValue(
                        rankList, MAX ), getStatValue( rankList, MEAN ), getStatValue( rankList, MEDIAN ), getStatValue(
                        rankList, STD ), getStatValue( presentAbsentList, MAX) );
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
                output.print( oneElement.getDE().getName() );
                output.print( "\t" + oneElement.getMax() );
                output.println( "\t" + oneElement.getPresentAbsentCall() );
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
        // TODO Auto-generated method stub
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

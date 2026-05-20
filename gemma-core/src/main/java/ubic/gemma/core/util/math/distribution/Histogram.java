/*
 * The baseCode project
 * 
 * Copyright (c) 2008-2019-2010 University of British Columbia
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
package ubic.gemma.core.util.math.distribution;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.jfree.data.xy.XYSeries;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.jet.stat.Descriptive;

/**
 * A simple histogram.
 * 
 * @author keshav
 * 
 */
public class Histogram {
    /**
     *
     */
    private static class BinInfo {
        public int index;
        public boolean isInRange;
        public boolean isOverflow;
        public boolean isUnderflow;
    }

    // total number of entries
    private int entries;

    private double[] hist;

    private double max;

    private double min;

    private String name;

    private int nbins;

    // number of values above the maximum bin.
    private double overflow = 0;

    // number of values below the minimum bin
    private double underflow = 0;

    /**
     * @param name
     * @param nbins
     * @param min
     * @param max
     */
    public Histogram( String name, int nbins, double min, double max ) {
        init( name, nbins );

        this.min = min;
        this.max = max;

    }

    /**
     * @param name
     * @param nbins
     * @param data
     */
    public Histogram( String name, int nbins, DoubleMatrix1D data ) {
        init( name, nbins );

        this.min = Descriptive.min( new DoubleArrayList( data.toArray() ) );
        this.max = Descriptive.max( new DoubleArrayList( data.toArray() ) );

        this.fill( data );

    }

    /**
     * The number of entries in the histogram (the number of times fill has been called).
     * 
     * @return number of entries
     */
    public int entries() {
        return entries;
    }

    /**
     * Fill the histogram with x.
     * 
     * @param x is the value to add in to the histogram
     */
    public void fill( double x ) {
        // use findBin method to work out which bin x falls in
        BinInfo bin = findBin( x );
        // check the result of findBin in case it was an overflow or underflow
        if ( bin.isUnderflow ) {
            underflow++;
        }
        if ( bin.isOverflow ) {
            overflow++;
        }
        if ( bin.isInRange ) {
            hist[bin.index]++;
        }

        // count the number of entries made by the fill method
        entries++;
    }

    /**
     * @param ghr
     */
    public void fill( DoubleMatrix1D ghr ) {
        for ( int i = 0; i < ghr.size(); i++ ) {
            this.fill( ghr.getQuick( i ) );
        }

    }

    /**
     * Add the given number of counts to the given bin.
     * 
     * @param binNum
     * @param count
     */
    public void fill( int binNum, int count ) {
        if ( binNum < 0 || binNum > nbins - 1 ) {
            throw new IllegalArgumentException( "Invalid bin number" );
        }
        if ( count < 0 ) {
            throw new IllegalArgumentException( "Count must be positive" );
        }
        hist[binNum] += count;
    }

    /**
     * Find the bin below which i% of the values are contained. This is only approximate (especially if the number of
     * bins is <~100)
     * 
     * @param q
     * @return approximate quantile. Passing values of 0 and 100 is equivalent to min() and max() respectively.
     */
    public Double getApproximateQuantile( int q ) {
        double t = underflow;
        if ( q >= 100 ) {
            return max;
        }
        if ( q <= 0 ) {
            return min;
        }

        for ( int i = 0; i < hist.length; i++ ) {
            t += hist[i];
            if ( t / entries > q / 100.0 ) {
                return getBinEdges()[i];
            }
        }

        return max;
    }

    /**
     * Returns the bin heights.
     * 
     * @return array of bin heights
     */
    public double[] getArray() {
        return hist;
    }

    /**
     * @return the number of items in the bin that has the most members.
     */
    public int getBiggestBinSize() {
        int m = Integer.MIN_VALUE;
        for ( double d : hist ) {
            if ( d > m ) m = ( int ) d;
        }
        return m;
    }

    /**
     * @return
     */
    public Double[] getBinEdges() {

        double step = this.min();
        double[] binHeights = this.getArray();

        Double[] result = new Double[binHeights.length];

        for ( int i = 0; i < binHeights.length; i++ ) {
            result[i] = step;
            step += this.stepSize();
        }

        return result;
    }

    public String[] getBinEdgesStrings() {

        double step = this.min();
        double[] binHeights = this.getArray();

        String[] result = new String[binHeights.length];

        for ( int i = 0; i < binHeights.length; i++ ) {
            result[i] = String.format( "%.3f", step );
            step += this.stepSize();
        }

        return result;
    }

    /**
     * @param x
     * @return the index of the bin where x would fall. If x is out of range you get 0 or (nbins - 1).
     */
    public int getBinOf( double x ) {
        if ( x < min ) {
            return 0;
        } else if ( x > max ) {
            return nbins - 1;
        } else {
            // search for histogram bin into which x falls FIXME make this faster with a binary search
            double binWidth = stepSize();
            for ( int i = 0; i < nbins; i++ ) {
                double highEdge = min + ( i + 1 ) * binWidth;
                if ( x <= highEdge ) {
                    return i;
                }
            }
            throw new IllegalStateException( "Failed to find the bin for " + x );
        }
    }

    /**
     * The name of the histogram.
     * 
     * @return histogram name
     */
    public String getName() {
        return name;
    }

    /**
     * @return maximum x value covered by histogram
     */
    public double max() {
        return max;
    }

    /**
     * @return minimum x value covered by histogram
     */
    public double min() {
        return min;
    }

    /**
     * Get the number of bins in the histogram. The range of the histogram defined by min and max, and the range is
     * divided into the number of returned.
     * 
     * @return number of bins
     */
    public int numberOfBins() {
        return nbins;
    }

    /**
     * The height of the overflow bin.
     * 
     * @return number of overflows
     */
    public double overflow() {
        return overflow;
    }

    /**
     * Provide graph for JFreePlot; counts expressed as a fraction.
     */
    public XYSeries plot() {
        XYSeries series = new XYSeries( this.name, true, true );

        double step = this.min();

        double binWidth = stepSize();

        double[] binHeights = this.getArray();
        for ( int i = 0; i < binHeights.length; i++ ) {
            series.add( step, binHeights[i] / entries );
            step += binWidth;
        }
        return series;
    }

    /**
     * @return size of each bin
     */
    public double stepSize() {
        return ( max - min ) / nbins;
    }

    /**
     * The height of the underflow bin.
     * 
     * @return number of underflows
     */
    public double underflow() {
        return underflow;
    }

    /**
     * Write the histogram to a file. The format is:
     * <p>
     * bin (lower edge), number in bin.
     * <p>
     * 
     * @param out
     * @throws IOException
     */
    public void writeToFile( FileWriter out ) throws IOException {

        DecimalFormat dfBin = new DecimalFormat( " ##0.00;-##0.00" );
        DecimalFormat dfCount = new DecimalFormat( " ##0" );

        double step = this.min();

        double binWidth = stepSize();

        double[] binHeights = this.getArray();

        for ( int i = 0; i < binHeights.length; i++ ) {

            String line = dfBin.format( step ) + "\t" + dfCount.format( binHeights[i] );
            if ( i < binHeights.length - 1 ) line += "\n";

            out.write( line );

            step += binWidth;

        }

        out.close();
    }

    /**
     * Determines the bin for a number in the histogram.
     * 
     * @return info on which bin x falls in.
     */
    private BinInfo findBin( double x ) {
        BinInfo bin = new BinInfo();
        bin.isInRange = false;
        bin.isUnderflow = false;
        bin.isOverflow = false;
        // first check if x is outside the range of the normal histogram bins
        if ( x < min ) {
            bin.isUnderflow = true;
        } else if ( x > max ) {
            bin.isOverflow = true;
        } else {
            // search for histogram bin into which x falls FIXME make this faster with a binary search
            double binWidth = stepSize();
            for ( int i = 0; i < nbins; i++ ) {
                double highEdge = min + ( i + 1 ) * binWidth;
                if ( x <= highEdge ) {
                    bin.isInRange = true;
                    bin.index = i;
                    break;
                }
            }
        }
        return bin;

    }

    /**
     * @param name
     * @param nbins
     */
    private void init( String n, int nb ) {
        this.name = n;
        this.nbins = nb;
        this.underflow = 0;
        this.overflow = 0;
        this.hist = new double[this.nbins];
        for ( int i = 0; i < hist.length; i++ ) {
            hist[i] = 0;
        }
    }
}

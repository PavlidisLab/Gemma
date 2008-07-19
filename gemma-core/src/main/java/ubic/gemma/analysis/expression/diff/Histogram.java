/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.expression.diff;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * A simple histogram.
 * 
 * @author keshav
 * @version $Id$
 */
public class Histogram {
    // TODO this is a candidate for basecode, but leaving it here for the moment.

    /**
     * @param name
     * @param nbins
     * @param min
     * @param max
     */
    public Histogram( String name, int nbins, double min, double max ) {
        m_nbins = nbins;
        m_min = min;
        m_max = max;
        m_name = name;
        m_hist = new double[m_nbins];
        m_underflow = 0;
        m_overflow = 0;
    }

    /**
     * Fill the histogram by taking the x and putting it into the appropriate bin.
     * 
     * @param x is the value to add in to the histogram
     */
    public void fill( double x ) {
        // use findBin method to work out which bin x falls in
        BinInfo bin = findBin( x );
        // check the result of findBin in case it was an overflow or underflow
        if ( bin.isUnderflow ) {
            m_underflow++;
        }
        if ( bin.isOverflow ) {
            m_overflow++;
        }
        if ( bin.isInRange ) {
            m_hist[bin.index]++;
        }

        // count the number of entries made by the fill method
        m_entries++;
    }

    /**
     * Private class used internally to store info about which bin of the histogram to use for a number to be filled.
     */
    private class BinInfo {
        public int index;
        public boolean isUnderflow;
        public boolean isOverflow;
        public boolean isInRange;
    }

    /**
     * Private internal utility method to figure out which bin of the histogram a number falls in.
     * 
     * @return info on which bin x falls in.
     */
    private BinInfo findBin( double x ) {
        BinInfo bin = new BinInfo();
        bin.isInRange = false;
        bin.isUnderflow = false;
        bin.isOverflow = false;
        // first check if x is outside the range of the normal histogram bins
        if ( x < m_min ) {
            bin.isUnderflow = true;
        } else if ( x > m_max ) {
            bin.isOverflow = true;
        } else {
            // search for histogram bin into which x falls
            double binWidth = ( m_max - m_min ) / m_nbins;
            for ( int i = 0; i < m_nbins; i++ ) {
                double highEdge = m_min + ( i + 1 ) * binWidth;
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
     * Write the histogram to a file. The format is:
     * <p>
     * bin (lower edge), number in bin.
     * <p>
     * 
     * @param out
     * @throws IOException
     */
    public void writeToFile( FileWriter out ) throws IOException {

        DecimalFormat df = new DecimalFormat( " ##0.00;-##0.00" );

        double step = 0;

        double binWidth = ( m_max - m_min ) / m_nbins;

        double[] binHeights = this.getArray();

        for ( int i = 0; i < binHeights.length; i++ ) {

            String line = df.format( step ) + "\t" + df.format( binHeights[i] );
            if ( i < binHeights.length - 1 ) line += "\n";

            out.write( line );

            step += binWidth;

        }

        out.close();
    }

    /**
     * Get number of entries in the histogram. This should correspond to the number of times the fill method has been
     * used.
     * 
     * @return number of entries
     */
    public int entries() {
        return m_entries;
    }

    /**
     * Get the name of the histogram. The name is an arbitrary label for the user, and is set by the constructor.
     * 
     * @return histogram name
     */
    public String name() {
        return m_name;
    }

    /**
     * Get the number of bins in the histogram. The range of the histogram defined by min and max is divided into this
     * many bins.
     * 
     * @return number of bins
     */
    public int numberOfBins() {
        return m_nbins;
    }

    /**
     * Get lower end of histogram range
     * 
     * @return minimum x value covered by histogram
     */
    public double min() {
        return m_min;
    }

    /**
     * Get upper end of histogram range
     * 
     * @return maximum x value covered by histogram
     */
    public double max() {
        return m_max;
    }

    /**
     * Get the height of the overflow bin. Any value passed to the fill method which falls above the range of the
     * histogram will be counted in the overflow bin.
     * 
     * @return number of overflows
     */
    public double overflow() {
        return m_overflow;
    }

    /**
     * Get the height of the underflow bin. Any value passed to the fill method which falls below the range of the
     * histogram will be counted in the underflow bin.
     * 
     * @return number of underflows
     */
    public double underflow() {
        return m_underflow;
    }

    /**
     * This method gives you the bin contents in the form of an array. It might be useful for example if you want to use
     * the histogram in some other way, for example to pass to a plotting package.
     * 
     * @return array of bin heights
     */
    public double[] getArray() {
        return m_hist;
    }

    // private data used internally by this class.
    private double[] m_hist;
    private String m_name;
    private double m_min;
    private double m_max;
    private int m_nbins;
    private int m_entries;
    private double m_overflow;
    private double m_underflow;
}

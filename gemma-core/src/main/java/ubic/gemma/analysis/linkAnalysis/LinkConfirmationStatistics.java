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
package ubic.gemma.analysis.linkAnalysis;

import com.ibm.icu.text.NumberFormat;

/**
 * Holds information about the level of support for links.
 * 
 * @author paul
 * @version $Id$
 */
public class LinkConfirmationStatistics {

    public final static int LINK_MAXIMUM_COUNT = 100;

    private int[] posLinkStats = new int[LINK_MAXIMUM_COUNT];
    private int[] negLinkStats = new int[LINK_MAXIMUM_COUNT];

    private int[] cuPosLinkStats = new int[LINK_MAXIMUM_COUNT];
    private int[] cuNegLinkStats = new int[LINK_MAXIMUM_COUNT];

    public LinkConfirmationStatistics() {
        for ( int i = 0; i < LINK_MAXIMUM_COUNT; i++ ) {
            posLinkStats[i] = 0;
            negLinkStats[i] = 0;
            cuPosLinkStats[i] = 0;
            cuNegLinkStats[i] = 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 3 );

        return buf.toString();
    }

    /**
     * Determine the largest number of data sets any link was seen in
     * 
     * @return
     */
    public int getMaxLinkSupport() {

        int maxSupport = 0;
        for ( int j = LinkConfirmationStatistics.LINK_MAXIMUM_COUNT - 1; j >= 0; j-- ) {
            if ( this.getRepCount( j ) != 0 ) {
                maxSupport = j;
                break;
            }
        }
        return maxSupport;
    }

    /**
     * @param replicates Number of data sets the link was seen in.
     */
    public void addPos( int replicates ) {
        posLinkStats[replicates]++;
    }

    /**
     * @param replicates Number of data sets the link was seen in.
     */
    public void addNeg( int replicates ) {
        negLinkStats[replicates]++;
    }

    /**
     * @param r
     * @return how many positive correlation links are replicated in exactly r data sets
     */
    public int getNegRepCount( int r ) {
        return negLinkStats[r];
    }

    /**
     * @param r
     * @return how many negative correlation links are replicated in exactly r data sets
     */
    public int getPosRepCount( int r ) {
        return posLinkStats[r];
    }

    /**
     * @param r
     * @return how many correlation links are replicated in exactly r data sets (including both positive and negative
     *         correlations).
     */
    public int getRepCount( int r ) {
        return posLinkStats[r] + negLinkStats[r];
    }

    /**
     * @param r
     * @return how many positive correlation links are replicated in r or more data sets (including both positive and
     *         negative correlations).
     */
    public int getCumulativePosRepCount( int r ) {
        cuPosLinkStats[LINK_MAXIMUM_COUNT - 1] = posLinkStats[LINK_MAXIMUM_COUNT - 1];
        for ( int j = LINK_MAXIMUM_COUNT - 2; j >= 0; j-- ) {
            cuPosLinkStats[j] = posLinkStats[j] + cuPosLinkStats[j + 1];
        }

        return cuPosLinkStats[r];
    }

    /**
     * @param r
     * @return how many negative correlation links are replicated in r or more data sets.
     */
    public int getCumulativeNegRepCount( int r ) {
        cuNegLinkStats[LINK_MAXIMUM_COUNT - 1] = negLinkStats[LINK_MAXIMUM_COUNT - 1];
        for ( int j = LINK_MAXIMUM_COUNT - 2; j >= 0; j-- ) {
            cuNegLinkStats[j] = negLinkStats[j] + cuNegLinkStats[j + 1];
        }
        return cuNegLinkStats[r];
    }

    /**
     * @param r
     * @return how many correlation links are replicated in r or more data sets.
     */
    public int getCumulativeRepCount( int r ) {
        return getCumulativeNegRepCount( r ) + getCumulativePosRepCount( r );
    }
}

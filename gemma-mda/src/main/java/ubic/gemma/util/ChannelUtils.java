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
package ubic.gemma.util;

/**
 * Determine if a quantitation type (by name) represents background or signal. This includes a variety of cases, some of
 * them 'special'.
 * 
 * @author paul
 * @version $Id$
 */
public class ChannelUtils {

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel A background.
     * 
     * @param name
     * @return
     */
    public static boolean isBackgroundChannelA( String name ) {
        return name.matches( "CH1B(N)?_(MEDIAN|MEAN)" ) || name.equals( "CH1_BKD" )
                || name.toLowerCase().matches( "b532[\\s_\\.](mean|median)" )
                || name.toUpperCase().equals( "BACKGROUND_CHANNEL 1MEDIAN" )
                || name.toUpperCase().equals( "G_BG_MEDIAN" ) || name.equals( "Ch1BkgMedian" )
                || name.equals( "ch1.Background" ) || name.toUpperCase().equals( "CH1_BKG_MEAN" )
                || name.equals( "CH1_BKD_ Median" ) || name.equals( "BKG1Mean" );
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel B background.
     * 
     * @param name
     * @return
     */
    public static boolean isBackgroundChannelB( String name ) {
        return name.matches( "CH2B(N)?_(MEDIAN|MEAN)" ) || name.equals( "CH2_BKD" )
                || name.toLowerCase().matches( "b635[\\s_\\.](mean|median)" )
                || name.toUpperCase().equals( "BACKGROUND_CHANNEL 2MEDIAN" )
                || name.toUpperCase().equals( "R_BG_MEDIAN" ) || name.equals( "Ch2BkgMedian" )
                || name.equals( "ch2.Background" ) || name.toUpperCase().equals( "CH2_BKG_MEAN" )
                || name.equals( "CH2_BKD_ Median" ) || name.equals( "BKG2Mean" );
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel A signal. (by
     * convention, green)
     * 
     * @param name
     * @return
     */
    public static boolean isSignalChannelA( String name ) {
        return name.matches( "CH1(I)?(N)?_MEDIAN" ) || name.matches( "CH1(I)?(N)?_MEAN" )
                || name.toUpperCase().equals( "RAW_DATA" ) || name.toLowerCase().matches( "f532[\\s_\\.](mean|median)" )
                || name.toUpperCase().equals( "SIGNAL_CHANNEL 1MEDIAN" ) || name.toLowerCase().matches( "ch1_smtm" )
                || name.equals( "G_MEAN" ) || name.equals( "Ch1SigMedian" ) || name.equals( "ch1.Intensity" )
                || name.toUpperCase().matches( "CH1_SIG(NAL)?_(MEAN|MEDIAN)" ) || name.equals( "CH1_ Median" )
                || name.toUpperCase().matches( "\\w{2}\\d{3}_CY3" ) || name.toUpperCase().matches( "CY3.*" )
                || name.toUpperCase().matches( "NORM(.*)CH1" ) || name.equals( "CH1Mean" )
                || name.toUpperCase().equals( "CH1_SIGNAL" ) || name.equals( "\"log2(532), gN\"" )
                || name.equals( "gProcessedSignal" ) || name.toUpperCase().equals( "CH1_SIG_MEDIAN" )
                || name.toUpperCase().equals( "INTENSITY1" );
    }

    /**
     * For two-color arrays: Given the quantitation type name, determine if it represents the channel B signal. (by
     * convention, red)
     * 
     * @param name
     * @return
     */
    public static boolean isSignalChannelB( String name ) {
        return name.matches( "CH2(I)?(N)?_MEDIAN" ) || name.matches( "CH2(I)?(N)?_MEAN" )
                || name.toUpperCase().equals( "RAW_CONTROL" )
                || name.toLowerCase().matches( "f635[\\s_\\.](mean|median)" )
                || name.toUpperCase().equals( "SIGNAL_CHANNEL 2MEDIAN" ) || name.toLowerCase().matches( "ch2_smtm" )
                || name.equals( "R_MEAN" ) || name.equals( "Ch2SigMedian" ) || name.equals( "ch2.Intensity" )
                || name.toUpperCase().matches( "CH2_SIG(NAL)?_(MEAN|MEDIAN)" ) || name.equals( "CH2_ Median" )
                || name.toUpperCase().matches( "\\w{2}\\d{3}_CY5" ) || name.toUpperCase().matches( "CY5.*" )
                || name.toUpperCase().matches( "NORM(.*)CH2" ) || name.equals( "CH2Mean" )
                || name.toUpperCase().equals( "CH2_SIGNAL" ) || name.equals( "\"log2(635), gN\"" )
                || name.equals( "rProcessedSignal" ) || name.toUpperCase().equals( "INTENSITY2" );
    }

}

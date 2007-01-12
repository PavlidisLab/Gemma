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
package ubic.gemma.loader.expression.geo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class to store the expression data prior to conversion. The data are read from series files sample by sample, and
 * within each sample designElement by designElement, and within each designElement, quantitationType by
 * quantitationType. Values are stored in vectors, roughly equivalent to DesignElementDataVectors.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoValues {

    private static Log log = LogFactory.getLog( GeoValues.class.getName() );

    /*
     * This plays the role of the BioAssayDimension; map of platform --> quantitationType --> samples
     */
    Map<GeoPlatform, Map<String, LinkedHashSet<GeoSample>>> sampleDimensions = new HashMap<GeoPlatform, Map<String, LinkedHashSet<GeoSample>>>();

    /*
     * Map of platform --> quantitationtype -> designElement -> values; values in same order as sampleVector.
     */
    Map<GeoPlatform, Map<String, Map<String, List<Object>>>> data = new HashMap<GeoPlatform, Map<String, Map<String, List<Object>>>>();

    private static Collection<String> skippableQuantitationTypes = new HashSet<String>();

    static {

        // These are from GenePix files. In Stanford files they are named differently than described here:
        // http://www.moleculardevices.com/pages/software/gn_genepix_file_formats.html

        // these are location and spot size information.
        skippableQuantitationTypes.add( "X_COORD" );
        skippableQuantitationTypes.add( "Y_COORD" );
        skippableQuantitationTypes.add( "TOP" );
        skippableQuantitationTypes.add( "BOT" );
        skippableQuantitationTypes.add( "LEFT" );
        skippableQuantitationTypes.add( "RIGHT" );
        skippableQuantitationTypes.add( "DIAMETER" );
        skippableQuantitationTypes.add( "TOT_SPIX" );
        skippableQuantitationTypes.add( "TOT_BPIX" );

        // the following are background-subtracted values that can be easily computed from the raw values
        skippableQuantitationTypes.add( "CH1D_MEAN" );
        skippableQuantitationTypes.add( "CH2D_MEAN" );
        skippableQuantitationTypes.add( "CH1D_MEDIAN" );
        skippableQuantitationTypes.add( "CH2D_MEDIAN" );

        // some raw items are skippable.(assumes we use median)
        skippableQuantitationTypes.add( "SUM_MEAN" );
        skippableQuantitationTypes.add( "RAT1_MEAN" );
        skippableQuantitationTypes.add( "RAT2_MEAN" );
        skippableQuantitationTypes.add( "PIX_RAT2_MEAN" );
        skippableQuantitationTypes.add( "PIX_RAT2_MEDIAN" );

        // otherwise deemed skippable.
        skippableQuantitationTypes.add( "PERGTBCH1I_1SD" );
        skippableQuantitationTypes.add( "PERGTBCH2I_1SD" );
        skippableQuantitationTypes.add( "PERGTBCH1I_2SD" );
        skippableQuantitationTypes.add( "PERGTBCH2I_2SD" );

        // these removed just in the interest of saving memory!
        skippableQuantitationTypes.add( "SUM_MEAN" );
        skippableQuantitationTypes.add( "SUM_MEDIAN" );
        skippableQuantitationTypes.add( "REGR" );
        skippableQuantitationTypes.add( "CORR" );
        skippableQuantitationTypes.add( "UNF_VALUE" ); // this is the same as 'value' but with the flagged points still
        // in.

    }

    /**
     * Store a value. It is assumed that quantitationTypes and designElements have unique names.
     * 
     * @param sample
     * @param quantitationType
     * @param designElement
     * @param value
     */
    public void addValue( GeoSample sample, String quantitationType, String designElement, Object value ) {

        if ( skippableQuantitationTypes.contains( quantitationType ) ) return;

        GeoPlatform platform = sample.getPlatforms().iterator().next();
        if ( !sampleDimensions.containsKey( platform ) ) {
            sampleDimensions.put( platform, new HashMap<String, LinkedHashSet<GeoSample>>() );
        }

        Map<String, LinkedHashSet<GeoSample>> samplePlatformMap = sampleDimensions.get( platform );
        if ( !samplePlatformMap.containsKey( quantitationType ) ) {
            samplePlatformMap.put( quantitationType, new LinkedHashSet<GeoSample>() );
        }

        LinkedHashSet<GeoSample> sampleQtMap = samplePlatformMap.get( quantitationType );
        if ( !sampleQtMap.contains( sample ) ) {
            sampleQtMap.add( sample );
        }

        if ( !data.containsKey( platform ) ) {
            data.put( platform, new HashMap<String, Map<String, List<Object>>>() );
        }

        Map<String, Map<String, List<Object>>> platformMap = data.get( platform );
        if ( !platformMap.containsKey( quantitationType ) ) {
            platformMap.put( quantitationType, new HashMap<String, List<Object>>() );
        }

        Map<String, List<Object>> qtMap = platformMap.get( quantitationType );
        if ( !qtMap.containsKey( designElement ) ) {
            qtMap.put( designElement, new ArrayList<Object>() );
        }

        qtMap.get( designElement ).add( value );
        if ( log.isTraceEnabled() ) {
            log.trace( "Adding value for " + sample + " qt=" + quantitationType + " de=" + designElement + " value="
                    + value );
        }

        assert qtMap.get( designElement ).size() == sampleQtMap.size() : "Number of  samples " + sampleQtMap.size()
                + " does not equal length of vector " + data.get( quantitationType ).get( designElement ).size();

    }

    /**
     * @param quantitationType
     * @param designElement
     * @return
     */
    public List<Object> getValues( GeoPlatform platform, String quantitationType, String designElement ) {
        return data.get( platform ).get( quantitationType ).get( designElement );
    }

    /**
     * @return
     */
    public GeoSample[] getSampleDimension( GeoPlatform platform, String quantitationType ) {
        return ( GeoSample[] ) sampleDimensions.get( platform ).get( quantitationType ).toArray();
    }

    /**
     * Get the indices of the data for a set of samples - this can be used to get a slice of the data. This is
     * inefficient but shouldn't need to be called very often.
     * 
     * @param platform
     * @param neededSamples, must be from the same platform.
     * @return
     */
    public int[] getIndices( GeoPlatform platform, List<GeoSample> neededSamples, String quantitationType ) {
        assert sampleDimensions.get( platform ) != null;
        if ( sampleDimensions.get( platform ).get( quantitationType ) == null ) {
            return null; // filtered out?
        }
        assert neededSamples.size() <= sampleDimensions.get( platform ).get( quantitationType ).size() : "Requested data for "
                + neededSamples.size()
                + " samples but only know about "
                + sampleDimensions.get( quantitationType ).size();

        List<Integer> result = new ArrayList<Integer>();
        for ( GeoSample sample : neededSamples ) {
            int i = 0;
            for ( GeoSample sampleInVector : sampleDimensions.get( platform ).get( quantitationType ) ) {
                if ( sample.equals( sampleInVector ) ) {
                    result.add( i );
                }
                i++;
            }

        }

        // convert to an array.
        int[] resultAr = new int[result.size()];
        int j = 0;
        for ( Integer i : result ) {
            resultAr[j] = i;
            j++;
        }
        return resultAr;

    }

    /**
     * Return a 'slice' of the data corresponding to the indices provided.
     * 
     * @param quantitationType
     * @param designElement
     * @param indices
     * @return
     */
    public List<Object> getValues( GeoPlatform platform, String quantitationType, String designElement, int[] indices ) {
        List<Object> result = new ArrayList<Object>();
        List<Object> rawvals = data.get( platform ).get( quantitationType ).get( designElement );

        // this can happen if the data doesn't contain that designElement.
        if ( rawvals == null ) return null;
        for ( int i : indices ) {
            assert rawvals.get( i ) != null : "No entry for index " + i;
            result.add( rawvals.get( i ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for ( GeoPlatform platform : sampleDimensions.keySet() ) {

            buf.append( "============== " + platform + " =================\n" );
            for ( String qType : sampleDimensions.get( platform ).keySet() ) {
                buf.append( "---------------- " + qType + " ------------------\n" );
                buf.append( "DesignEl" );

                for ( GeoSample sam : sampleDimensions.get( platform ).get( qType ) ) {
                    buf.append( "\t" + sam.getGeoAccession() );
                }
                buf.append( "\n" );
                for ( String dEl : data.get( platform ).get( qType ).keySet() ) {
                    buf.append( dEl );
                    for ( Object val : data.get( platform ).get( qType ).get( dEl ) ) {
                        buf.append( "\t" + val );
                    }
                    buf.append( "\n" );
                }
            }
        }

        return buf.toString();
    }

    /**
     * Remove the data for a given platform (use to save memory)
     * 
     * @param geoPlatform
     */
    public void clear( GeoPlatform geoPlatform ) {
        this.data.remove( geoPlatform );
    }

}

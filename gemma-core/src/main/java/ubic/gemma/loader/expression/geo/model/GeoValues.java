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
 * <p>
 * This is an important class as it encompasses how we convert GEO sample data into vectors. There are a couple of
 * assumption that this is predicated on. First, we assume that all samples are presented with their quantitation types
 * in the same order. Second, we assume that all samples have the same quantitation type, OR at worst, some are missing
 * off the 'end' for some samples. We do not assume that all samples have quantitation types with the same names
 * (quantitation types correspond to column names in the GEO files).
 * <p>
 * There are two counterexamples we have found that push or violate these assumptions: GSE360 and GSE4345 (which is
 * really broken). Loading GSE4345 results in a cast exception because the quantitation types are 'mixed up' across the
 * samples..
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoValues {

    private static Log log = LogFactory.getLog( GeoValues.class.getName() );

    /*
     * This plays the role of the BioAssayDimension; map of platform --> quantitationType --> samples
     */
    Map<GeoPlatform, Map<Object, LinkedHashSet<GeoSample>>> sampleDimensions = new HashMap<GeoPlatform, Map<Object, LinkedHashSet<GeoSample>>>();

    /*
     * Map of platform --> quantitationtype -> designElement -> values; values in same order as sampleVector.
     */
    Map<GeoPlatform, Map<Object, Map<String, List<Object>>>> data = new HashMap<GeoPlatform, Map<Object, Map<String, List<Object>>>>();

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
     * Store a value. It is assumed that designElements have unique names.
     * <p>
     * Implementation note: The way this works: the first time we see a sample, we associate it with a 'dimension' that
     * is connected to the platform and quantitation type. In parallel, we add the data to a 'vector' for the
     * designElement that is likewise connected to the platform the sample uses, the quantitation type. Because in GEO
     * files samples are seen one at a time, the vectors for each designelement are built up. Thus it is important that
     * we add a value for each sample for each design element.
     * <p>
     * Note what happens if data is MISSING for a given designElement/quantitationType/sample combination. This can
     * happen (typically all the quantitation types for a designelement in a given sample). This method will NOT be
     * called. When the next sample is processed, the new data will be added onto the end in the wrong place. Then the
     * data in the vectors stored here will be incorrect. Thus the GEO parser has to ensure that each vector is
     * 'completed' before moving to the next sample.
     * 
     * @param sample
     * @param quantitationTypeIndex The column number for the quantitation type, needed because the names of the
     *        quantitation types don't always match across samples (but hopefully the columns do). Even though the first
     *        column contains the design element name (ID_REF), the first quantitation type should be numbered 0. This
     *        is almost always a good way to match values across samples, there is a single (pathological?) case where
     *        the order isn't the same for two samples.
     * @param quantitationTypeIndex Identifies the quantitation type.
     * @param designElement
     * @param value The data point to be stored.
     */
    public void addValue( GeoSample sample, Integer quantitationTypeIndex, String designElement, Object value ) {

        if ( skippableQuantitationTypes.contains( quantitationTypeIndex ) ) return;

        if ( sample.getPlatforms().size() > 1 ) {
            throw new IllegalArgumentException( sample + ": Can't handle samples that use multiple platforms" );
        }

        GeoPlatform platform = sample.getPlatforms().iterator().next();
        if ( !sampleDimensions.containsKey( platform ) ) {
            sampleDimensions.put( platform, new HashMap<Object, LinkedHashSet<GeoSample>>() );
        }

        Map<Object, LinkedHashSet<GeoSample>> samplePlatformMap = sampleDimensions.get( platform );
        if ( !samplePlatformMap.containsKey( quantitationTypeIndex ) ) {
            samplePlatformMap.put( quantitationTypeIndex, new LinkedHashSet<GeoSample>() );
        }

        LinkedHashSet<GeoSample> sampleQtMap = samplePlatformMap.get( quantitationTypeIndex );
        if ( !sampleQtMap.contains( sample ) ) {
            sampleQtMap.add( sample );
        }

        if ( !data.containsKey( platform ) ) {
            data.put( platform, new HashMap<Object, Map<String, List<Object>>>() );
        }

        Map<Object, Map<String, List<Object>>> platformMap = data.get( platform );
        if ( !platformMap.containsKey( quantitationTypeIndex ) ) {
            platformMap.put( quantitationTypeIndex, new HashMap<String, List<Object>>() );
        }

        Map<String, List<Object>> qtMap = platformMap.get( quantitationTypeIndex );
        if ( !qtMap.containsKey( designElement ) ) {
            qtMap.put( designElement, new ArrayList<Object>() );
        }

        qtMap.get( designElement ).add( value );

        if ( log.isTraceEnabled() ) {
            log.trace( "Adding value for platform=" + platform + " sample=" + sample + " qt=" + quantitationTypeIndex
                    + " de=" + designElement + " value=" + value );
        }

        // assert qtMap.get( designElement ).size() == sampleQtMap.size() : "Duplicate quantitation type name in series?
        // "
        // + "While processing data for " + sample + ": Number of samples " + sampleQtMap.size()
        // + " for designElement=" + designElement + " quantType=" + quantitationTypeIndex
        // + " does not equal length of vector "
        // + data.get( platform ).get( quantitationTypeIndex ).get( designElement ).size();

    }

    /**
     * @param samplePlatform
     * @return Collection of Objects representing the quantitation types for the given platform.
     */
    public Collection<Object> getQuantitationTypes( GeoPlatform samplePlatform ) {
        return this.data.get( samplePlatform ).keySet();
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
     * inefficient but shouldn't need to be called all that frequently.
     * 
     * @param platform
     * @param neededSamples, must be from the same platform. If we don't have data for a given sample, the index
     *        returned will be null. This can happen when some samples don't have all the quantitation types (GSE360 for
     *        example).
     * @return
     */
    public Integer[] getIndices( GeoPlatform platform, List<GeoSample> neededSamples, Integer quantitationType ) {
        assert sampleDimensions.get( platform ) != null;
        if ( sampleDimensions.get( platform ).get( quantitationType ) == null ) {
            return null; // filtered out?
        }

        List<Integer> result = new ArrayList<Integer>();
        for ( GeoSample sample : neededSamples ) {
            int i = 0;
            boolean found = false;
            for ( GeoSample sampleInVector : sampleDimensions.get( platform ).get( quantitationType ) ) {
                if ( sample.equals( sampleInVector ) ) {
                    result.add( i );
                    found = true;
                }
                i++;
            }

            if ( !found ) result.add( null );

        }

        return result.toArray( new Integer[result.size()] );

    }

    /**
     * If possible, null out the data for a quantitation type on a given platform.
     * 
     * @param platform
     * @param datasetSamples
     * @param quantitationTypeIndex
     */
    public void clear( GeoPlatform platform, List<GeoSample> datasetSamples, Integer quantitationTypeIndex ) {
        if ( datasetSamples.size() != sampleDimensions.get( platform ).get( quantitationTypeIndex ).size() ) {
            return; // can't really clear
        }
        log.debug( "Clearing" );
        data.get( platform ).remove( quantitationTypeIndex );
    }

    /**
     * Return a 'slice' of the data corresponding to the indices provided.
     * 
     * @param quantitationType
     * @param designElement
     * @param indices
     * @return
     */
    public List<Object> getValues( GeoPlatform platform, Integer quantitationType, String designElement,
            Integer[] indices ) {
        List<Object> result = new ArrayList<Object>();
        List<Object> rawvals = data.get( platform ).get( quantitationType ).get( designElement );

        // this can happen if the data doesn't contain that designElement.
        if ( rawvals == null ) return null;
        for ( Integer i : indices ) {
            if ( i == null ) {
                result.add( null );
            } else {

                /*
                 * There can be values missing if some data are missing for some samples. For example, on GSE1004,
                 * sample GSM15832 was run on HG-U95V1 while the rest are on HG-U95V2, so a few probes are missing data.
                 */

                if ( rawvals.size() < ( i + 1 ) ) {
                    throw new IllegalStateException( "Data out of bounds index=" + i + "(" + designElement + " on "
                            + platform + " quant.type # " + quantitationType + ")" );
                }
                Object value = rawvals.get( i );
                if ( value == null ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "No data for index " + i + "(" + designElement + " on " + platform
                                + " quant.type # " + quantitationType + ")" );
                }
                result.add( value );
            }
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

            assert data.get( platform ) != null : platform;

            buf.append( "============== " + platform + " =================\n" );
            for ( Object qType : sampleDimensions.get( platform ).keySet() ) {
                buf.append( "---------------- " + qType + " ------------------\n" );
                buf.append( "DesignEl" );

                for ( GeoSample sam : sampleDimensions.get( platform ).get( qType ) ) {
                    buf.append( "\t" + sam.getGeoAccession() );
                }
                buf.append( "\n" );

                assert data.get( platform ).get( qType ) != null;
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

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class to store the expression data prior to conversion. The data are read from series files sample by sample, and
 * within each sample designElement by designElement, and within each designElement, quantitationType by
 * quantitationType. Values are stored in vectors, roughly equivalent to DesignElementDataVectors.
 * <p>
 * This is an important class as it encompasses how we convert GEO sample data into vectors. There are a couple of
 * assumptions that this is predicated on. First, we assume that all samples are presented with their quantitation types
 * in the same order. Second, we assume that all samples have the same quantitation type, OR at worst, some are missing
 * off the 'end' for some samples (in which case the vectors are padded). We do not assume that all samples have
 * quantitation types with the same names (quantitation types correspond to column names in the GEO files).
 * <p>
 * There are two counterexamples we have found (so far) that push or violate these assumptions: GSE360 and GSE4345
 * (which is really broken). Loading GSE4345 results in a cast exception because the quantitation types are 'mixed up'
 * across the samples.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoValues implements Serializable {

    private static final long serialVersionUID = 3748363645735281578L;

    private static Log log = LogFactory.getLog( GeoValues.class.getName() );

    /*
     * This plays the role of the BioAssayDimension; map of platform --> quantitationType --> samples
     */
    Map<GeoPlatform, Map<Object, LinkedHashSet<GeoSample>>> sampleDimensions = new HashMap<GeoPlatform, Map<Object, LinkedHashSet<GeoSample>>>();

    /*
     * Map of platform --> quantitationtype -> designElement -> values; values in same order as sampleVector.
     */
    Map<GeoPlatform, Map<Object, Map<String, List<Object>>>> data = new HashMap<GeoPlatform, Map<Object, Map<String, List<Object>>>>();

    // private Map<Object, String> quantitationTypeMap = new HashMap<Object, String>();

    private static Collection<String> skippableQuantitationTypes = new HashSet<String>();

    private static Collection<String> aggressivelyRemovedQuantitationTypes = new HashSet<String>();

    static {

        // Most of these are from GenePix files. In Stanford files they are named differently than described here:
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

        //  
        // unfortunately the non-background-subtracted values aren't always available.
        // skippableQuantitationTypes.add( "CH1D_MEAN" );
        // skippableQuantitationTypes.add( "CH2D_MEAN" );
        // skippableQuantitationTypes.add( "CH1D_MEDIAN" );
        // skippableQuantitationTypes.add( "CH2D_MEDIAN" );

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
        skippableQuantitationTypes.add( "SUM_MEDIAN" );
        skippableQuantitationTypes.add( "REGR" );
        skippableQuantitationTypes.add( "CORR" );
        skippableQuantitationTypes.add( "UNF_VALUE" ); // this is the same as 'value' but with the flagged points still
        // in.

        // these occur in some agilent files
        skippableQuantitationTypes.add( "PositionX" );
        skippableQuantitationTypes.add( "PositionY" );
        skippableQuantitationTypes.add( "rNumPix" );
        skippableQuantitationTypes.add( "gNumPix" );

        // Remove these if we see them and we're being aggressive
        aggressivelyRemovedQuantitationTypes.add( "RAT2N_MEAN" );
        aggressivelyRemovedQuantitationTypes.add( "RAT2N_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH2DN_MEAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH2IN_MEAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH2BN_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH2IN_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH2DN_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "RAT1N_MEAN" );
        aggressivelyRemovedQuantitationTypes.add( "RAT1N_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH1DN_MEAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH1IN_MEAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH1BN_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH1IN_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH1DN_MEDIAN" );
        aggressivelyRemovedQuantitationTypes.add( "CH2I_SD" );
        aggressivelyRemovedQuantitationTypes.add( "CH1B_SD" );
        aggressivelyRemovedQuantitationTypes.add( "CH2B_SD" );
        aggressivelyRemovedQuantitationTypes.add( "CH2I_SD" );
        aggressivelyRemovedQuantitationTypes.add( "CH1_PER_SAT" );
        aggressivelyRemovedQuantitationTypes.add( "CH2_PER_SAT" );
        aggressivelyRemovedQuantitationTypes.add( "RAT2_SD" );
        aggressivelyRemovedQuantitationTypes.add( "RAT1_SD" );
        aggressivelyRemovedQuantitationTypes.add( "LOG_RAT2N_MEDIAN" );

    }

    /**
     * Some quantitation types are 'skippable' - they are easily recomputed from other values, or are not necessary in
     * the system. Skipping these makes loading the data more manageable for some data sets that are very large.
     * 
     * @param quantitationTypeName
     * @param aggressive To be more aggressive in remove unwanted quantitation types.
     * @return true if the name is NOT on the 'skippable' list.
     */
    public boolean isWantedQuantitationType( String quantitationTypeName, boolean aggressive ) {
        if ( quantitationTypeName == null )
            throw new IllegalArgumentException( "Quantitation type name cannot be null" );
        return !skippableQuantitationTypes.contains( quantitationTypeName )
                && !aggressivelyRemovedQuantitationTypes.contains( quantitationTypeName );
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
     *        is almost always a good way to match values across samples, there ARE cases where the order isn't the same
     *        for two samples in the same series.
     * @param quantitationTypeIndex Identifies the quantitation type.
     * @param designElement
     * @param value The data point to be stored.
     */
    public void addValue( GeoSample sample, Integer quantitationTypeIndex, String designElement, Object value ) {

        // we really don't allow null values at this stage.
        if ( value == null ) {
            throw new IllegalArgumentException( "Attempted to add null for sample=" + sample + " qtype="
                    + quantitationTypeIndex + " de=" + designElement );
        }

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
    }

    private Map<GeoPlatform, Map<String, Integer>> quantitationTypeNameMap = new HashMap<GeoPlatform, Map<String, Integer>>();
    private Map<GeoPlatform, Map<Integer, Collection<String>>> quantitationTypeIndexMap = new HashMap<GeoPlatform, Map<Integer, Collection<String>>>();

    /**
     * @param columnName
     * @param index - the actual index of the data in the final data structure, not necessarily the column where the
     *        data are found in the data file (as that can vary from sample to sample).
     */
    public void addQuantitationType( GeoPlatform platform, String columnName, Integer index ) {
        if ( columnName == null ) throw new IllegalArgumentException( "Column name cannot be null" );

        if ( !quantitationTypeNameMap.containsKey( platform ) ) {
            quantitationTypeNameMap.put( platform, new HashMap<String, Integer>() );
            quantitationTypeIndexMap.put( platform, new HashMap<Integer, Collection<String>>() );
        }

        Map<String, Integer> qtNameMapForPlatform = quantitationTypeNameMap.get( platform );
        Map<Integer, Collection<String>> qtIndexMapForPlatform = quantitationTypeIndexMap.get( platform );

        if ( qtNameMapForPlatform.containsKey( columnName )
                && qtNameMapForPlatform.get( columnName ).intValue() != index.intValue() ) {
            throw new IllegalArgumentException( "You just tried to reassign the column for a quantitation type" );
        }

        qtNameMapForPlatform.put( columnName, index );
        if ( !qtIndexMapForPlatform.containsKey( index ) ) {
            qtIndexMapForPlatform.put( index, new HashSet<String>() );
            qtIndexMapForPlatform.get( index ).add( columnName );
            log.debug( "Added quantitation type " + columnName + " at index " + index + " for platform " + platform );
        }

        // did we get a new column name for the same index?
        if ( !qtIndexMapForPlatform.get( index ).contains( columnName ) ) {
            /*
             * This is often a bad thing -- we have to live with it. It means we already have a QT for this column, but
             * now the name has changed. Sometimes it's just a name change - some people put the sample name as a suffix
             * in the quantitation type, for example. In other cases, it means the data for one quantitation type will
             * effectively be used for another; validation will fail in this case because the number of values won't
             * match for all the vectors.
             */
            log.warn( "Column #" + index + " has an additional name: " + columnName + ", it already has names: "
                    + StringUtils.join( qtIndexMapForPlatform.get( index ), " " ) );

            qtIndexMapForPlatform.get( index ).add( columnName ); // add it anyway.

        }

    }

    /**
     * @param columnName
     * @return
     */
    public Integer getQuantitationTypeIndex( GeoPlatform platform, String columnName ) {
        return this.quantitationTypeNameMap.get( platform ).get( columnName );
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
    public List<Object> getValues( GeoPlatform platform, Integer quantitationType, String designElement ) {
        return data.get( platform ).get( quantitationType ).get( designElement );
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
        Map<Object, Map<String, List<Object>>> map = data.get( platform );
        assert map != null;
        Map<String, List<Object>> map2 = map.get( quantitationType );
        assert map2 != null : "No data for qt " + quantitationType + " on " + platform;
        List<Object> rawvals = map2.get( designElement );

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
                    throw new IllegalStateException( "Data out of bounds index=" + i + " (" + designElement + " on "
                            + platform + " quant.type # " + quantitationType + ") - vector has only " + rawvals.size()
                            + " values." );
                }
                Object value = rawvals.get( i );
                if ( value == null ) {
                    if ( log.isDebugEnabled() )
                        log
                                .debug( "No data for index " + i + " (" + designElement + " on " + platform
                                        + " quant.type # " + quantitationType + ") - vector has " + rawvals.size()
                                        + " values." );
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
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for ( GeoPlatform platform : sampleDimensions.keySet() ) {

            assert data.get( platform ) != null : platform;

            buf.append( "============== " + platform + " =================\n" );
            Object[] ar = sampleDimensions.get( platform ).keySet().toArray();
            Arrays.sort( ar );
            for ( Object qType : ar ) {
                String qTName = StringUtils.join( quantitationTypeIndexMap.get( platform ).get( qType ), "/" );
                buf.append( "---------------- Platform " + platform + " QuantitationType #" + qType + " (" + qTName
                        + ") ------------------\n" );
                buf.append( "DeEl" );

                for ( GeoSample sam : sampleDimensions.get( platform ).get( qType ) ) {
                    buf.append( "\t" + sam.getGeoAccession() );
                }
                buf.append( "\n" );

                assert data.get( platform ).get( qType ) != null;
                Object[] els = data.get( platform ).get( qType ).keySet().toArray();
                Arrays.sort( els );
                for ( Object dEl : els ) {
                    buf.append( dEl );
                    for ( Object val : data.get( platform ).get( qType ).get( dEl ) ) {
                        if ( StringUtils.isBlank( val.toString() ) ) {
                            val = ".";
                        }
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

    /**
     * This method can only be called once a sample has been completely processed, and before a new sample has been
     * started.
     */
    public void validate() {
        for ( GeoPlatform platform : sampleDimensions.keySet() ) {

            Map<Object, Map<String, List<Object>>> d = data.get( platform );

            for ( Object qType : sampleDimensions.get( platform ).keySet() ) {

                // This is the number of samples that have been processed so far for the given quantitation type.
                int numSamples = sampleDimensions.get( platform ).get( qType ).size();

                if ( skippableQuantitationTypes.contains( qType ) ) continue;
                Collection<String> qtNames = quantitationTypeIndexMap.get( platform ).get( qType );

                // if ( qtNames.size() > 1 ) {
                // log.warn( "There are " + qtNames.size() + " names for this data column" );
                // }

                Map<String, List<Object>> q = d.get( qType );
                boolean warned = false;
                for ( String designElement : q.keySet() ) {
                    List<Object> vals = q.get( designElement );
                    if ( vals.size() < numSamples ) {
                        int paddingAmount = numSamples - vals.size();
                        if ( !warned )
                            log.warn( "Padding some vectors with " + paddingAmount + " values for quantitation type "
                                    + qType + "(" + StringUtils.join( qtNames, "/" ) + ")" );
                        warned = true;
                        for ( int i = 0; i < paddingAmount; i++ ) {
                            vals.add( null );
                        }
                    } else if ( vals.size() > numSamples ) {
                        log.error( "Samples so far: "
                                + StringUtils.join( sampleDimensions.get( platform ).get( qType ), ',' ) );
                        throw new IllegalStateException( "Validation failed at platform=" + platform
                                + " designelement=" + designElement + " qType=" + qType + " expected " + numSamples
                                + " values, got " + vals.size() + "; name(s) for qType are "
                                + StringUtils.join( qtNames, "," ) );
                        // pad all the other vectors that are too short.
                        // int paddingAmount = vals.size() - numSamples;
                        // if ( !warned )
                        // log.warn( "Vector for designelement=" + designElement + " on platform=" + platform
                        // + " is long; Padding other vectors with " + paddingAmount
                        // + " values for quantitation type " + qType + " (" + StringUtils.join( qtNames, "/" )
                        // + ")" );
                        // warned = true;
                        // for ( String de : q.keySet() ) {
                        // List<Object> v = q.get( de );
                        // if ( v.size() < vals.size() ) {
                        // for ( int i = 0; i < paddingAmount; i++ ) {
                        // vals.add( null );
                        // }
                        // }
                        // }
                    }
                }
                if ( log.isDebugEnabled() )
                    log.debug( qType + " ok on " + platform + ", all vectors have " + numSamples + " values" );
            }
        }

    }
}

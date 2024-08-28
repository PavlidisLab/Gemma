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
package ubic.gemma.core.loader.expression.geo.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.expression.geo.model.GeoDataset.PlatformType;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class to store the expression data prior to conversion. The data are read from series files sample by sample, and
 * within each sample designElement by designElement, and within each designElement, quantitationType by
 * quantitationType. Values are stored in vectors, roughly equivalent to DesignElementDataVectors.
 * This is an important class as it encompasses how we convert GEO sample data into vectors. There are a couple of
 * assumptions that this is predicated on. First, we assume that all samples are presented with their quantitation types
 * in the same order. Second, we assume that all samples have the same quantitation type, OR at worst, some are missing
 * off the 'end' for some samples (in which case the vectors are padded). We do not assume that all samples have
 * quantitation types with the same names (quantitation types correspond to column names in the GEO files).
 * There are two counterexamples we have found (so far) that push or violate these assumptions: GSE360 and GSE4345
 * (which is really broken). Loading GSE4345 results in a cast exception because the quantitation types are 'mixed up'
 * across the samples.
 *
 * @author pavlidis
 */
public class GeoValues implements Serializable {

    private static final long serialVersionUID = 3748363645735281578L;
    private static final Collection<String> aggressivelyRemovedQuantitationTypes = new HashSet<>();
    private static final Log log = LogFactory.getLog( GeoValues.class.getName() );
    private static final Collection<String> skippableQuantitationTypes = new HashSet<>();

    // private Map<Object, String> quantitationTypeMap = new HashMap<Object, String>();

    static {

        // Most of these are from GenePix files. In Stanford files they are named differently than described here:
        // http://www.moleculardevices.com/pages/software/gn_genepix_file_formats.html

        // these are location and spot size information.
        GeoValues.skippableQuantitationTypes.add( "X_COORD" );
        GeoValues.skippableQuantitationTypes.add( "Y_COORD" );
        GeoValues.skippableQuantitationTypes.add( "X" );
        GeoValues.skippableQuantitationTypes.add( "Y" );
        GeoValues.skippableQuantitationTypes.add( "TOP" );
        GeoValues.skippableQuantitationTypes.add( "BOT" );
        GeoValues.skippableQuantitationTypes.add( "LEFT" );
        GeoValues.skippableQuantitationTypes.add( "RIGHT" );
        GeoValues.skippableQuantitationTypes.add( "DIAMETER" );
        GeoValues.skippableQuantitationTypes.add( "TOT_SPIX" );
        GeoValues.skippableQuantitationTypes.add( "TOT_BPIX" );
        GeoValues.skippableQuantitationTypes.add( "Slide_block" );
        GeoValues.skippableQuantitationTypes.add( "Slide_row" );
        GeoValues.skippableQuantitationTypes.add( "B Pixels" );
        GeoValues.skippableQuantitationTypes.add( "F Pixels" );
        GeoValues.skippableQuantitationTypes.add( "Bkgd_area" );
        GeoValues.skippableQuantitationTypes.add( "Spot_area" );
        GeoValues.skippableQuantitationTypes.add( "Spot_diameter" );
        GeoValues.skippableQuantitationTypes.add( "Bkgd_diameter" );
        GeoValues.skippableQuantitationTypes.add( "CH2_BKD_AREA" );
        GeoValues.skippableQuantitationTypes.add( "CH1_BKD_AREA" );
        GeoValues.skippableQuantitationTypes.add( "ch1 Area" );
        GeoValues.skippableQuantitationTypes.add( "ch2 Area" );
        GeoValues.skippableQuantitationTypes.add( "CH1_AREA" );
        GeoValues.skippableQuantitationTypes.add( "AREA" );
        GeoValues.skippableQuantitationTypes.add( "CH2_AREA" );
        GeoValues.skippableQuantitationTypes.add( "CH2_Spot_Area" );
        GeoValues.skippableQuantitationTypes.add( "CH1_Spot_Area" );
        GeoValues.skippableQuantitationTypes.add( "CH1_SIGNAL_AREA" );
        GeoValues.skippableQuantitationTypes.add( "CH2_SIGNAL_AREA" );
        GeoValues.skippableQuantitationTypes.add( "Spot Area" );
        GeoValues.skippableQuantitationTypes.add( "Area To Perimeter" );
        GeoValues.skippableQuantitationTypes.add( "Background Area" );
        GeoValues.skippableQuantitationTypes.add( "Signal Area" );
        GeoValues.skippableQuantitationTypes.add( "Ignored Area" );
        GeoValues.skippableQuantitationTypes.add( "Probe 1Area%" );
        GeoValues.skippableQuantitationTypes.add( "BKD_AREA" );
        GeoValues.skippableQuantitationTypes.add( "Perim-to-area failed" );
        GeoValues.skippableQuantitationTypes.add( "F_AREA_L" );
        GeoValues.skippableQuantitationTypes.add( "F_AREA_H" );
        GeoValues.skippableQuantitationTypes.add( "F_AREA_M" );
        GeoValues.skippableQuantitationTypes.add( "Probe 2Area%" );
        GeoValues.skippableQuantitationTypes.add( "Probe 2 %Area" );
        GeoValues.skippableQuantitationTypes.add( "B_AREA_H" );
        GeoValues.skippableQuantitationTypes.add( "B_AREA_M" );
        GeoValues.skippableQuantitationTypes.add( "B_AREA_L" );
        GeoValues.skippableQuantitationTypes.add( "Bkgd_area" );
        GeoValues.skippableQuantitationTypes.add( "Dia." );
        GeoValues.skippableQuantitationTypes.add( "Slide_row" );
        GeoValues.skippableQuantitationTypes.add( "Slide_column" );
        GeoValues.skippableQuantitationTypes.add( "Slide_block" );
        // unfortunately the non-background-subtracted values aren't always available.
        // skippableQuantitationTypes.add( "CH1D_MEAN" );
        // skippableQuantitationTypes.add( "CH2D_MEAN" );
        // skippableQuantitationTypes.add( "CH1D_MEDIAN" );
        // skippableQuantitationTypes.add( "CH2D_MEDIAN" );

        // some raw items are skippable.(assumes we use median)
        GeoValues.skippableQuantitationTypes.add( "SUM_MEAN" );
        GeoValues.skippableQuantitationTypes.add( "RAT1_MEAN" );
        GeoValues.skippableQuantitationTypes.add( "RAT2_MEAN" );
        GeoValues.skippableQuantitationTypes.add( "PIX_RAT2_MEAN" );
        GeoValues.skippableQuantitationTypes.add( "PIX_RAT2_MEDIAN" );

        // otherwise deemed skippable.
        GeoValues.skippableQuantitationTypes.add( "PERGTBCH1I_1SD" );
        GeoValues.skippableQuantitationTypes.add( "PERGTBCH2I_1SD" );
        GeoValues.skippableQuantitationTypes.add( "PERGTBCH1I_2SD" );
        GeoValues.skippableQuantitationTypes.add( "PERGTBCH2I_2SD" );

        // these removed just in the interest of saving memory!
        GeoValues.skippableQuantitationTypes.add( "SUM_MEDIAN" );
        GeoValues.skippableQuantitationTypes.add( "REGR" );
        GeoValues.skippableQuantitationTypes.add( "CORR" );
        GeoValues.skippableQuantitationTypes
                .add( "UNF_VALUE" ); // this is the same as 'value' but with the flagged points still
        // in.

        // these occur in some agilent files
        GeoValues.skippableQuantitationTypes.add( "PositionX" );
        GeoValues.skippableQuantitationTypes.add( "PositionY" );
        GeoValues.skippableQuantitationTypes.add( "rNumPix" );
        GeoValues.skippableQuantitationTypes.add( "gNumPix" );

        // Remove these if we see them and we're being aggressive
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "RAT2N_MEAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "RAT2N_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2DN_MEAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2IN_MEAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2BN_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2IN_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2DN_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "RAT1N_MEAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "RAT1N_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1DN_MEAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1IN_MEAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1BN_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1IN_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1DN_MEDIAN" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2I_SD" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1B_SD" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2B_SD" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2I_SD" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH1_PER_SAT" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "CH2_PER_SAT" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "RAT2_SD" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "RAT1_SD" );
        GeoValues.aggressivelyRemovedQuantitationTypes.add( "LOG_RAT2N_MEDIAN" );

        String[] moreAgg = new String[] { "gBGPixSDev", "ch1 Background Std Dev", "rBGPixSDev",
                "ch2 Background Std Dev", "CY3_BKD_SD", "CY5_BKD_SD", "BEAD_STDERR", "CH1_BKD_SD", "CH2_BKD_SD",
                "R_BG_SD", "G_BG_SD", "CH1_SD", "CH2_SD", "G_SD", "R_SD", "ch1 Background Std Dev",
                "ch1 Signal Noise Ratio", "ch2 Background Std Dev", "ch2 Signal Noise Ratio", "Bkgd_stdev", "F635 SD",
                "F532 SD" };

        GeoValues.aggressivelyRemovedQuantitationTypes.addAll( Arrays.asList( moreAgg ) );

        // We no longer keep "absent-present" calls (affymetrix and others)
        GeoValues.skippableQuantitationTypes.add( "ABS_CALL" );
        GeoValues.skippableQuantitationTypes.add( "ABS CALL" );
        GeoValues.skippableQuantitationTypes.add( "CALL" );
        GeoValues.skippableQuantitationTypes.add( "Detection Pval" );
        GeoValues.skippableQuantitationTypes.add( "DETECTION P-VALUE" );
        GeoValues.skippableQuantitationTypes.add( "Detection_p-value" );
        GeoValues.skippableQuantitationTypes.add( "Detection_pvalue" );
        GeoValues.skippableQuantitationTypes.add( "D_P-VALUE" );

        GeoValues.skippableQuantitationTypes.add( "Detection" );
        GeoValues.skippableQuantitationTypes.add( "Detection call" );
        GeoValues.skippableQuantitationTypes.add( "rIsWellAboveBG" );
        GeoValues.skippableQuantitationTypes.add( "gIsWellAboveBG" );
        GeoValues.skippableQuantitationTypes.add( "CH2_IsWellAboveBG" );
        GeoValues.skippableQuantitationTypes.add( "CH1_IsWellAboveBG" );
        GeoValues.skippableQuantitationTypes.add( "COMPUTED.G_IS_WELL_ABOVE_BG" );
        GeoValues.skippableQuantitationTypes.add( "COMPUTED.R_IS_WELL_ABOVE_BG" );
        // related QC calls that are too hard to use as they have no consistent meaning.
        GeoValues.skippableQuantitationTypes.add( "FLAG" );
        GeoValues.skippableQuantitationTypes.add( "FLAGS" );
        GeoValues.skippableQuantitationTypes.add( "QUALITY_FLAG" );
        GeoValues.skippableQuantitationTypes.add( "CH2_Flag" );
        GeoValues.skippableQuantitationTypes.add( "CH1_Flag" );
        GeoValues.skippableQuantitationTypes.add( "IsManualFlag" );
        GeoValues.skippableQuantitationTypes.add( "Flag_high_pmt" );
        GeoValues.skippableQuantitationTypes.add( "Flag_low_pmt" );
        GeoValues.skippableQuantitationTypes.add( "Autoflag" );
        GeoValues.skippableQuantitationTypes.add( "FLAGGED" );
        GeoValues.skippableQuantitationTypes.add( "FlagCy5" );
        GeoValues.skippableQuantitationTypes.add( "FlagCy3" );
        GeoValues.skippableQuantitationTypes.add( "FLAG_L" );
        GeoValues.skippableQuantitationTypes.add( "FLAG_H" );
        GeoValues.skippableQuantitationTypes.add( "Flagbkgrd" );
        GeoValues.skippableQuantitationTypes.add( "FLAG_M" );
        GeoValues.skippableQuantitationTypes.add( "flag1" );
        GeoValues.skippableQuantitationTypes.add( "Flag.30236" );
        GeoValues.skippableQuantitationTypes.add( "flag2" );
        GeoValues.skippableQuantitationTypes.add( "Flagged?" );
        GeoValues.skippableQuantitationTypes.add( "Pos_Fraction" );
        GeoValues.skippableQuantitationTypes.add( "Pairs_Used" );

        String[] moreSkip = new String[] { "Pos_Fraction", "% > B635+2SD", "% > B635+1SD", "% > B532+2SD",
                "% > B532+1SD", "F532 % Sat.", "F635 % Sat.", "rIsSaturated", "gIsSaturated", "ch1 Signal Noise Ratio",
                "ch2 Signal Noise Ratio", "gIsFeatNonUnifOL", "gIsPosAndSignif", "rIsPosAndSignif",
                "rIsFeatNonUnifOL" };

        GeoValues.skippableQuantitationTypes.addAll( Arrays.asList( moreSkip ) );

    }

    /*
     * Map of platform --> quantitationtype -> designElement -> values; values in same order as sampleVector.
     */
    private final Map<GeoPlatform, Map<Integer, Map<String, List<Object>>>> data = new HashMap<>();
    private final Map<GeoPlatform, Map<Integer, Collection<String>>> quantitationTypeIndexMap = new HashMap<>();
    private final Map<GeoPlatform, Map<String, Integer>> quantitationTypeNameMap = new HashMap<>();

    /*
     * This plays the role of the BioAssayDimension; map of platform --> quantitationType --> samples
     */
    private final Map<GeoPlatform, Map<Integer, LinkedHashSet<GeoSample>>> sampleDimensions = new HashMap<>();

    /**
     * @param columnName column name
     * @param index      - the actual index of the data in the final data structure, not necessarily the column where the
     *                   data are found in the data file (as that can vary from sample to sample).
     * @param platform   platform
     */
    public void addQuantitationType( GeoPlatform platform, String columnName, Integer index ) {
        if ( columnName == null )
            throw new IllegalArgumentException( "Column name cannot be null" );

        if ( !quantitationTypeNameMap.containsKey( platform ) ) {
            quantitationTypeNameMap.put( platform, new HashMap<>() );
            quantitationTypeIndexMap.put( platform, new HashMap<>() );
        }

        Map<String, Integer> qtNameMapForPlatform = quantitationTypeNameMap.get( platform );
        Map<Integer, Collection<String>> qtIndexMapForPlatform = quantitationTypeIndexMap.get( platform );

        if ( qtNameMapForPlatform.containsKey( columnName )
                && qtNameMapForPlatform.get( columnName ).intValue() != index.intValue() ) {
            throw new IllegalArgumentException( "You just tried to reassign the column for a quantitation type" );
        }

        qtNameMapForPlatform.put( columnName, index );
        if ( !qtIndexMapForPlatform.containsKey( index ) ) {
            qtIndexMapForPlatform.put( index, new HashSet<>() );
            qtIndexMapForPlatform.get( index ).add( columnName );
            GeoValues.log.debug( "Added quantitation type " + columnName + " at index " + index + " for platform "
                    + platform );
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
            GeoValues.log
                    .warn( "Column #" + index + " has an additional name: " + columnName + ", it already has names: "
                            + StringUtils.join( qtIndexMapForPlatform.get( index ), " " ) );

            /*
             * We check here to see if the quantitation type is actually an unwanted one, based on the name.
             */
            boolean newNameIsWanted = this.isWantedQuantitationType( columnName );

            if ( !newNameIsWanted ) {
                GeoValues.log
                        .warn( "Alternate name is an unwanted quantitation type; data may be retaining anyway because of other name" );
            }

            qtIndexMapForPlatform.get( index ).add( columnName ); // add it anyway.

        }

    }

    /**
     * Only call this to add a sample for which there are no data.
     *
     * @param sample geo sample
     */
    public void addSample( GeoSample sample ) {
        GeoPlatform platform = sample.getPlatforms().iterator().next();

        if ( platform.getTechnology().equals( PlatformType.MPSS ) || platform.getTechnology()
                .equals( PlatformType.SAGE ) ) {
            /*
             * We're not going to add data for this. Setting this false is a bit redundant (see
             * GeoPlatform.useDataFromGeo) but can't hurt.
             */
            platform.setUseDataFromGEO( false );

        } else if ( !sampleDimensions.containsKey( platform ) ) {
            /*
             * Problem: if this is the first sample, we don't know how many quantitation types to expect. However, for
             * some data sets, there is no data provided in the SOFT file (e.g., RNA-seq), so this would be okay.
             */
            if ( sample.isMightNotHaveDataInFile() ) {
                this.addSample( sample, 0 );
                GeoValues.log
                        .warn( "Data not anticipated to be present (RNA-seq etc.), adding dummy quantitation type" );
                return;
                // throw new IllegalStateException( "Samples must have a platform assigned." );
            }

            // exon array data sets are sometimes missing the data, which we compute later anyway from CEL files.
            // See bug 3981 and GSE28383 and GSE28886
            if ( GeoPlatform.isAffymetrixExonArray( platform.getGeoAccession() ) ) {
                this.addSample( sample, 0 );
                sample.setMightNotHaveDataInFile( true );
                GeoValues.log.warn( "Data not anticipated to be usable (exon arrays), adding dummy quantitation type" );
                return;
            }

            this.addSample( sample, 0 );
            sample.setMightNotHaveDataInFile( true );
            GeoValues.log.warn( "Sample lacks data, no data will be imported for this data set" );
            platform.setUseDataFromGEO( false );

            // throw new UnsupportedOperationException(
            // "Can't deal with empty samples when that sample is the first one on its platform." );

        } else {
            Map<Integer, LinkedHashSet<GeoSample>> samplePlatformMap = sampleDimensions.get( platform );
            for ( Integer quantitationTypeIndex : samplePlatformMap.keySet() ) {
                LinkedHashSet<GeoSample> sampleQtMap = samplePlatformMap.get( quantitationTypeIndex );
                sampleQtMap.add( sample );
            }
        }

    }

    /**
     * Store a value. It is assumed that designElements have unique names.
     * Implementation note: The first time we see a sample, we associate it with a 'dimension' that is connected to the
     * platform and quantitation type. In parallel, we add the data to a 'vector' for the designElement that is likewise
     * connected to the platform the sample uses, the quantitation type. Because in GEO files samples are seen one at a
     * time, the vectors for each designElement are built up. Thus it is important that we add a value for each sample
     * for each design element.
     * Note what happens if data is MISSING for a given designElement/quantitationType/sample combination. This can
     * happen (typically all the quantitation types for a designElement in a given sample). This method will NOT be
     * called. When the next sample is processed, the new data will be added onto the end in the wrong place. Then the
     * data in the vectors stored here will be incorrect. Thus the GEO parser has to ensure that each vector is
     * 'completed' before moving to the next sample.
     *
     * @param sample                sample
     * @param quantitationTypeIndex The column number for the quantitation type, needed because the names of the
     *                              quantitation types don't always match across samples (but hopefully the columns do). Even though the first
     *                              column contains the design element name (ID_REF), the first quantitation type should be numbered 0. This
     *                              is almost always a good way to match values across samples, there ARE cases where the order isn't the same
     *                              for two samples in the same series.
     * @param designElement         design element
     * @param value                 The data point to be stored.
     */
    public void addValue( GeoSample sample, Integer quantitationTypeIndex, String designElement, Object value ) {

        // we really don't allow null values at this stage.
        if ( value == null ) {
            throw new IllegalArgumentException(
                    "Attempted to add null for sample=" + sample + " qtype=" + quantitationTypeIndex + " de="
                            + designElement );
        }

        GeoPlatform platform = this.addSample( sample, quantitationTypeIndex );

        if ( !data.containsKey( platform ) ) {
            data.put( platform, new HashMap<>() );
        }

        Map<Integer, Map<String, List<Object>>> platformMap = data.get( platform );
        if ( !platformMap.containsKey( quantitationTypeIndex ) ) {
            platformMap.put( quantitationTypeIndex, new HashMap<>() );
        }

        Map<String, List<Object>> qtMap = platformMap.get( quantitationTypeIndex );
        if ( !qtMap.containsKey( designElement ) ) {
            qtMap.put( designElement, new ArrayList<>() );
        }

        qtMap.get( designElement ).add( value );

        if ( GeoValues.log.isTraceEnabled() ) {
            GeoValues.log.trace( "Adding value for platform=" + platform + " sample=" + sample + " qt="
                    + quantitationTypeIndex + " de=" + designElement + " value=" + value );
        }
    }

    /**
     * Remove the data for a given platform (use to save memory)
     *
     * @param geoPlatform geo platform
     */
    public void clear( GeoPlatform geoPlatform ) {
        this.data.remove( geoPlatform );
    }

    /**
     * If possible, null out the data for a quantitation type on a given platform.
     *
     * @param platform              platform
     * @param datasetSamples        dataset samples
     * @param quantitationTypeIndex QT index
     */
    public void clear( GeoPlatform platform, List<GeoSample> datasetSamples, Integer quantitationTypeIndex ) {
        if ( datasetSamples.size() != sampleDimensions.get( platform ).get( quantitationTypeIndex ).size() ) {
            return; // can't really clear
        }
        GeoValues.log.debug( "Clearing" );
        data.get( platform ).remove( quantitationTypeIndex );
    }

    /**
     * Get the indices of the data for a set of samples - this can be used to get a slice of the data. This is
     * inefficient but shouldn't need to be called all that frequently.
     *
     * @param platform         platform
     * @param neededSamples,   must be from the same platform. If we don't have data for a given sample, the index
     *                         returned will be null. This can happen when some samples don't have all the quantitation types (GSE360 for
     *                         example).
     * @param quantitationType quantitation type
     * @return integer array
     */
    public Integer[] getIndices( GeoPlatform platform, List<GeoSample> neededSamples, Integer quantitationType ) {

        if ( !platform.useDataFromGeo() ) {
            return null;
        }

        assert sampleDimensions.get( platform ) != null;
        if ( sampleDimensions.get( platform ).get( quantitationType ) == null ) {
            return null; // filtered out?
        }

        List<Integer> result = new ArrayList<>();
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

            if ( !found )
                result.add( null );

        }

        return result.toArray( new Integer[0] );

    }

    public Integer getQuantitationTypeIndex( GeoPlatform platform, String columnName ) {
        Map<String, Integer> map = this.quantitationTypeNameMap.get( platform );
        if ( map == null ) {
            // See bug 4181 - this happens when the platform has no data and we don't expect that.
            throw new IllegalStateException( "No QT map for " + platform );
        }
        return map.get( columnName );
    }

    /**
     * @param samplePlatform sample platform
     * @return Collection of Objects representing the quantitation types for the given platform.
     */
    public Collection<Integer> getQuantitationTypes( GeoPlatform samplePlatform ) {
        assert samplePlatform != null;
        assert this.data.get( samplePlatform ) != null;
        return this.data.get( samplePlatform ).keySet();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public List<Object> getValues( GeoPlatform platform, Integer quantitationType, String designElement ) {
        return data.get( platform ).get( quantitationType ).get( designElement );
    }

    /**
     * @param quantitationType QT
     * @param designElement    design element
     * @param indices          indices
     * @param platform         platforms
     * @return a 'slice' of the data corresponding to the indices provided.
     */
    public List<Object> getValues( GeoPlatform platform, Integer quantitationType, String designElement,
            Integer[] indices ) {
        List<Object> result = new ArrayList<>();
        Map<Integer, Map<String, List<Object>>> map = data.get( platform );
        assert map != null : "No data for platform=" + platform;
        Map<String, List<Object>> map2 = map.get( quantitationType );
        assert map2 != null : "No data for qt " + quantitationType + " on " + platform;
        List<Object> rawvals = map2.get( designElement );

        // this can happen if the data doesn't contain that designElement.
        if ( rawvals == null )
            return null;
        for ( Integer i : indices ) {
            if ( i == null ) {
                result.add( null );
            } else {

                /*
                 * There can be values missing if some data are missing for some samples. For example, on GSE1004,
                 * sample GSM15832 was run on HG-U95V1 while the rest are on HG-U95V2, so a few probes are missing data.
                 */
                if ( rawvals.size() < ( i + 1 ) ) {
                    throw new IllegalStateException(
                            "Data out of bounds index=" + i + " (" + designElement + " on " + platform
                                    + " quant.type # " + quantitationType + ") - vector has only " + rawvals.size()
                                    + " values." );
                }
                Object value = rawvals.get( i );
                if ( value == null ) {
                    if ( GeoValues.log.isDebugEnabled() )
                        GeoValues.log.debug( "No data for index " + i + " (" + designElement + " on " + platform
                                + " quant.type # " + quantitationType + ") - vector has " + rawvals.size()
                                + " values." );
                }
                result.add( value );
            }
        }
        return result;
    }

    public boolean hasData() {
        return !this.sampleDimensions.isEmpty();
    }

    /**
     * Some quantitation types are 'skippable' - they are easily recomputed from other values, or are not necessary in
     * the system. Skipping these makes loading the data more manageable for some data sets that are very large.
     *
     * @param quantitationTypeName QT name
     * @return true if the name is NOT on the 'skippable' list.
     */
    public boolean isWantedQuantitationType( String quantitationTypeName ) {
        if ( quantitationTypeName == null )
            throw new IllegalArgumentException( "Quantitation type name cannot be null" );
        return !GeoValues.skippableQuantitationTypes.contains( quantitationTypeName )
                && !GeoValues.aggressivelyRemovedQuantitationTypes.contains( quantitationTypeName );
    }

    /**
     * This creates a new GeoValues that has data only for the selected samples. The quantiatation type information will
     * be semi-deep copies. This is only needed for when we are splitting a series apart, especially when it is not
     * along Platform lines.
     *
     * @param samples samples
     * @return geo values
     */
    public GeoValues subset( Collection<GeoSample> samples ) {

        GeoValues v = new GeoValues();

        /*
         * First, create new sampleDimensions and start setting up empty data.
         */
        for ( GeoSample s : samples ) {
            GeoPlatform p = s.getPlatforms().iterator().next();

            if ( !v.sampleDimensions.containsKey( p ) ) {
                v.sampleDimensions.put( p, new HashMap<>() );

                // deep copy.
                for ( Integer o : this.sampleDimensions.get( p ).keySet() ) {
                    v.sampleDimensions.get( p ).put( o, new LinkedHashSet<>() );
                    for ( GeoSample ss : this.sampleDimensions.get( p ).get( o ) ) {
                        v.sampleDimensions.get( p ).get( o ).add( ss ); // could use add all
                    }
                }

                v.data.put( p, new HashMap<>() );
                for ( Integer o : this.data.get( p ).keySet() ) {
                    v.data.get( p ).put( o, new HashMap<>() );

                    for ( String probeId : this.data.get( p ).get( o ).keySet() ) {
                        v.data.get( p ).get( o ).put( probeId, new ArrayList<>() );
                    }
                }
            }
        }

        /*
         * Then, subset the data.
         */
        for ( GeoPlatform p : v.sampleDimensions.keySet() ) {
            for ( Integer o : v.sampleDimensions.get( p ).keySet() ) {
                LinkedHashSet<GeoSample> dimsamples = v.sampleDimensions.get( p ).get( o );

                int i = 0;
                for ( Iterator<GeoSample> it = dimsamples.iterator(); it.hasNext(); ) {
                    GeoSample geoSample = it.next();

                    if ( samples.contains( geoSample ) ) {

                        Map<String, List<Object>> newmap = v.data.get( p ).get( o );
                        for ( String probeId : newmap.keySet() ) {
                            newmap.get( probeId ).add( this.data.get( p ).get( o ).get( probeId ).get( i ) );
                        }

                    } else {
                        // this is where we remove the unneded samples from the sampledimensions.
                        it.remove();
                    }

                    i++;
                }

            }
        }

        /*
         * The qt stuff can just be copied over, not deep copy.
         */
        v.quantitationTypeIndexMap.putAll( this.quantitationTypeIndexMap );
        v.quantitationTypeNameMap.putAll( this.quantitationTypeNameMap );

        return v;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for ( GeoPlatform platform : sampleDimensions.keySet() ) {

            assert data.get( platform ) != null : platform;

            buf.append( "============== " ).append( platform ).append( " =================\n" );
            List<Integer> ar = new ArrayList<>( sampleDimensions.get( platform ).keySet() );
            Collections.sort( ar );
            for ( Integer qType : ar ) {
                String qTName = StringUtils.join( quantitationTypeIndexMap.get( platform ).get( qType ), "/" );
                buf.append( "---------------- Platform " ).append( platform ).append( " QuantitationType #" )
                        .append( qType ).append( " (" ).append( qTName ).append( ") ------------------\n" );
                buf.append( "DeEl" );

                for ( GeoSample sam : sampleDimensions.get( platform ).get( qType ) ) {
                    buf.append( "\t" ).append( sam.getGeoAccession() );
                }
                buf.append( "\n" );

                Map<String, List<Object>> map = data.get( platform ).get( qType );
                assert map != null;
                List<String> els = new ArrayList<>( map.keySet() );
                Collections.sort( els );
                for ( String dEl : els ) {
                    buf.append( dEl );

                    for ( Object val : map.get( dEl ) ) {
                        if ( val == null || StringUtils.isBlank( val.toString() ) ) {
                            val = ".";
                        }
                        buf.append( "\t" ).append( val );
                    }

                    buf.append( "\n" );
                }
            }
        }

        if ( buf.length() == 0 ) {
            return "No values stored";
        }

        return buf.toString();
    }

    /**
     * This method can only be called once a sample has been completely processed, and before a new sample has been
     * started.
     */
    public void validate() {
        for ( GeoPlatform platform : sampleDimensions.keySet() ) {

            Map<Integer, Map<String, List<Object>>> d = data.get( platform );

            for ( Integer qType : sampleDimensions.get( platform ).keySet() ) {

                // This is the number of samples that have been processed so far for the given quantitation type.
                int numSamples = sampleDimensions.get( platform ).get( qType ).size();

                // FIXME! this is not comparing the QT index, not its value!
                if ( GeoValues.skippableQuantitationTypes.contains( qType ) )
                    continue;

                Map<Integer, Collection<String>> qtMap = quantitationTypeIndexMap.get( platform );
                if ( qtMap == null ) {
                    // for data sets where there is no data, this could happen.
                    if ( platform.useDataFromGeo() ) {
                        throw new IllegalStateException( "Missing quantitation type index map for " + platform );
                    }
                    continue;

                }
                Collection<String> qtNames = qtMap.get( qType );

                Map<String, List<Object>> q = d.get( qType );
                boolean warned = false;
                for ( Entry<String, List<Object>> e : q.entrySet() ) {
                    String designElement = e.getKey();
                    List<Object> vals = e.getValue();

                    if ( vals.size() < numSamples ) {
                        int paddingAmount = numSamples - vals.size();
                        if ( !warned )
                            GeoValues.log.warn( "Padding some vectors with " + paddingAmount
                                    + " values for quantitation type " + qType + "(" + StringUtils.join( qtNames, "/" )
                                    + ")" );
                        warned = true;
                        for ( int i = 0; i < paddingAmount; i++ ) {
                            vals.add( null );
                        }
                    } else if ( vals.size() > numSamples ) {
                        GeoValues.log.error( "Samples so far: " + StringUtils
                                .join( sampleDimensions.get( platform ).get( qType ), ',' ) );
                        throw new IllegalStateException(
                                "Validation failed at platform=" + platform + " designelement=" + designElement
                                        + " qType=" + qType + " expected " + numSamples + " values, got " + vals.size()
                                        + "; name(s) for qType are " + StringUtils.join( qtNames, "," ) );
                    }
                }
                if ( GeoValues.log.isDebugEnabled() )
                    GeoValues.log
                            .debug( qType + " ok on " + platform + ", all vectors have " + numSamples + " values" );
            }
        }

    }

    /**
     * Only needs to be called 'externally' if you know there is no data for the sample.
     *
     * @param sample                sample
     * @param quantitationTypeIndex QT index
     * @return geo platform
     */
    private GeoPlatform addSample( GeoSample sample, Integer quantitationTypeIndex ) {
        if ( sample.getPlatforms().size() > 1 ) {
            throw new IllegalArgumentException( sample + ": Can't handle samples that use multiple platforms" );
        }

        GeoPlatform platform = sample.getPlatforms().iterator().next();
        if ( !sampleDimensions.containsKey( platform ) ) {
            sampleDimensions.put( platform, new HashMap<>() );
        }

        Map<Integer, LinkedHashSet<GeoSample>> samplePlatformMap = sampleDimensions.get( platform );
        if ( !samplePlatformMap.containsKey( quantitationTypeIndex ) ) {
            samplePlatformMap.put( quantitationTypeIndex, new LinkedHashSet<>() );
        }

        LinkedHashSet<GeoSample> sampleQtMap = samplePlatformMap.get( quantitationTypeIndex );
        sampleQtMap.add( sample );
        return platform;
    }

}

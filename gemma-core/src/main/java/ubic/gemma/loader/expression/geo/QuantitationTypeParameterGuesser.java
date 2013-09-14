/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.loader.expression.geo;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.io.FileHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.model.GeoValues;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.util.Settings;

/**
 * Has the unpleasant task of figuring out what the quantitation type should look like, given a description and name
 * string.
 * 
 * @author Paul
 * @version $Id$
 * @see GeoValues for a list of Quantitation Type names which are skipped (so some of the below might not be needed)
 */
public class QuantitationTypeParameterGuesser {

    private static Log log = LogFactory.getLog( QuantitationTypeParameterGuesser.class.getName() );

    private static Set<String> measuredSignalDescPatterns = new HashSet<String>();
    private static Set<String> derivedSignalDescPatterns = new HashSet<String>();
    private static Set<String> ratioDescPatterns = new HashSet<String>();
    private static Set<String> measuredSignalNamePatterns = new HashSet<String>();
    private static Set<String> derivedSignalNamePatterns = new HashSet<String>();
    // private static Set<String> ratioStringNamePatterns = new HashSet<String>();

    private static Set<String> isNormalizedPatterns = new HashSet<String>();
    private static Set<String> isBackgroundSubtractedNamePatterns = new HashSet<String>();
    private static Set<String> isBackgroundSubtractedDescPatterns = new HashSet<String>();
    private static Set<String> isPreferredNamePatterns = new HashSet<String>();
    // private static Set<String> isPreferredDescPatterns = new HashSet<String>();

    private static Map<ScaleType, Set<String>> scaleDescPatterns = new HashMap<ScaleType, Set<String>>();
    private static Map<StandardQuantitationType, Set<String>> typeDescPatterns = new HashMap<StandardQuantitationType, Set<String>>();
    private static Map<PrimitiveType, Set<String>> representationDescPatterns = new HashMap<PrimitiveType, Set<String>>();
    private static Map<Boolean, Set<String>> isBackgroundDescPatterns = new HashMap<Boolean, Set<String>>();

    private static Map<ScaleType, Set<String>> scaleNamePatterns = new HashMap<ScaleType, Set<String>>();
    private static Map<StandardQuantitationType, Set<String>> typeNamePatterns = new HashMap<StandardQuantitationType, Set<String>>();
    private static Map<PrimitiveType, Set<String>> representationNamePatterns = new HashMap<PrimitiveType, Set<String>>();
    private static Map<Boolean, Set<String>> isBackgroundNamePatterns = new HashMap<Boolean, Set<String>>();
    private static Map<Boolean, Set<String>> isRatioNamePatterns = new HashMap<Boolean, Set<String>>();
    private static Map<Boolean, Set<String>> isRatioDescPatterns = new HashMap<Boolean, Set<String>>();

    static {
        CompositeConfiguration config = new CompositeConfiguration();

        String gemmaAppDataHome = Settings.getString( "gemma.appdata.home" );
        if ( StringUtils.isNotBlank( gemmaAppDataHome ) ) {
            try {
                PropertiesConfiguration pc = new PropertiesConfiguration();
                FileHandler handler = new FileHandler( pc );
                handler.setFileName( gemmaAppDataHome + File.separatorChar + "quantitationType.properties" );
                handler.load();
            } catch ( ConfigurationException e ) {
                log.info( "No custom quantitation type descriptors found" );
            }
        }

        measuredSignalDescPatterns.addAll( Arrays.asList( config.getStringArray( "measuredSignalPatterns" ) ) );
        derivedSignalDescPatterns.addAll( Arrays.asList( config.getStringArray( "derivedSignalPatterns" ) ) );

        ratioDescPatterns.addAll( Arrays.asList( config.getStringArray( "ratioStringPatterns" ) ) );

        measuredSignalDescPatterns.add( ".*channel[\\s_ ][12] (mean|median) (signal|intensity) (?!- background).*" );
        measuredSignalDescPatterns.add( ".*(red|green|cy5|cy3) (mean|median) (feature)? intensity.*" );
        measuredSignalDescPatterns.add( ".*(red|green|cy5|cy3) (mean|median) (feature)? intensity.*" );

        measuredSignalNamePatterns.add( ".*[rg]_?(mean|median).*?(?!sd)" );
        measuredSignalNamePatterns.add( ".*ch[12][ib]?_(mean|median|^sd).*?(?!sd).*" );
        measuredSignalNamePatterns.add( ".*ch[12]_(mean|bkg).*?(?!sd)" );
        measuredSignalNamePatterns.add( ".*channel [12] (mean|median) (signal|intensity).*" );
        measuredSignalNamePatterns.add( "[fb](635|532)[_\\s\\.](mean|median).*?(?!b(635|532))(?!sd)" );

        derivedSignalDescPatterns
                .add( ".*channel [12] (mean|median) signal background (subtracted|corrected).*(?!ratio).*" );
        derivedSignalNamePatterns.add( "ch[12][nd]_(mean|median).*(?!ratio).*" );

        derivedSignalNamePatterns.add( ".*channel[\\s_ ][12]\\s?(mean|median)?\\s?(signal|intensity) - background" );
        derivedSignalDescPatterns.add( ".*(?<!ratio).*(?<!un)normalized.*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*processed_signal" );
        derivedSignalDescPatterns.add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*relative abundance of a transcript.*" );
        derivedSignalDescPatterns.add( "mas(\\s+)?[56](\\.[0-9])? signal.*" );
        derivedSignalDescPatterns
                .add( ".*(?<!ratio).*background[\\s-](subtraction|substraction|subtracted|corrected).*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*processed.*" );
        derivedSignalNamePatterns.add( "pos[/_](neg|fraction).*" );
        derivedSignalDescPatterns.add( "sum_of_(mean|median)s" );
        derivedSignalNamePatterns.add( "^%.*" );
        derivedSignalDescPatterns.add( ".*\\s+\\s.*" );
        derivedSignalNamePatterns.add( "ch[12]_per_sat.*" );
        derivedSignalNamePatterns.add( "f(635|532)[_\\s\\.](mean|median)(\\s-\\s|_)b(635|532)" );
        derivedSignalNamePatterns.add( "pergtbch[12].*" );

        scaleDescPatterns.put( ScaleType.PERCENT, new HashSet<String>() );
        scaleDescPatterns.put( ScaleType.LINEAR, new HashSet<String>() );
        scaleDescPatterns.put( ScaleType.LOG2, new HashSet<String>() );
        scaleDescPatterns.put( ScaleType.LOG10, new HashSet<String>() );
        scaleDescPatterns.put( ScaleType.LOGBASEUNKNOWN, new HashSet<String>() );
        scaleDescPatterns.put( ScaleType.UNSCALED, new HashSet<String>() );

        scaleNamePatterns.put( ScaleType.PERCENT, new HashSet<String>() );
        scaleNamePatterns.put( ScaleType.LINEAR, new HashSet<String>() );
        scaleNamePatterns.put( ScaleType.LOG2, new HashSet<String>() );
        scaleNamePatterns.put( ScaleType.LOG10, new HashSet<String>() );
        scaleNamePatterns.put( ScaleType.LOGBASEUNKNOWN, new HashSet<String>() );
        scaleNamePatterns.put( ScaleType.UNSCALED, new HashSet<String>() );

        scaleDescPatterns.get( ScaleType.PERCENT ).add( "^(the\\s)?percent(age)?.*" );
        scaleDescPatterns.get( ScaleType.PERCENT ).add( "^%.*" );

        scaleDescPatterns.get( ScaleType.LOG2 ).add( ".*log( )?2.*" );
        scaleDescPatterns.get( ScaleType.LOG10 ).add( ".*log( )?10.*" );
        scaleDescPatterns.get( ScaleType.LOGBASEUNKNOWN ).add( ".*log( )?(?!(10|2)).*" );
        scaleDescPatterns.get( ScaleType.LOG2 ).add( "log (base 2)" );
        scaleDescPatterns.get( ScaleType.LOG2 ).add( "(gc?)rma(\\W.*)?" );
        scaleDescPatterns.get( ScaleType.LOG2 ).add( "mas(\\s)?[56](\\.[0-9])? signal.*" );

        scaleNamePatterns.get( ScaleType.PERCENT ).add( "^%.*" );
        scaleNamePatterns.get( ScaleType.PERCENT ).add( "pergtbch[12].*" );
        scaleNamePatterns.get( ScaleType.PERCENT ).add( "ch[12]_per_sat.*" );
        scaleNamePatterns.get( ScaleType.LOG2 ).add( ".*log( )?2.*" );
        scaleNamePatterns.get( ScaleType.LOG10 ).add( ".*log( )?10.*" );
        scaleNamePatterns.get( ScaleType.LOGBASEUNKNOWN ).add( ".*log( )?(?!(10|2)).*" );

        typeDescPatterns.put( StandardQuantitationType.PRESENTABSENT, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.AMOUNT, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.CONFIDENCEINDICATOR, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.COORDINATE, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.CORRELATION, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.OTHER, new HashSet<String>() );

        typeNamePatterns.put( StandardQuantitationType.PRESENTABSENT, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.AMOUNT, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.CONFIDENCEINDICATOR, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.COORDINATE, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.CORRELATION, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.OTHER, new HashSet<String>() );

        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "[rg]_(bg)?_?sd" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "[bf](532|635)[_\\s]sd" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "p_value" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "d_p-value" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "ch[12](_bkd)?b?n? ?_(\\s)?sd" );
        typeDescPatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add(
                ".*(mean|median|background) standard deviation.*" );
        typeDescPatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "standard deviation.*" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( ".*ch[12][_\\s]confidence.*" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( ".*ch[12][i][_\\s]sd" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( ".*det(ection)?[_\\s-]p(-value)?.*" );

        typeDescPatterns.get( StandardQuantitationType.AMOUNT ).addAll( measuredSignalDescPatterns );
        typeDescPatterns.get( StandardQuantitationType.AMOUNT ).addAll( derivedSignalDescPatterns );
        typeNamePatterns.get( StandardQuantitationType.AMOUNT ).addAll( measuredSignalNamePatterns );
        typeNamePatterns.get( StandardQuantitationType.AMOUNT ).addAll( derivedSignalNamePatterns );

        typeNamePatterns.get( StandardQuantitationType.PRESENTABSENT ).add( ".*(pre|abs)([ _])?call.*" );
        typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT ).add( ".*call.+present.*" );
        typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT ).add( ".*dchip detection call.*" );
        typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT ).add( "detection" );

        typeDescPatterns.get( StandardQuantitationType.CORRELATION ).add( ".*correlation.*" );

        typeNamePatterns.get( StandardQuantitationType.COORDINATE ).add(
                ".*(array_row|array_column|top|left|right|bot).*" );
        typeNamePatterns.get( StandardQuantitationType.COORDINATE ).add( "(x_coord|y_coord|x_location|y_location|x|y)" );
        typeNamePatterns.get( StandardQuantitationType.COORDINATE ).add( "(row|column)" );

        typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "^pairs.*" );
        typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "area" );
        typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "dia\\.?(meter)?" );
        typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "flags?" ); // FLAGS are now skipped
        typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "(m|p)m[_\\s]excess" );
        typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "negative" );
        typeDescPatterns.get( StandardQuantitationType.OTHER ).add(
                "number of pixels used to calculate a feature's intensity" ); // special case...

        representationDescPatterns.put( PrimitiveType.DOUBLE, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.INT, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.STRING, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.BOOLEAN, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.CHAR, new HashSet<String>() );

        representationNamePatterns.put( PrimitiveType.DOUBLE, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.INT, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.STRING, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.BOOLEAN, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.CHAR, new HashSet<String>() );

        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of (background\\s)?pixels.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of feature pixels.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of (positive )?probe pairs.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of probe set.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*(?<!positive/)pairs[_\\s]used.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( "number of (positive|negative) probe pairs" );
        representationNamePatterns.get( PrimitiveType.INT ).add( "pairs[_\\s]in[_\\s]?avg" );
        representationDescPatterns.get( PrimitiveType.INT ).add( "area" );
        representationDescPatterns.get( PrimitiveType.INT ).add( "b[\\s_]pixels" );
        representationDescPatterns.get( PrimitiveType.INT ).add(
                ".*(array_row|array_column|top|left(?!\\safter)|right|bot).*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*(x_coord|y_coord|x_location|y_location).*" );
        representationNamePatterns.get( PrimitiveType.STRING ).add( "abs([ _])?call" );
        representationNamePatterns.get( PrimitiveType.STRING ).add( "flag(s)?" );
        representationDescPatterns.get( PrimitiveType.DOUBLE ).add( ".*ratio.*" );

        isBackgroundDescPatterns.put( Boolean.FALSE, new HashSet<String>() );
        isBackgroundDescPatterns.put( Boolean.TRUE, new HashSet<String>() );

        isBackgroundNamePatterns.put( Boolean.FALSE, new HashSet<String>() );
        isBackgroundNamePatterns.put( Boolean.TRUE, new HashSet<String>() );

        isRatioNamePatterns.put( Boolean.FALSE, new HashSet<String>() );
        isRatioNamePatterns.put( Boolean.TRUE, new HashSet<String>() );
        isRatioDescPatterns.put( Boolean.FALSE, new HashSet<String>() );
        isRatioDescPatterns.put( Boolean.TRUE, new HashSet<String>() );

        isRatioNamePatterns.get( Boolean.TRUE ).add( "(pix_)?rat[12]n?_(mean|median)" );
        isRatioNamePatterns.get( Boolean.TRUE ).add( ".*\\(.+?/.+?\\).*" );
        isRatioDescPatterns.get( Boolean.TRUE ).add( ".*(fold[_\\s]change|ratio).*" );
        isRatioDescPatterns.get( Boolean.TRUE ).add( ".*test/reference.*" );
        isRatioDescPatterns.get( Boolean.TRUE ).add( ".*normch2/normch1.*" );
        isRatioDescPatterns.get( Boolean.TRUE ).add( ".*percent(age)?.*" );

        isBackgroundNamePatterns.get( Boolean.TRUE ).add( "ch[12]b.*" );
        isBackgroundDescPatterns
                .get( Boolean.TRUE )
                .add( ".*(?<!subtracted\\s(by\\s)?)(?<!over the\\s)((pixel|feature)\\s)?(background(\\s|\\sintensity|\\ssignal)?)(?!subtracted).*" );

        isNormalizedPatterns.add( ".*(?<!un)normalized.*" );
        isNormalizedPatterns.add( "ch[12](b)?n.*" );
        isNormalizedPatterns.add( "(unf_)?value" );
        isNormalizedPatterns.add( "rma" );
        isNormalizedPatterns.add( "dchip" );

        isBackgroundSubtractedDescPatterns.add( ".*(?<!ratio).*background[\\s-](subtracted|corrected).*(?!ratio).*" );
        isBackgroundSubtractedDescPatterns.add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        isBackgroundSubtractedDescPatterns.add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        isBackgroundSubtractedDescPatterns.add( ".*background intensity subtracted.*" );
        isBackgroundSubtractedNamePatterns.add( ".*- ch[12]_bkd" );
        isBackgroundSubtractedNamePatterns.add( "ch[12]d.*" );
        isBackgroundSubtractedNamePatterns.add( ".*((- )|_)b(532|635)" );
        isBackgroundSubtractedNamePatterns.add( ".*- background" );
        isBackgroundSubtractedNamePatterns.add( "rma" );
        isBackgroundSubtractedNamePatterns.add( "gcrma" );
        isBackgroundSubtractedNamePatterns.add( "dchip" );
        isBackgroundSubtractedDescPatterns
                .add( ".*channel [12] (mean|median) signal background (subtracted|corrected).*(?!ratio).*" );

        // note: unf_value is without the flagged values removed.
        isPreferredNamePatterns.add( "value" );
        isPreferredNamePatterns.add( "rma" );
        isPreferredNamePatterns.add( "gcrma" );
        isPreferredNamePatterns.add( "dchip" );
        isPreferredNamePatterns.add( "chpsignal" );
        isPreferredNamePatterns.add( "signal" );
        // isPreferredNamePatterns.add( ".*?log_rat2n_mean" );

    }

    public static void guessQuantitationTypeParameters( QuantitationType qt, String name, String description ) {
        guessQuantitationTypeParameters( qt, name, description, null );
    }

    /**
     * Attempt to fill in the details of the quantitation type.
     * 
     * @param qt QuantitationType to fill in details for.
     * @param namelc of the quantitation type from the GEO sample column
     * @param descriptionlc of the quantitation type from the GEO sample column
     * @param exampleValue to help conversion test whether the parameters match.
     */
    public static void guessQuantitationTypeParameters( QuantitationType qt, String name, String description,
            Object exampleValue ) {

        String namelc = name.toLowerCase();

        String descriptionlc = null;

        if ( description != null ) {
            descriptionlc = description.toLowerCase();
        } else {
            descriptionlc = "";
        }

        /*
         * Here are our default values.
         */
        GeneralType gType = GeneralType.QUANTITATIVE;
        ScaleType sType = ScaleType.LINEAR;
        StandardQuantitationType qType = StandardQuantitationType.OTHER;
        Boolean isBackground = Boolean.FALSE;
        PrimitiveType rType = PrimitiveType.DOUBLE;
        Boolean isBackgroundSubtracted = Boolean.FALSE;
        // Boolean isPreferred = Boolean.FALSE;
        Boolean isNormalized = Boolean.FALSE;
        Boolean isRatio = Boolean.FALSE;

        sType = guessScaleType( namelc, descriptionlc );
        qType = guessType( namelc, descriptionlc );
        rType = guessPrimitiveType( namelc, descriptionlc, exampleValue );

        isBackground = guessIsBackground( namelc, descriptionlc ) && maybeBackground( namelc, descriptionlc );
        isBackgroundSubtracted = isBackgroundSubtracted( namelc, descriptionlc );
        isNormalized = isNormalized( namelc, descriptionlc );
        isRatio = isRatio( namelc, descriptionlc );

        if ( qType.equals( StandardQuantitationType.AMOUNT ) || qType.equals( StandardQuantitationType.COUNT ) ) {
            gType = GeneralType.QUANTITATIVE;
        } else if ( qType.equals( StandardQuantitationType.PRESENTABSENT ) ) {
            gType = GeneralType.CATEGORICAL;
            sType = ScaleType.OTHER;
        } else if ( qType.equals( StandardQuantitationType.COORDINATE ) ) {
            rType = PrimitiveType.INT;
        }

        if ( name.contains( "Probe ID" ) || descriptionlc.equalsIgnoreCase( "Probe Set ID" )
                || name.equals( "experiment name" ) ) {
            /*
             * special case...not a quantitation type.
             */
            qType = StandardQuantitationType.OTHER;
            sType = ScaleType.UNSCALED;
            gType = GeneralType.CATEGORICAL;
        }

        if ( descriptionlc.contains( "qualitative" ) ) {
            gType = GeneralType.CATEGORICAL;
        }

        qt.setGeneralType( gType );

        if ( qt.getScale() == null ) qt.setScale( sType );

        if ( qt.getType() == null ) qt.setType( qType );

        if ( qt.getRepresentation() == null ) qt.setRepresentation( rType );

        if ( qt.getIsBackground() == null ) qt.setIsBackground( isBackground );

        if ( qt.getIsBackgroundSubtracted() == null ) qt.setIsBackgroundSubtracted( isBackgroundSubtracted );

        if ( qt.getIsNormalized() == null ) qt.setIsNormalized( isNormalized );

        if ( qt.getIsRatio() == null ) qt.setIsRatio( isRatio );

        if ( qt.getIsPreferred() == null ) qt.setIsPreferred( isPreferred( qt ) );

        if ( qt.getIsMaskedPreferred() == null ) qt.setIsMaskedPreferred( Boolean.FALSE );

    }

    /**
     * @param namelc
     * @param descriptionlc
     * @return
     */
    protected static Boolean guessIsBackground( String name, String description ) {
        for ( Boolean type : isBackgroundDescPatterns.keySet() ) {
            for ( String patt : isBackgroundDescPatterns.get( type ) ) {
                if ( description.matches( patt ) ) {
                    return type;
                }
            }
            for ( String patt : isBackgroundNamePatterns.get( type ) ) {
                if ( name.matches( patt ) ) {
                    return type;
                }
            }

        }
        return Boolean.FALSE;
    }

    protected static PrimitiveType guessPrimitiveType( String name, String description, Object exampleValue ) {

        String exampleString = null;
        boolean couldBeDouble = true;
        boolean couldBeInt = true;
        if ( exampleValue != null ) {
            if ( exampleValue instanceof Double ) {
                return PrimitiveType.DOUBLE;
            } else if ( exampleValue instanceof Integer ) {
                return PrimitiveType.INT;
            } else if ( exampleValue instanceof Boolean ) {
                return PrimitiveType.BOOLEAN;
            } else if ( exampleValue instanceof String ) {
                exampleString = ( String ) exampleValue;
            }

            /*
             * Goofy special case of 'null' thanks to bad decision by GEO data submitters. See bug 1760
             */
            if ( StringUtils.isNotBlank( exampleString ) && exampleString != null
                    && !exampleString.equalsIgnoreCase( "null" ) ) {

                try {
                    Double.parseDouble( exampleString );
                } catch ( NumberFormatException e ) {
                    couldBeDouble = false;
                }

                try {
                    Integer.parseInt( exampleString );
                } catch ( NumberFormatException e ) {
                    couldBeInt = false;
                }
            }
        }

        for ( PrimitiveType type : representationDescPatterns.keySet() ) {
            for ( String patt : representationNamePatterns.get( type ) ) {
                if ( name.matches( patt ) ) {
                    if ( type.equals( PrimitiveType.DOUBLE ) && !couldBeDouble ) {
                        continue; // cannot be double.
                    }
                    if ( type.equals( PrimitiveType.INT ) && !couldBeInt ) {
                        continue;
                    }

                    return type;
                }
            }
            for ( String patt : representationDescPatterns.get( type ) ) {
                if ( description.matches( patt ) ) {
                    if ( type.equals( PrimitiveType.DOUBLE ) && !couldBeDouble ) {
                        continue; // cannot be double.
                    }
                    if ( type.equals( PrimitiveType.INT ) && !couldBeInt ) {
                        continue;
                    }
                    return type;
                }
            }

        }

        if ( !couldBeDouble ) return PrimitiveType.STRING;

        return PrimitiveType.DOUBLE;

    }

    protected static ScaleType guessScaleType( String name, String description ) {
        for ( ScaleType type : scaleDescPatterns.keySet() ) {
            for ( String patt : scaleNamePatterns.get( type ) ) {
                if ( name.matches( patt ) ) {
                    log.debug( "!!!!!name=" + name + " matched " + patt );
                    return type;
                }
            }
            for ( String patt : scaleDescPatterns.get( type ) ) {
                if ( description.toLowerCase().matches( patt ) ) {
                    log.debug( "!!!!!description=" + description + " matched " + patt );
                    return type;
                }

            }

        }
        return ScaleType.LINEAR; // default
    }

    /**
     * @param name
     * @param description
     * @return
     */
    protected static StandardQuantitationType guessType( String name, String description ) {
        for ( StandardQuantitationType type : typeDescPatterns.keySet() ) {

            boolean isQuant = type == StandardQuantitationType.AMOUNT || type == StandardQuantitationType.COUNT;

            if ( isQuant && !maybeDerivedSignal( name ) ) {
                continue;
            } else if ( isQuant && !maybeMeasuredSignal( name ) ) {
                continue;
            }

            for ( String patt : typeNamePatterns.get( type ) ) {
                log.debug( "name=" + name + " test " + patt );
                if ( name.matches( patt ) ) {
                    // special case for derived signal
                    log.debug( "!!!!!name=" + name + " matched " + patt );
                    return type;
                }
            }

            for ( String patt : typeDescPatterns.get( type ) ) {
                log.debug( "description=" + description + " test " + patt );
                if ( description.matches( patt ) ) {
                    log.debug( "!!!!!description=" + description + " matched " + patt );
                    return type;
                }
            }

        }

        return StandardQuantitationType.AMOUNT; // default.

    }

    /**
     * @param name
     * @param description
     * @return
     */
    protected static boolean isBackgroundSubtracted( String name, String description ) {
        for ( String patt : isBackgroundSubtractedNamePatterns ) {
            log.debug( name + " test " + patt );
            if ( name.matches( patt ) ) {
                log.debug( "name=" + name + " <<<matched " + patt );
                return true;
            }
        }
        for ( String patt : isBackgroundSubtractedDescPatterns ) {
            log.debug( description + " test " + patt );
            if ( description.matches( patt ) ) {
                log.debug( description + " <<<matched " + patt );
                return true;
            }
        }
        return false;
    }

    /**
     * @param name
     * @param description
     * @return
     */
    protected static boolean isNormalized( String name, String description ) {
        for ( String patt : isNormalizedPatterns ) {
            if ( name.matches( patt ) ) {
                log.debug( "name=" + name + " matched " + patt );
                return true;
            }
        }
        for ( String patt : isNormalizedPatterns ) {
            if ( description.matches( patt ) ) {
                log.debug( description + " matched " + patt );
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a quantitation type is 'preferred'.
     * 
     * @param qt
     * @return
     */
    protected static boolean isPreferred( QuantitationType qt ) {
        assert qt != null;
        // if ( qt.getIsBackground() || !qt.getIsNormalized() ) {
        // log.info( qt + " is not normalized " );
        // return false; // definitely not
        // }
        String name = qt.getName().toLowerCase();
        for ( String patt : isPreferredNamePatterns ) {
            if ( name.matches( patt ) ) {
                log.debug( "name=" + name + " <<<matched " + patt );
                return true;
            }
        }

        return false;

    }

    protected static boolean isRatio( String name, String description ) {

        if ( !maybeRatio( name ) ) return false;

        for ( Boolean type : isRatioNamePatterns.keySet() ) {
            for ( String patt : isRatioNamePatterns.get( type ) ) {
                log.debug( name + " test " + patt );
                if ( name.matches( patt ) ) {
                    return type;
                }
            }
            for ( String patt : isRatioDescPatterns.get( type ) ) {
                log.debug( description + " test " + patt );
                if ( description.matches( patt ) ) {
                    return type;
                }
            }
        }

        return false;
    }

    /**
     * @param namelc
     * @param descriptionlc
     * @return
     */
    protected static boolean maybeBackground( String namelc, String descriptionlc ) {
        if ( descriptionlc.contains( "background over the background" ) ) {
            return false; // definitely not.
        } else if ( descriptionlc.contains( "above the background" ) ) {
            return false; // definitely not.
        } else if ( descriptionlc.contains( "background subtracted" ) ) {
            return false; // definitely not.
        } else if ( descriptionlc.contains( "background-corrected" ) ) {
            return false; // definitely not.
        } else if ( descriptionlc.matches( "subtracted by .*? background" ) ) {
            return false; // definitely not.
        } else if ( namelc.matches( ".*- b(532|635)" ) ) {
            return false; // definitely not.
        } else if ( namelc.matches( ".*- background" ) ) {
            return false; // definitely not.
        } else if ( namelc.matches( "f(532|635).*" ) ) {
            return false; // definitely not.
        }

        // this is really a 'maybe'.
        return true;
    }

    protected static boolean maybeDerivedSignal( String name ) {
        if ( name.matches( "(pix_)?rat(io)?.*" ) ) {
            return false;
        }
        return true;
    }

    protected static boolean maybeMeasuredSignal( String name ) {

        if ( name.matches( ".*[_\\s]sd" ) ) {
            return false;
        }
        if ( name.matches( ".*[_\\s]avg" ) ) {
            return false;
        }
        if ( name.matches( "(pix_)?(rat(io)?).*" ) ) {
            return false;
        }

        if ( name.matches( "detection" ) ) return false;

        if ( name.matches( "CHPDetection" ) ) return false;

        return true;
    }

    protected static boolean maybeRatio( String name ) {
        if ( name.matches( "(p|m)m[\\s_]excess" ) ) {
            return false;
        }
        return true;
    }
}

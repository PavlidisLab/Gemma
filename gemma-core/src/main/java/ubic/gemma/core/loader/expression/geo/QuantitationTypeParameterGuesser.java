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
package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.expression.geo.model.GeoValues;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.core.config.Settings;

import java.io.File;
import java.util.*;

/**
 * Has the unpleasant task of figuring out what the quantitation type should look like, given a description and name
 * string.
 *
 * @author Paul
 * @see GeoValues for a list of Quantitation Type names which are skipped (so some of the below might not be needed)
 */
@SuppressWarnings("WeakerAccess")
public class QuantitationTypeParameterGuesser {

    private static final Log log = LogFactory.getLog( QuantitationTypeParameterGuesser.class.getName() );

    private static final Set<String> measuredSignalDescPatterns = new HashSet<>();
    private static final Set<String> derivedSignalDescPatterns = new HashSet<>();
    private static final Set<String> measuredSignalNamePatterns = new HashSet<>();
    private static final Set<String> derivedSignalNamePatterns = new HashSet<>();

    private static final Set<String> isNormalizedPatterns = new HashSet<>();
    private static final Set<String> isBackgroundSubtractedNamePatterns = new HashSet<>();
    private static final Set<String> isBackgroundSubtractedDescPatterns = new HashSet<>();
    private static final Set<String> isPreferredNamePatterns = new HashSet<>();

    private static final Map<ScaleType, Set<String>> scaleDescPatterns = new HashMap<>();
    private static final Map<StandardQuantitationType, Set<String>> typeDescPatterns = new HashMap<>();
    private static final Map<PrimitiveType, Set<String>> representationDescPatterns = new HashMap<>();
    private static final Map<Boolean, Set<String>> isBackgroundDescPatterns = new HashMap<>();

    private static final Map<ScaleType, Set<String>> scaleNamePatterns = new HashMap<>();
    private static final Map<StandardQuantitationType, Set<String>> typeNamePatterns = new HashMap<>();
    private static final Map<PrimitiveType, Set<String>> representationNamePatterns = new HashMap<>();
    private static final Map<Boolean, Set<String>> isBackgroundNamePatterns = new HashMap<>();
    private static final Map<Boolean, Set<String>> isRatioNamePatterns = new HashMap<>();
    private static final Map<Boolean, Set<String>> isRatioDescPatterns = new HashMap<>();

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
                QuantitationTypeParameterGuesser.log.info( "No custom quantitation type descriptors found" );
            }
        }

        QuantitationTypeParameterGuesser.measuredSignalDescPatterns
                .addAll( Arrays.asList( config.getStringArray( "measuredSignalPatterns" ) ) );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns
                .addAll( Arrays.asList( config.getStringArray( "derivedSignalPatterns" ) ) );

        QuantitationTypeParameterGuesser.measuredSignalDescPatterns
                .add( ".*channel[\\s_ ][12] (mean|median) (signal|intensity) (?!- background).*" );
        QuantitationTypeParameterGuesser.measuredSignalDescPatterns
                .add( ".*(red|green|cy5|cy3) (mean|median) (feature)? intensity.*" );

        QuantitationTypeParameterGuesser.measuredSignalNamePatterns.add( ".*[rg]_?(mean|median).*?(?!sd)" );
        QuantitationTypeParameterGuesser.measuredSignalNamePatterns.add( ".*ch[12][ib]?_(mean|median|^sd).*?(?!sd).*" );
        QuantitationTypeParameterGuesser.measuredSignalNamePatterns.add( ".*ch[12]_(mean|bkg).*?(?!sd)" );
        QuantitationTypeParameterGuesser.measuredSignalNamePatterns
                .add( ".*channel [12] (mean|median) (signal|intensity).*" );
        QuantitationTypeParameterGuesser.measuredSignalNamePatterns
                .add( "[fb](635|532)[_\\s\\.](mean|median).*?(?!b(635|532))(?!sd)" );

        QuantitationTypeParameterGuesser.derivedSignalDescPatterns
                .add( ".*channel [12] (mean|median) signal background (subtracted|corrected).*(?!ratio).*" );
        QuantitationTypeParameterGuesser.derivedSignalNamePatterns.add( "ch[12][nd]_(mean|median).*(?!ratio).*" );

        QuantitationTypeParameterGuesser.derivedSignalNamePatterns
                .add( ".*channel[\\s_ ][12]\\s?(mean|median)?\\s?(signal|intensity) - background" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns
                .add( ".*(?<!ratio).*(?<!un)normalized.*(?!ratio).*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns.add( ".*processed_signal" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns
                .add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns.add( ".*relative abundance of a transcript.*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns.add( "mas(\\s+)?[56](\\.[0-9])? signal.*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns
                .add( ".*(?<!ratio).*background[\\s-](subtraction|substraction|subtracted|corrected).*(?!ratio).*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns.add( ".*processed.*" );
        QuantitationTypeParameterGuesser.derivedSignalNamePatterns.add( "pos[/_](neg|fraction).*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns.add( "sum_of_(mean|median)s" );
        QuantitationTypeParameterGuesser.derivedSignalNamePatterns.add( "^%.*" );
        QuantitationTypeParameterGuesser.derivedSignalDescPatterns.add( ".*\\s+\\s.*" );
        QuantitationTypeParameterGuesser.derivedSignalNamePatterns.add( "ch[12]_per_sat.*" );
        QuantitationTypeParameterGuesser.derivedSignalNamePatterns
                .add( "f(635|532)[_\\s\\.](mean|median)(\\s-\\s|_)b(635|532)" );
        QuantitationTypeParameterGuesser.derivedSignalNamePatterns.add( "pergtbch[12].*" );

        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.PERCENT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.LINEAR, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.LOG2, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.LOG10, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.LOGBASEUNKNOWN, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.UNSCALED, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleDescPatterns.put( ScaleType.LN, new HashSet<String>() );

        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.PERCENT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.LN, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.LINEAR, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.LOG2, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.LOG10, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.LOGBASEUNKNOWN, new HashSet<String>() );
        QuantitationTypeParameterGuesser.scaleNamePatterns.put( ScaleType.UNSCALED, new HashSet<String>() );

        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.PERCENT ).add( "^(the\\s)?percent(age)?.*" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.PERCENT ).add( "^%.*" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOG2 ).add( ".*log( )?2.*" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOG10 ).add( ".*log( )?10.*" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOGBASEUNKNOWN )
                .add( ".*log( )?(?!(10|2)).*" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOG2 ).add( "log (base 2)" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOG2 ).add( "(gc?)rma(\\W.*)?" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOG2 ).add( ".*?\\brma\\b.*?" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LOG2 )
                .add( "mas(\\s)?[56](\\.[0-9])? signal.*" );
        QuantitationTypeParameterGuesser.scaleDescPatterns.get( ScaleType.LN ).add( ".*?natural log.*" );

        QuantitationTypeParameterGuesser.scaleNamePatterns.get( ScaleType.PERCENT ).add( "^%.*" );
        QuantitationTypeParameterGuesser.scaleNamePatterns.get( ScaleType.PERCENT ).add( "pergtbch[12].*" );
        QuantitationTypeParameterGuesser.scaleNamePatterns.get( ScaleType.PERCENT ).add( "ch[12]_per_sat.*" );
        QuantitationTypeParameterGuesser.scaleNamePatterns.get( ScaleType.LOG2 ).add( ".*log( )?2.*" );
        QuantitationTypeParameterGuesser.scaleNamePatterns.get( ScaleType.LOG10 ).add( ".*log( )?10.*" );
        QuantitationTypeParameterGuesser.scaleNamePatterns.get( ScaleType.LOGBASEUNKNOWN )
                .add( ".*log( )?(?!(10|2)).*" );

        QuantitationTypeParameterGuesser.typeDescPatterns
                .put( StandardQuantitationType.PRESENTABSENT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeDescPatterns.put( StandardQuantitationType.AMOUNT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeDescPatterns
                .put( StandardQuantitationType.CONFIDENCEINDICATOR, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeDescPatterns
                .put( StandardQuantitationType.COORDINATE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeDescPatterns
                .put( StandardQuantitationType.CORRELATION, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeDescPatterns.put( StandardQuantitationType.OTHER, new HashSet<String>() );

        QuantitationTypeParameterGuesser.typeNamePatterns
                .put( StandardQuantitationType.PRESENTABSENT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeNamePatterns.put( StandardQuantitationType.AMOUNT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeNamePatterns
                .put( StandardQuantitationType.CONFIDENCEINDICATOR, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeNamePatterns
                .put( StandardQuantitationType.COORDINATE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeNamePatterns
                .put( StandardQuantitationType.CORRELATION, new HashSet<String>() );
        QuantitationTypeParameterGuesser.typeNamePatterns.put( StandardQuantitationType.OTHER, new HashSet<String>() );

        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( "[rg]_(bg)?_?sd" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( "[bf](532|635)[_\\s]sd" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( "p_value" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( "d_p-value" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( "ch[12](_bkd)?b?n? ?_(\\s)?sd" );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( ".*(mean|median|background) standard deviation.*" );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( "standard deviation.*" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( ".*ch[12][_\\s]confidence.*" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( ".*ch[12][i][_\\s]sd" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR )
                .add( ".*det(ection)?[_\\s-]p(-value)?.*" );

        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.AMOUNT )
                .addAll( QuantitationTypeParameterGuesser.measuredSignalDescPatterns );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.AMOUNT )
                .addAll( QuantitationTypeParameterGuesser.derivedSignalDescPatterns );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.AMOUNT )
                .addAll( QuantitationTypeParameterGuesser.measuredSignalNamePatterns );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.AMOUNT )
                .addAll( QuantitationTypeParameterGuesser.derivedSignalNamePatterns );

        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.PRESENTABSENT )
                .add( ".*(pre|abs)([ _])?call.*" );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT )
                .add( ".*call.+present.*" );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT )
                .add( ".*dchip detection call.*" );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT )
                .add( "detection" );

        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.CORRELATION )
                .add( ".*correlation.*" );

        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.COORDINATE )
                .add( ".*(array_row|array_column|top|left|right|bot).*" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.COORDINATE )
                .add( "(x_coord|y_coord|x_location|y_location|x|y)" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.COORDINATE )
                .add( "(row|column)" );

        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "^pairs.*" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "area" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.OTHER )
                .add( "dia\\.?(meter)?" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.OTHER )
                .add( "flags?" ); // FLAGS are now skipped
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.OTHER )
                .add( "(m|p)m[_\\s]excess" );
        QuantitationTypeParameterGuesser.typeNamePatterns.get( StandardQuantitationType.OTHER ).add( "negative" );
        QuantitationTypeParameterGuesser.typeDescPatterns.get( StandardQuantitationType.OTHER )
                .add( "number of pixels used to calculate a feature's intensity" ); // special case...

        QuantitationTypeParameterGuesser.representationDescPatterns.put( PrimitiveType.DOUBLE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationDescPatterns.put( PrimitiveType.INT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationDescPatterns.put( PrimitiveType.STRING, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationDescPatterns.put( PrimitiveType.BOOLEAN, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationDescPatterns.put( PrimitiveType.CHAR, new HashSet<String>() );

        QuantitationTypeParameterGuesser.representationNamePatterns.put( PrimitiveType.DOUBLE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationNamePatterns.put( PrimitiveType.INT, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationNamePatterns.put( PrimitiveType.STRING, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationNamePatterns.put( PrimitiveType.BOOLEAN, new HashSet<String>() );
        QuantitationTypeParameterGuesser.representationNamePatterns.put( PrimitiveType.CHAR, new HashSet<String>() );

        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*number of (background\\s)?pixels.*" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*number of feature pixels.*" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*number of (positive )?probe pairs.*" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*number of probe set.*" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*(?<!positive/)pairs[_\\s]used.*" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( "number of (positive|negative) probe pairs" );
        QuantitationTypeParameterGuesser.representationNamePatterns.get( PrimitiveType.INT )
                .add( "pairs[_\\s]in[_\\s]?avg" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT ).add( "area" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT ).add( "b[\\s_]pixels" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*(array_row|array_column|top|left(?!\\safter)|right|bot).*" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.INT )
                .add( ".*(x_coord|y_coord|x_location|y_location).*" );
        QuantitationTypeParameterGuesser.representationNamePatterns.get( PrimitiveType.STRING ).add( "abs([ _])?call" );
        QuantitationTypeParameterGuesser.representationNamePatterns.get( PrimitiveType.STRING ).add( "flag(s)?" );
        QuantitationTypeParameterGuesser.representationDescPatterns.get( PrimitiveType.DOUBLE ).add( ".*ratio.*" );

        QuantitationTypeParameterGuesser.isBackgroundDescPatterns.put( Boolean.FALSE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.isBackgroundDescPatterns.put( Boolean.TRUE, new HashSet<String>() );

        QuantitationTypeParameterGuesser.isBackgroundNamePatterns.put( Boolean.FALSE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.isBackgroundNamePatterns.put( Boolean.TRUE, new HashSet<String>() );

        QuantitationTypeParameterGuesser.isRatioNamePatterns.put( Boolean.FALSE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.isRatioNamePatterns.put( Boolean.TRUE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.isRatioDescPatterns.put( Boolean.FALSE, new HashSet<String>() );
        QuantitationTypeParameterGuesser.isRatioDescPatterns.put( Boolean.TRUE, new HashSet<String>() );

        QuantitationTypeParameterGuesser.isRatioNamePatterns.get( Boolean.TRUE )
                .add( "(pix_)?rat[12]n?_(mean|median)" );
        QuantitationTypeParameterGuesser.isRatioNamePatterns.get( Boolean.TRUE ).add( ".*\\(.+?/.+?\\).*" );
        QuantitationTypeParameterGuesser.isRatioDescPatterns.get( Boolean.TRUE ).add( ".*(fold[_\\s]change|ratio).*" );
        QuantitationTypeParameterGuesser.isRatioDescPatterns.get( Boolean.TRUE ).add( ".*test/reference.*" );
        QuantitationTypeParameterGuesser.isRatioDescPatterns.get( Boolean.TRUE ).add( ".*normch2/normch1.*" );
        QuantitationTypeParameterGuesser.isRatioDescPatterns.get( Boolean.TRUE ).add( ".*percent(age)?.*" );

        QuantitationTypeParameterGuesser.isBackgroundNamePatterns.get( Boolean.TRUE ).add( "ch[12]b.*" );
        QuantitationTypeParameterGuesser.isBackgroundDescPatterns.get( Boolean.TRUE )
                .add( ".*(?<!subtracted\\s(by\\s)?)(?<!over the\\s)((pixel|feature)\\s)?(background(\\s|\\sintensity|\\ssignal)?)(?!subtracted).*" );

        QuantitationTypeParameterGuesser.isNormalizedPatterns.add( ".*(?<!un)normalized.*" );
        QuantitationTypeParameterGuesser.isNormalizedPatterns.add( "ch[12](b)?n.*" );
        QuantitationTypeParameterGuesser.isNormalizedPatterns.add( "(unf_)?value" );
        QuantitationTypeParameterGuesser.isNormalizedPatterns.add( "rma" );
        QuantitationTypeParameterGuesser.isNormalizedPatterns.add( "dchip" );

        QuantitationTypeParameterGuesser.isBackgroundSubtractedDescPatterns
                .add( ".*(?<!ratio).*background[\\s-](subtracted|corrected).*(?!ratio).*" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedDescPatterns
                .add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedDescPatterns
                .add( ".*background intensity subtracted.*" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( ".*- ch[12]_bkd" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( "ch[12]d.*" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( ".*((- )|_)b(532|635)" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( ".*- background" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( "rma" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( "gcrma" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns.add( "dchip" );
        QuantitationTypeParameterGuesser.isBackgroundSubtractedDescPatterns
                .add( ".*channel [12] (mean|median) signal background (subtracted|corrected).*(?!ratio).*" );

        // note: unf_value is without the flagged values removed.
        QuantitationTypeParameterGuesser.isPreferredNamePatterns.add( "value" );
        QuantitationTypeParameterGuesser.isPreferredNamePatterns.add( "rma" );
        QuantitationTypeParameterGuesser.isPreferredNamePatterns.add( "gcrma" );
        QuantitationTypeParameterGuesser.isPreferredNamePatterns.add( "dchip" );
        QuantitationTypeParameterGuesser.isPreferredNamePatterns.add( "chpsignal" );
        QuantitationTypeParameterGuesser.isPreferredNamePatterns.add( "signal" );
        // isPreferredNamePatterns.add( ".*?log_rat2n_mean" );

    }

    public static void guessQuantitationTypeParameters( QuantitationType qt, String name, String description ) {
        QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( qt, name, description, null );
    }

    /**
     * Attempt to fill in the details of the quantitation type.
     *
     * @param qt           QuantitationType to fill in details for.
     * @param name         of the quantitation type from the GEO sample column
     * @param description  of the quantitation type from the GEO sample column
     * @param exampleValue to help conversion test whether the parameters match.
     */
    public static void guessQuantitationTypeParameters( QuantitationType qt, String name, String description,
            Object exampleValue ) {

        String namelc = name.toLowerCase();

        String descriptionlc;

        if ( description != null ) {
            descriptionlc = description.toLowerCase();
        } else {
            descriptionlc = "";
        }

        /*
         * Here are our default values.
         */
        GeneralType gType = GeneralType.QUANTITATIVE;
        ScaleType sType;
        StandardQuantitationType qType;
        Boolean isBackground;
        PrimitiveType rType;
        Boolean isBackgroundSubtracted;
        // Boolean isPreferred = Boolean.FALSE;
        Boolean isNormalized;
        Boolean isRatio;

        sType = QuantitationTypeParameterGuesser.guessScaleType( namelc, descriptionlc );
        qType = QuantitationTypeParameterGuesser.guessType( namelc, descriptionlc );
        rType = QuantitationTypeParameterGuesser.guessPrimitiveType( namelc, descriptionlc, exampleValue );

        isBackground = QuantitationTypeParameterGuesser.guessIsBackground( namelc, descriptionlc )
                && QuantitationTypeParameterGuesser.maybeBackground( namelc, descriptionlc );
        isBackgroundSubtracted = QuantitationTypeParameterGuesser.isBackgroundSubtracted( namelc, descriptionlc );
        isNormalized = QuantitationTypeParameterGuesser.isNormalized( namelc, descriptionlc );
        isRatio = QuantitationTypeParameterGuesser.isRatio( namelc, descriptionlc );

        if ( qType.equals( StandardQuantitationType.AMOUNT ) || qType.equals( StandardQuantitationType.COUNT ) ) {
            //noinspection DataFlowIssue
            gType = GeneralType.QUANTITATIVE;
        } else if ( qType.equals( StandardQuantitationType.PRESENTABSENT ) ) {
            gType = GeneralType.CATEGORICAL;
            sType = ScaleType.OTHER;
        } else if ( qType.equals( StandardQuantitationType.COORDINATE ) ) {
            rType = PrimitiveType.INT;
        }

        if ( name.contains( "Probe ID" ) || descriptionlc.equalsIgnoreCase( "Probe Set ID" ) || name
                .equals( "experiment name" ) ) {
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

        if ( qt.getScale() == null )
            qt.setScale( sType );

        if ( qt.getType() == null )
            qt.setType( qType );

        if ( qt.getRepresentation() == null )
            qt.setRepresentation( rType );

        qt.setIsBackground( isBackground );

        qt.setIsBackgroundSubtracted( isBackgroundSubtracted );

        qt.setIsNormalized( isNormalized );

        qt.setIsRatio( isRatio );

        qt.setIsPreferred( QuantitationTypeParameterGuesser.isPreferred( qt ) );

        qt.setIsMaskedPreferred( Boolean.FALSE );

    }

    protected static Boolean guessIsBackground( String name, String description ) {
        for ( Boolean type : QuantitationTypeParameterGuesser.isBackgroundDescPatterns.keySet() ) {
            for ( String patt : QuantitationTypeParameterGuesser.isBackgroundDescPatterns.get( type ) ) {
                if ( description.matches( patt ) ) {
                    return type;
                }
            }
            for ( String patt : QuantitationTypeParameterGuesser.isBackgroundNamePatterns.get( type ) ) {
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
            if ( StringUtils.isNotBlank( exampleString ) && !exampleString.equalsIgnoreCase( "null" ) ) {

                try {
                    //noinspection ResultOfMethodCallIgnored // ok, checking for exceptions
                    Double.parseDouble( exampleString );
                } catch ( NumberFormatException e ) {
                    couldBeDouble = false;
                }

                try {
                    //noinspection ResultOfMethodCallIgnored // ok, checking for exceptions
                    Integer.parseInt( exampleString );
                } catch ( NumberFormatException e ) {
                    couldBeInt = false;
                }
            }
        }

        for ( PrimitiveType type : QuantitationTypeParameterGuesser.representationDescPatterns.keySet() ) {
            PrimitiveType guessed;
            guessed = QuantitationTypeParameterGuesser.checkType( type, name, couldBeDouble, couldBeInt );
            if ( guessed != null )
                return guessed;
            guessed = QuantitationTypeParameterGuesser.checkType( type, description, couldBeDouble, couldBeInt );
            if ( guessed != null )
                return guessed;
        }

        if ( !couldBeDouble )
            return PrimitiveType.STRING;

        return PrimitiveType.DOUBLE;

    }

    protected static ScaleType guessScaleType( String name, String description ) {
        for ( ScaleType type : QuantitationTypeParameterGuesser.scaleDescPatterns.keySet() ) {
            for ( String patt : QuantitationTypeParameterGuesser.scaleNamePatterns.get( type ) ) {
                if ( name.matches( patt ) ) {
                    QuantitationTypeParameterGuesser.log.debug( "!!!!!name=" + name + " matched " + patt );
                    return type;
                }
            }
            for ( String patt : QuantitationTypeParameterGuesser.scaleDescPatterns.get( type ) ) {
                if ( description.toLowerCase().matches( patt ) ) {
                    QuantitationTypeParameterGuesser.log
                            .debug( "!!!!!description=" + description + " matched " + patt );
                    return type;
                }

            }

        }
        return ScaleType.LINEAR; // default
    }

    protected static StandardQuantitationType guessType( String name, String description ) {
        for ( StandardQuantitationType type : QuantitationTypeParameterGuesser.typeDescPatterns.keySet() ) {

            boolean isQuant = type == StandardQuantitationType.AMOUNT || type == StandardQuantitationType.COUNT;

            if ( isQuant && !QuantitationTypeParameterGuesser.maybeDerivedSignal( name ) ) {
                continue;
            } else if ( isQuant && !QuantitationTypeParameterGuesser.maybeMeasuredSignal( name ) ) {
                continue;
            }

            for ( String patt : QuantitationTypeParameterGuesser.typeNamePatterns.get( type ) ) {
                QuantitationTypeParameterGuesser.log.debug( "name=" + name + " test " + patt );
                if ( name.matches( patt ) ) {
                    // special case for derived signal
                    QuantitationTypeParameterGuesser.log.debug( "!!!!!name=" + name + " matched " + patt );
                    return type;
                }
            }

            for ( String patt : QuantitationTypeParameterGuesser.typeDescPatterns.get( type ) ) {
                QuantitationTypeParameterGuesser.log.debug( "description=" + description + " test " + patt );
                if ( description.matches( patt ) ) {
                    QuantitationTypeParameterGuesser.log
                            .debug( "!!!!!description=" + description + " matched " + patt );
                    return type;
                }
            }

        }

        return StandardQuantitationType.AMOUNT; // default.

    }

    protected static boolean isBackgroundSubtracted( String name, String description ) {
        for ( String patt : QuantitationTypeParameterGuesser.isBackgroundSubtractedNamePatterns ) {
            QuantitationTypeParameterGuesser.log.debug( name + " test " + patt );
            if ( name.matches( patt ) ) {
                QuantitationTypeParameterGuesser.log.debug( "name=" + name + " <<<matched " + patt );
                return true;
            }
        }
        for ( String patt : QuantitationTypeParameterGuesser.isBackgroundSubtractedDescPatterns ) {
            QuantitationTypeParameterGuesser.log.debug( description + " test " + patt );
            if ( description.matches( patt ) ) {
                QuantitationTypeParameterGuesser.log.debug( description + " <<<matched " + patt );
                return true;
            }
        }
        return false;
    }

    protected static boolean isNormalized( String name, String description ) {
        for ( String patt : QuantitationTypeParameterGuesser.isNormalizedPatterns ) {
            if ( name.matches( patt ) ) {
                QuantitationTypeParameterGuesser.log.debug( "name=" + name + " matched " + patt );
                return true;
            }
        }
        for ( String patt : QuantitationTypeParameterGuesser.isNormalizedPatterns ) {
            if ( description.matches( patt ) ) {
                QuantitationTypeParameterGuesser.log.debug( description + " matched " + patt );
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a quantitation type is 'preferred'.
     *
     * @param qt QT
     * @return is preferred
     */
    protected static boolean isPreferred( QuantitationType qt ) {
        assert qt != null;
        // if ( qt.getIsBackground() || !qt.getIsNormalized() ) {
        // log.info( qt + " is not normalized " );
        // return false; // definitely not
        // }
        String name = qt.getName().toLowerCase();
        for ( String patt : QuantitationTypeParameterGuesser.isPreferredNamePatterns ) {
            if ( name.matches( patt ) ) {
                QuantitationTypeParameterGuesser.log.debug( "name=" + name + " <<<matched " + patt );
                return true;
            }
        }

        return false;

    }

    protected static boolean isRatio( String name, String description ) {

        if ( !QuantitationTypeParameterGuesser.maybeRatio( name ) )
            return false;

        for ( Boolean type : QuantitationTypeParameterGuesser.isRatioNamePatterns.keySet() ) {
            for ( String patt : QuantitationTypeParameterGuesser.isRatioNamePatterns.get( type ) ) {
                QuantitationTypeParameterGuesser.log.debug( name + " test " + patt );
                if ( name.matches( patt ) ) {
                    return type;
                }
            }
            for ( String patt : QuantitationTypeParameterGuesser.isRatioDescPatterns.get( type ) ) {
                QuantitationTypeParameterGuesser.log.debug( description + " test " + patt );
                if ( description.matches( patt ) ) {
                    return type;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("SimplifiableIfStatement") // Better readability
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
        } else
            return !namelc.matches( "f(532|635).*" );

        // this is really a 'maybe'.
    }

    protected static boolean maybeDerivedSignal( String name ) {
        return !name.matches( "(pix_)?rat(io)?.*" );
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

        if ( name.matches( "detection" ) )
            return false;

        //noinspection RedundantIfStatement // Better for readability
        if ( name.matches( "CHPDetection" ) )
            return false;

        return true;
    }

    protected static boolean maybeRatio( String name ) {
        return !name.matches( "([pm])m[\\s_]excess" );
    }

    private static PrimitiveType checkType( PrimitiveType type, String value, boolean couldBeDouble,
            boolean couldBeInt ) {
        for ( String patt : QuantitationTypeParameterGuesser.representationDescPatterns.get( type ) ) {
            if ( value.matches( patt ) ) {
                if ( type.equals( PrimitiveType.DOUBLE ) && !couldBeDouble ) {
                    continue; // cannot be double.
                }
                if ( type.equals( PrimitiveType.INT ) && !couldBeInt ) {
                    continue;
                }
                return type;
            }
        }
        return null;
    }
}

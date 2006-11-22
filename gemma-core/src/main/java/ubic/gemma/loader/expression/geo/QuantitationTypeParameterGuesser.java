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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.util.ConfigUtils;

/**
 * Has the unpleasant task of figuring out what the quantitation type should look like, given a description and name
 * string.
 * 
 * @author Paul
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public class QuantitationTypeParameterGuesser {

    private static Log log = LogFactory.getLog( QuantitationTypeParameterGuesser.class.getName() );

    private static Set<String> measuredSignalDescPatterns = new HashSet<String>();
    private static Set<String> derivedSignalDescPatterns = new HashSet<String>();
    private static Set<String> ratioStringDescPatterns = new HashSet<String>();
    private static Set<String> measuredSignalNamePatterns = new HashSet<String>();
    private static Set<String> derivedSignalNamePatterns = new HashSet<String>();
    private static Set<String> ratioStringNamePatterns = new HashSet<String>();

    private static Set<String> isNormalizedPatterns = new HashSet<String>();
    private static Set<String> isBackgroundSubtractedPatterns = new HashSet<String>();
    private static Set<String> isPreferredPatterns = new HashSet<String>();

    private static Map<ScaleType, Set<String>> scaleDescPatterns = new HashMap<ScaleType, Set<String>>();
    private static Map<StandardQuantitationType, Set<String>> typeDescPatterns = new HashMap<StandardQuantitationType, Set<String>>();
    private static Map<PrimitiveType, Set<String>> representationDescPatterns = new HashMap<PrimitiveType, Set<String>>();
    private static Map<Boolean, Set<String>> isBackgroundDescPatterns = new HashMap<Boolean, Set<String>>();

    private static Map<ScaleType, Set<String>> scaleNamePatterns = new HashMap<ScaleType, Set<String>>();
    private static Map<StandardQuantitationType, Set<String>> typeNamePatterns = new HashMap<StandardQuantitationType, Set<String>>();
    private static Map<PrimitiveType, Set<String>> representationNamePatterns = new HashMap<PrimitiveType, Set<String>>();
    private static Map<Boolean, Set<String>> isBackgroundNamePatterns = new HashMap<Boolean, Set<String>>();

    static {
        CompositeConfiguration config = new CompositeConfiguration();

        String gemmaAppDataHome = ConfigUtils.getString( "gemma.appdata.home" );
        if ( StringUtils.isNotBlank( gemmaAppDataHome ) ) {
            try {
                config.addConfiguration( new PropertiesConfiguration( gemmaAppDataHome + File.separatorChar
                        + "quantitationType.properties" ) );
            } catch ( ConfigurationException e ) {
                log.warn( "No custom quantitation type descriptors found" );
            }
        }

        measuredSignalDescPatterns.addAll( config.getList( "measuredSignalPatterns" ) );
        derivedSignalDescPatterns.addAll( config.getList( "derivedSignalPatterns" ) );
        ratioStringDescPatterns.addAll( config.getList( "ratioStringPatterns" ) );

        measuredSignalDescPatterns.add( ".*channel [12] (mean|median) (signal|intensity).*" );
        measuredSignalDescPatterns.add( ".*(red|green|cy5|cy3) (mean|median) (feature)? intensity.*" );
        measuredSignalDescPatterns.add( ".*(red|green|cy5|cy3) (mean|median) (feature)? intensity.*" );

        measuredSignalNamePatterns.add( ".*[rg]_(mean|median).*?(?!sd)" );
        measuredSignalNamePatterns.add( ".*ch[12][ib]?_(mean|median|^sd).*?(?!sd).*" );
        measuredSignalNamePatterns.add( ".*ch[12]_(mean|bkg).*?(?!sd)" );
        measuredSignalNamePatterns.add( ".*channel [12] (mean|median) (signal|intensity).*" );

        derivedSignalDescPatterns
                .add( ".*channel [12] (mean|median) signal background (subtracted|corrected).*(?!ratio).*" );
        derivedSignalNamePatterns.add( "ch[12][nd]_(mean|median).*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*(?<!ratio).*(?<!un)normalized.*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*relative abundance of a transcript.*" );
        derivedSignalDescPatterns.add( "mas5 signal.*" );
        derivedSignalDescPatterns.add( ".*(?<!ratio).*background[\\s-](subtracted|corrected).*(?!ratio).*" );
        derivedSignalDescPatterns.add( ".*processed.*" );
        derivedSignalNamePatterns.add( "pos[/_](neg|fraction).*" );
        derivedSignalDescPatterns.add( "sum_of_(mean|median)s" );

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

        scaleNamePatterns.get( ScaleType.PERCENT ).add( "^%.*" );
        scaleNamePatterns.get( ScaleType.PERCENT ).add( "PERGTBCH[12].*" );
        scaleNamePatterns.get( ScaleType.LOG2 ).add( ".*log( )?2.*" );
        scaleNamePatterns.get( ScaleType.LOG10 ).add( ".*log( )?10.*" );
        scaleNamePatterns.get( ScaleType.LOGBASEUNKNOWN ).add( ".*log( )?(?!(10|2)).*" );

        typeDescPatterns.put( StandardQuantitationType.PRESENTABSENT, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.DERIVEDSIGNAL, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.MEASUREDSIGNAL, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.CONFIDENCEINDICATOR, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.RATIO, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.COORDINATE, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.CORRELATION, new HashSet<String>() );
        typeDescPatterns.put( StandardQuantitationType.OTHER, new HashSet<String>() );

        typeNamePatterns.put( StandardQuantitationType.PRESENTABSENT, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.DERIVEDSIGNAL, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.MEASUREDSIGNAL, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.CONFIDENCEINDICATOR, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.RATIO, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.COORDINATE, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.CORRELATION, new HashSet<String>() );
        typeNamePatterns.put( StandardQuantitationType.OTHER, new HashSet<String>() );

        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "[rg]_(bg)?_?sd" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "p_value" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "d_p-value" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "ch[12](_bkd)?b?n? ?_(\\s)?sd" );
        typeDescPatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add(
                ".*(mean|median|background) standard deviation.*" );
        typeDescPatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( "standard deviation.*" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( ".*ch[12][_\\s]confidence.*" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( ".*ch[12][i][_\\s]sd" );
        typeNamePatterns.get( StandardQuantitationType.CONFIDENCEINDICATOR ).add( ".*det(ection)?[_\\s-]p(-value)?.*" );

        typeDescPatterns.get( StandardQuantitationType.MEASUREDSIGNAL ).addAll( measuredSignalDescPatterns );
        typeDescPatterns.get( StandardQuantitationType.DERIVEDSIGNAL ).addAll( derivedSignalDescPatterns );
        typeNamePatterns.get( StandardQuantitationType.MEASUREDSIGNAL ).addAll( measuredSignalNamePatterns );
        typeNamePatterns.get( StandardQuantitationType.DERIVEDSIGNAL ).addAll( derivedSignalNamePatterns );

        typeNamePatterns.get( StandardQuantitationType.PRESENTABSENT ).add( ".*abs([ _])?call.*" );
        typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT ).add( ".*call.+present.*" );
        typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT ).add( ".*dchip detection call.*" );
        typeDescPatterns.get( StandardQuantitationType.PRESENTABSENT ).add( "detection" );

        typeDescPatterns.get( StandardQuantitationType.CORRELATION ).add( ".*correlation.*" );

        typeNamePatterns.get( StandardQuantitationType.RATIO ).add( "(pix_)?rat[12]n?_(mean|median).*" );
        typeDescPatterns.get( StandardQuantitationType.RATIO ).add( ".*(fold_change|ratio).*" );
        typeDescPatterns.get( StandardQuantitationType.RATIO ).add( ".*test/reference.*" );
        typeDescPatterns.get( StandardQuantitationType.RATIO ).add( ".*normch2/normch1.*" );

        typeDescPatterns.get( StandardQuantitationType.COORDINATE ).add(
                ".*(array_row|array_column|top|left|right|bot).*" );
        typeDescPatterns.get( StandardQuantitationType.COORDINATE ).add( ".*(x_coord|y_coord|x_location|y_location).*" );
        typeDescPatterns.get( StandardQuantitationType.COORDINATE ).add( "(row|column)" );

        typeDescPatterns.get( StandardQuantitationType.OTHER ).add( "^pairs.*" );
        typeDescPatterns.get( StandardQuantitationType.OTHER ).add( "area" );
        typeDescPatterns.get( StandardQuantitationType.OTHER ).add(
                "number of pixels used to calculate a feature's intensity" ); // special case...

        representationDescPatterns.put( PrimitiveType.DOUBLE, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.INT, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.STRING, new HashSet<String>() );
        representationDescPatterns.put( PrimitiveType.BOOLEAN, new HashSet<String>() );

        representationNamePatterns.put( PrimitiveType.DOUBLE, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.INT, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.STRING, new HashSet<String>() );
        representationNamePatterns.put( PrimitiveType.BOOLEAN, new HashSet<String>() );

        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of (background\\s)?pixels.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of feature pixels.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of (positive )?probe pairs.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*number of probe set.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*(?<!positive/)pairs[_\\s]used.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*pairs[_\\s]in[_\\s]?avg.*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( "area" );
        representationDescPatterns.get( PrimitiveType.INT ).add( "b[\\s_]pixels" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*(array_row|array_column|top|left|right|bot).*" );
        representationDescPatterns.get( PrimitiveType.INT ).add( ".*(x_coord|y_coord|x_location|y_location).*" );
        representationDescPatterns.get( PrimitiveType.STRING ).add( ".*abs([ _])?call.*" );

        isBackgroundDescPatterns.put( Boolean.FALSE, new HashSet<String>() );
        isBackgroundDescPatterns.put( Boolean.TRUE, new HashSet<String>() );

        isBackgroundNamePatterns.put( Boolean.FALSE, new HashSet<String>() );
        isBackgroundNamePatterns.put( Boolean.TRUE, new HashSet<String>() );

        isBackgroundDescPatterns
                .get( Boolean.TRUE )
                .add(
                        ".*(?<!subtracted\\s(by\\s)?)(?<!over the\\s)((pixel|feature)\\s)?(background(\\s|\\sintensity|\\ssignal)?)(?!subtracted).*" );

        isNormalizedPatterns.add( ".*(?<!un)normalized.*" );
        isBackgroundSubtractedPatterns.add( ".*(?<!ratio).*background[\\s-](subtracted|corrected).*(?!ratio).*" );
        isBackgroundSubtractedPatterns.add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        isBackgroundSubtractedPatterns.add( ".*(?<!ratio).*difference between.*(?!ratio).*" );
        isBackgroundSubtractedPatterns
                .add( ".*channel [12] (mean|median) signal background (subtracted|corrected).*(?!ratio).*" );

    }

    /**
     * @param qt
     * @return
     */
    protected static boolean isPreferred( QuantitationType qt ) {
        return !qt.getIsBackground() && qt.getIsNormalized() && qt.getIsBackgroundSubtracted();
    }

    protected static boolean isBackgroundSubtracted( String name, String description ) {
        for ( String patt : isBackgroundSubtractedPatterns ) {
            log.debug( name + " test " + patt );
            if ( name.matches( patt ) ) {
                log.debug( "name=" + name + " <<<matched " + patt );
                return true;
            }
        }
        for ( String patt : isNormalizedPatterns ) {
            log.debug( description + " test " + patt );
            if ( description.matches( patt ) ) {
                log.debug( description + " <<<matched " + patt );
                return true;
            }
        }
        return false;
    }

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
     * @param name
     * @param description
     * @return
     */
    protected static StandardQuantitationType guessType( String name, String description ) {
        for ( StandardQuantitationType type : typeDescPatterns.keySet() ) {
            for ( String patt : typeNamePatterns.get( type ) ) {
                log.debug( "name=" + name + " test " + patt );
                if ( name.matches( patt ) ) {
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

        return StandardQuantitationType.MEASUREDSIGNAL;

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
                if ( description.matches( patt ) ) {
                    log.debug( "!!!!!description=" + description + " matched " + patt );
                    return type;
                }

            }

        }
        return ScaleType.LINEAR; // default
    }

    protected static PrimitiveType guessPrimitiveType( String name, String description ) {
        for ( PrimitiveType type : representationDescPatterns.keySet() ) {
            for ( String patt : representationNamePatterns.get( type ) ) {
                if ( name.matches( patt ) ) return type;
            }
            for ( String patt : representationDescPatterns.get( type ) ) {
                if ( description.matches( patt ) ) return type;
            }

        }

        return PrimitiveType.DOUBLE;

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
        }

        // this is really a 'maybe'.
        return true;
    }

    /**
     * Attempt to fill in the details of the quantitation type.
     * 
     * @param qt QuantitationType to fill in details for.
     * @param namelc of the quantitation type from the GEO sample column
     * @param descriptionlc of the quantitation type from the GEO sample column
     */
    public static void guessQuantitationTypeParameters( QuantitationType qt, String name, String description ) {

        String namelc = name.toLowerCase();
        String descriptionlc = description.toLowerCase();

        /*
         * Here are our default values.
         */
        GeneralType gType = GeneralType.QUANTITATIVE;
        ScaleType sType = ScaleType.LINEAR;
        StandardQuantitationType qType = StandardQuantitationType.OTHER;
        Boolean isBackground = Boolean.FALSE;
        PrimitiveType rType = PrimitiveType.DOUBLE;

        sType = guessScaleType( namelc, descriptionlc );
        qType = guessType( namelc, descriptionlc );
        rType = guessPrimitiveType( namelc, descriptionlc );
        isBackground = guessIsBackground( namelc, descriptionlc ) && maybeBackground( namelc, descriptionlc );

        if ( qType.equals( StandardQuantitationType.MEASUREDSIGNAL ) ) {
            // rType = PrimitiveType.DOUBLE;
            gType = GeneralType.QUANTITATIVE;
        } else if ( qType.equals( StandardQuantitationType.PRESENTABSENT ) ) {
            gType = GeneralType.CATEGORICAL;
            sType = ScaleType.OTHER;
            // rType = PrimitiveType.STRING;
        } else if ( qType.equals( StandardQuantitationType.COORDINATE ) ) {
            rType = PrimitiveType.INT;
        }

        if ( name.contains( "Probe ID" ) || description.equalsIgnoreCase( "Probe Set ID" ) ) {
            /*
             * special case...not a quantitation type.
             */
            qType = StandardQuantitationType.OTHER;
            sType = ScaleType.UNSCALED;
            gType = GeneralType.CATEGORICAL;
        }

        qt.setGeneralType( gType );
        qt.setScale( sType );
        qt.setType( qType );
        qt.setRepresentation( rType );
        qt.setIsBackground( isBackground );

    }

}

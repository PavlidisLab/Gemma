/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.NamedMatrix;
import baseCode.dataStructure.matrix.ObjectMatrix2DNamed;
import baseCode.dataStructure.matrix.StringMatrix2DNamed;
import baseCode.util.StringUtil;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.loader.loaderutils.FileCombiningParser;

/**
 * Parse the raw files from MAGE-ML. The results are returned as a Collection of NamedMatrix's
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see baseCode.dataStructure.matrix.NamedMatrix;
 */
public class RawDataParser implements FileCombiningParser {
    /**
     * 
     */
    static final String NAN_STRING = Double.toString( Double.NaN );

    private static Log log = LogFactory.getLog( RawDataParser.class.getName() );

    private final List<BioAssay> bioAssays;

    private BioAssay currentBioAssay;

    private final BioAssayDimensions dimensions;

    // careful, this is basically a global variable. Make sure it gets reset when necessary.
    private int linesParsed = 0;

    private QuantitationTypeData qtData = new QuantitationTypeData();

    private Collection<Object> results = new LinkedHashSet<Object>();

    private char separator = '\t';

    /**
     * @param dimension
     * @param dimension2
     * @param dimensiom
     */
    public RawDataParser( List<BioAssay> bioAssays, BioAssayDimensions dimensions ) {
        super();
        this.bioAssays = bioAssays;
        this.dimensions = dimensions;
    }

    /**
     * @return Returns the qtData.
     */
    public QuantitationTypeData getQtData() {
        return this.qtData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#getResults()
     */
    public Collection<Object> getResults() {
        return results;
    }

    /**
     * Parse, if already set the BioAssays.
     * 
     * @throws IOException
     */
    public void parse() throws IOException {
        assert bioAssays != null;
        for ( BioAssay bioAssay : bioAssays ) {
            currentBioAssay = bioAssay;
            parse( bioAssay );
        }
        convertResultsToMatrices();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.FileCombiningParser#parse(java.util.List)
     */
    public void parse( List<File> files ) throws IOException {
        for ( File file : files ) {
            log.info( "Parsing " + file.getAbsolutePath() );
            parse( file );
        }

        convertResultsToMatrices();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        return StringUtil.splitPreserveAllTokens( line, this.separator );
    }

    /**
     * @param streams
     * @throws IOException
     */
    public void parseStreams( List<InputStream> streams ) throws IOException {
        int i = 0;
        for ( InputStream stream : streams ) {
            currentBioAssay = bioAssays.get( i );
            if ( currentBioAssay == null ) throw new IllegalStateException( "No " + i + "nth BioAssay" );
            this.parse( stream );
            i++;
        }

        convertResultsToMatrices();
    }

    /**
     * @param separator The separator to set.
     */
    public void setSeparator( char separator ) {
        this.separator = separator;
    }

    /**
     * Once done parsing a series of files, convert them into matrices.
     */
    private void convertResultsToMatrices() {
        Collection<QuantitationType> qts = qtData.getQuantitationTypes();
        for ( QuantitationType quantitationType : qts ) {
            convertResultToMatrix( quantitationType );
        }
    }

    /**
     * @param quantitationType
     */
    private void convertResultToMatrix( QuantitationType quantitationType ) {
        log.info( "Generating matrix for quantitation type: " + quantitationType.getName() );
        NamedMatrix matrix = qtData.getDataMatrix( quantitationType );
        results.add( matrix );
    }

    /**
     * Get the designElement for the current row being parsed.
     * 
     * @param hasDesignElements
     * @param designElements
     */
    private DesignElement getDesignElement( boolean hasDesignElements, List<DesignElement> designElements ) {
        DesignElement de;
        if ( hasDesignElements ) {
            if ( designElements.size() < linesParsed )
                throw new IllegalArgumentException(
                        "Number of lines in stream doesn't match number of DesignElements in the dimension. " );
            de = designElements.get( linesParsed );
            if ( de == null ) throw new NullPointerException( "DesignElement cannot be null" );
        } else {
            de = Reporter.Factory.newInstance();
            de.setName( "R_" + Integer.toString( linesParsed ) );
        }
        return de;
    }

    /**
     * @param bioAssay
     */
    private void parse( BioAssay bioAssay ) throws IOException {
        if ( bioAssay.getRawDataFile() == null )
            throw new IllegalArgumentException( "RawDataFile must not be null for BioAssay " + bioAssay );
        File f = bioAssay.getRawDataFile().asFile();
        if ( !f.canRead() ) throw new IOException( "Cannot read from " + f );
        parse( f );
    }

    /**
     * @param file
     * @throws IOException
     */
    private void parse( File f ) throws IOException {
        InputStream is = new FileInputStream( f );
        parse( is );
        is.close();
    }

    /**
     * This is the main parse method.
     * 
     * @param is
     * @throws IOException
     */
    private void parse( InputStream is ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        assert currentBioAssay != null;
        boolean hasDesignElements = dimensions.getDesignElementDimension( currentBioAssay ) != null
                && dimensions.getDesignElementDimension( currentBioAssay ).size() > 0;

        log.info( "Parsing 'raw' data for bioAssay: " + currentBioAssay );

        List<DesignElement> designElements = dimensions.getDesignElementDimension( currentBioAssay );
        List<QuantitationType> quantitationTypes = dimensions.getQuantitationTypeDimension( currentBioAssay );

        if ( hasDesignElements ) {
            qtData.setUp( quantitationTypes, designElements );
        } else {
            qtData.setUp( quantitationTypes );
        }

        if ( quantitationTypes == null )
            throw new NullPointerException( "Null quantitationTypeDimension for " + currentBioAssay );

        String line = null;
        linesParsed = 0;
        while ( ( line = br.readLine() ) != null ) {
            String[] fields = ( String[] ) parseOneLine( line );

            DesignElement de = getDesignElement( hasDesignElements, designElements );

            for ( int i = 0; i < fields.length; i++ ) {
                String field = fields[i];
                if ( StringUtils.isBlank( field ) ) field = NAN_STRING;
                QuantitationType qt = quantitationTypes.get( i );
                qtData.addData( qt, de, field );
            }

            linesParsed++;
            if ( linesParsed % ( PARSE_ALERT_FREQUENCY << 2 ) == 0 )
                log.info( "Read in " + linesParsed + " lines  for " + currentBioAssay.getName() + "..." );

        }
        log.info( "Read in " + linesParsed + " items..." );
    }

    /**
     * Encapsulates the relationship between QuantitationTypes, DesignElements and the Measurements (which will be
     * turned into DataVectors).
     */
    class QuantitationTypeData {

        // Note we specifiy the use of LinkedHashMap to ensure order is predictable. We use string instead of the
        // entities because their hashcodes are not zero.
        LinkedHashMap<String, LinkedHashMap<String, List<Object>>> dataMap = new LinkedHashMap<String, LinkedHashMap<String, List<Object>>>();

        Map<String, DesignElement> designElementsForNames = new HashMap<String, DesignElement>();
        Map<String, QuantitationType> quantitationTypesForNames = new HashMap<String, QuantitationType>();

        /**
         * Supports Boolean, String, Double and Integer data.
         * 
         * @param qt
         * @param de
         * @param data
         */
        public void addData( QuantitationType qt, DesignElement de, String data ) {

            if ( qt == null || de == null )
                throw new IllegalArgumentException( "Quantitation type and design element cannot be null" );
            if ( StringUtils.isBlank( data ) )
                throw new IllegalArgumentException( "Data cannot be blank for " + qt.getName() + ", " + de.getName() );

            PrimitiveType pt = qt.getRepresentation();
            String qtName = qt.getName();
            String deName = de.getName();

            if ( !dataMap.get( qtName ).containsKey( deName ) ) {
                dataMap.get( qtName ).put( deName, new ArrayList<Object>() );
            }
            List<Object> list = dataMap.get( qtName ).get( deName );

            try {
                if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                    if ( data.equals( NAN_STRING ) )
                        list.add( Boolean.FALSE );
                    else
                        list.add( new Boolean( data ) );
                } else if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                    if ( data.equals( NAN_STRING ) )
                        list.add( new Double( Double.NaN ) );
                    else
                        list.add( new Double( data ) );
                } else if ( pt.equals( PrimitiveType.INT ) ) {
                    if ( data.equals( NAN_STRING ) )
                        list.add( new Integer( 0 ) );
                    else
                        list.add( new Integer( data ) );
                } else {
                    if ( data.equals( NAN_STRING ) )
                        list.add( "" );
                    else
                        list.add( data );
                }
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( e );
            }

        }

        /**
         * @param qt
         * @return
         */
        public LinkedHashMap<String, List<Object>> getDataForQuantitationType( QuantitationType qt ) {
            return dataMap.get( qt );
        }

        /**
         * @param qts
         * @return
         */
        @SuppressWarnings("synthetic-access")
        public NamedMatrix getDataMatrix( QuantitationType qt ) {
            Collection<String> designElementNames = dataMap.get( qt.getName() ).keySet();
            int numDesignElements = designElementNames.size();
            int numBioAssays = bioAssays.size();

            log.info( "Creating " + numDesignElements + " x " + numBioAssays + " matrix for " + qt.getName() );

            String qtName = qt.getName();
            NamedMatrix result = new ObjectMatrix2DNamed( numDesignElements, numBioAssays );
            LinkedHashMap<String, List<Object>> quantitationTypeDesignElements = dataMap.get( qtName );

            int column = 0;
            for ( BioAssay ba : bioAssays ) {
                result.addColumnName( ba.getName(), column );
                column++;
            }

            int row = 0;
            for ( String deName : designElementNames ) {
                result.addRowName( deName, row );
                column = 0;
                for ( Object obj : quantitationTypeDesignElements.get( deName ) ) {
                    if ( obj == null ) {
                        throw new RuntimeException( "Got null value for row " + row + ", column " + column
                                + ", reporter: " + deName + " quantitation type: " + qtName );
                    }
                    result.set( row, column, obj );
                    column++;
                }
                row++;
            }
            return result;
        }

        /**
         * @param qt
         * @return
         */
        public List<DesignElement> getDesignElementsForQuantitationType( QuantitationType qt ) {
            List<DesignElement> result = new ArrayList<DesignElement>();

            for ( String deName : dataMap.get( qt.getName() ).keySet() ) {
                result.add( designElementsForNames.get( deName ) );
            }
            return result;

        }

        /**
         * @param n
         * @return
         */
        public QuantitationType getNthQuantitationType( int n ) {
            return getQuantitationTypes().get( n );
        }

        /**
         * @return
         */
        public List<QuantitationType> getQuantitationTypes() {
            List<QuantitationType> result = new ArrayList<QuantitationType>();
            for ( String qtName : dataMap.keySet() ) {
                result.add( quantitationTypesForNames.get( qtName ) );
            }
            return result;
        }

        /**
         * Use this method if you don't know the design elements ahead of time (slower).
         * 
         * @param quantitationTypes
         */
        public void setUp( List<QuantitationType> quantitationTypes ) {
            for ( QuantitationType type : quantitationTypes ) {
                quantitationTypesForNames.put( type.getName(), type );
                if ( !dataMap.containsKey( type.getName() ) )
                    dataMap.put( type.getName(), new LinkedHashMap<String, List<Object>>() );
            }
        }

        /**
         * This must be called before any processing can be completed.
         * 
         * @param quantitationTypes
         * @param designElements
         */
        public void setUp( List<QuantitationType> quantitationTypes, List<DesignElement> designElements ) {
            for ( DesignElement element : designElements ) {
                designElementsForNames.put( element.getName(), element );
            }
            for ( QuantitationType type : quantitationTypes ) {
                quantitationTypesForNames.put( type.getName(), type );
                if ( !dataMap.containsKey( type.getName() ) )
                    dataMap.put( type.getName(), new LinkedHashMap<String, List<Object>>() );

                LinkedHashMap<String, List<Object>> map = dataMap.get( type.getName() );
                for ( DesignElement element : designElements ) {
                    if ( !map.containsKey( element.getName() ) ) {
                        map.put( element.getName(), new ArrayList<Object>() );
                    }
                }
            }
        }
    }

}

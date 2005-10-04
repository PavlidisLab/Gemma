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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.NamedMatrix;
import baseCode.dataStructure.matrix.StringMatrix2DNamed;
import baseCode.util.StringUtil;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;
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
    private static Log log = LogFactory.getLog( RawDataParser.class.getName() );

    private final List<BioAssay> bioAssays;
    private BioAssay currentBioAssay;

    private final BioAssayDimensions dimensions;

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

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#getResults()
     */
    public Collection<Object> getResults() {
        return results;
    }

    /**
     * @throws IOException
     */
    public void parse() throws IOException {
        assert bioAssays != null;
        for ( BioAssay bioAssay : bioAssays ) {
            currentBioAssay = bioAssay;
            parse( bioAssay );
        }

        Collection<QuantitationType> qts = qtData.getQuantitationTypes();
        for ( QuantitationType quantitationType : qts ) {
            NamedMatrix matrix = qtData.getDataMatrix( quantitationType );
            results.add( matrix );
        }

    }

    /**
     * @param file
     * @throws IOException
     */
    public void parse( File f ) throws IOException {
        InputStream is = new FileInputStream( f );
        parse( is );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.io.InputStream)
     */
    public void parse( InputStream is ) throws IOException {
        linesParsed = 0;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String line = null;

        while ( ( line = br.readLine() ) != null ) {
            String[] fields = ( String[] ) parseOneLine( line );

            DesignElement de = dimensions.getDesignElementDimension( currentBioAssay ).get( linesParsed );

            for ( int i = 0; i < fields.length; i++ ) {
                String field = fields[i];
                QuantitationType qt = dimensions.getQuantitationTypeDimension( currentBioAssay ).get( i );
                qtData.addData( qt, de, field );
            }

            linesParsed++;
            if ( linesParsed % PARSE_ALERT_FREQUENCY == 0 ) log.debug( "Read in " + linesParsed + " items..." );

        }
        log.info( "Read in " + linesParsed + " items..." );
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
    }

    /**
     * 
     */
    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        try {
            parse( new FileInputStream( new File( filename ) ) );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        String[] fields = StringUtil.splitPreserveAllTokens( line, this.separator );
        return fields;
    }

    /**
     * @param streams
     * @throws IOException
     */
    public void parseStreams( List<InputStream> streams ) throws IOException {
        for ( InputStream stream : streams ) {
            this.parse( stream );
        }
    }

    /**
     * @param separator The separator to set.
     */
    public void setSeparator( char separator ) {
        this.separator = separator;
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

    class QuantitationTypeData {

        // Note we specifiy the use of LinkedHashMap to ensure order is predictable.
        LinkedHashMap<QuantitationType, LinkedHashMap<DesignElement, List<Object>>> dataMap = new LinkedHashMap<QuantitationType, LinkedHashMap<DesignElement, List<Object>>>();

        /**
         * Supports Boolean, String, Double and Integer data.
         * 
         * @param qt
         * @param de
         * @param data
         */
        public void addData( QuantitationType qt, DesignElement de, String data ) {
            PrimitiveType pt = qt.getRepresentation();

            if ( !dataMap.containsKey( qt ) ) {
                dataMap.put( qt, new LinkedHashMap<DesignElement, List<Object>>() );
            }

            if ( !dataMap.get( qt ).containsKey( de ) ) {
                dataMap.get( qt ).put( de, new ArrayList<Object>() );
            }

            try {
                if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                    dataMap.get( qt ).get( de ).add( new Boolean( data ) );
                } else if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                    dataMap.get( qt ).get( de ).add( new Double( data ) );
                } else if ( pt.equals( PrimitiveType.INT ) ) {
                    dataMap.get( qt ).get( de ).add( new Integer( data ) );
                } else {
                    dataMap.get( qt ).get( de ).add( data );
                }
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( e );
            }

        }

        /**
         * 
         * @param qt
         * @return
         */
        public LinkedHashMap<DesignElement, List<Object>> getDataForQuantitationType( QuantitationType qt ) {
            return dataMap.get( qt );
        }

        /**
         * @param qts
         * @return
         */
        @SuppressWarnings("synthetic-access")
        public NamedMatrix getDataMatrix( QuantitationType qt ) {
            int numDesignElements = dataMap.get( qt ).keySet().size();
            assert bioAssays.size() == dataMap.get( qt ).keySet().size();
            int numBioAssays = bioAssays.size();

            NamedMatrix result = new StringMatrix2DNamed( numDesignElements, numBioAssays );

            int i = 0;
            for ( DesignElement de : dataMap.get( qt ).keySet() ) {
                result.addRowName( de.getName(), i );
                i++;

                int j = 0;
                for ( Object obj : dataMap.get( qt ).get( de ) ) {
                    result.set( i, j, obj );
                    j++;
                }

            }

            int j = 0;
            for ( BioAssay ba : bioAssays ) {
                result.addColumnName( ba.getName(), j );
                j++;
            }
            return result;

        }

        /**
         * @return
         */
        public List<QuantitationType> getQuantitationTypes() {
            return new ArrayList<QuantitationType>( dataMap.keySet() );
        }

        /**
         * @param n
         * @return
         */
        public QuantitationType getNthQuantitationType( int n ) {
            return ( new ArrayList<QuantitationType>( dataMap.keySet() ) ).get( n );
        }

        /**
         * @param qt
         * @return
         */
        public List<DesignElement> getDesignElementsForQuantitationType( QuantitationType qt ) {
            return new ArrayList<DesignElement>( dataMap.get( qt ).keySet() );
        }
    }

    /**
     * @return Returns the qtData.
     */
    public QuantitationTypeData getQtData() {
        return this.qtData;
    }

}

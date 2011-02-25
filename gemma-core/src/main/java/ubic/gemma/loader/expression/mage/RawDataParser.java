/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.expression.mage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.parser.FileCombiningParser;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Parse the raw files from ArrayExpress (MAGE-ML). The format for the raw files is to have no column or row labels.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see baseCode.dataStructure.matrix.NamedMatrix;
 */
public class RawDataParser implements FileCombiningParser {
    private static Log log = LogFactory.getLog( RawDataParser.class.getName() );

    /**
     * 
     */
    static final String NAN_STRING = Double.toString( Double.NaN );

    List<BioAssay> bioAssays;

    private BioAssay currentBioAssay;

    private BioAssayDimensions dimensions = null;

    // careful, this is basically a global variable. Make sure it gets reset when necessary.
    private int linesParsed = 0;

    private QuantitationTypeData qtData = new QuantitationTypeData();

    private Collection<Object> results = new LinkedHashSet<Object>();

    private ExpressionExperiment expressionExperiment;

    private int selector = -1;

    private char separator = '\t';

    /**
     * 
     */
    public RawDataParser() {
    }

    /**
     * @param dimension
     * @param dimension2
     * @param dimensiom
     */
    public RawDataParser( List<BioAssay> bioAssays, BioAssayDimensions dimensions ) {
        super();
        if ( bioAssays == null || bioAssays.size() == 0 || dimensions == null )
            throw new IllegalArgumentException( "Null parameters." );
        this.bioAssays = bioAssays;
        this.dimensions = dimensions;
        //        
        // if ( bioAssays.iterator().next().getArrayDesignsUsed() == null )
        // throw new IllegalArgumentException( "arrayDesignsUsed must be set" );
        //        
        // if ( bioAssays.iterator().next().getArrayDesignsUsed().size() > 1 )
        // log.warn("Warning!! More than one array design for these bioAssays. ")
        //        
        // arrayDesignUsed = ( ArrayDesign ) bioAssays.iterator().next().getArrayDesignsUsed().iterator().next();
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
     * @see ubic.gemma.loader.loaderutils.Parser#getResults()
     */
    public Collection<Object> getResults() {
        return results;
    }

    /**
     * @return Returns the selector.
     */
    public int getSelector() {
        return this.selector;
    }

    /**
     * Parse, if already set the BioAssays.
     * 
     * @throws IOException
     */
    public void parse() throws IOException {
        assert bioAssays != null;
        int i = 0;
        for ( BioAssay bioAssay : bioAssays ) {
            currentBioAssay = bioAssay;
            log.info( "Parsing " + bioAssay.getName() + " (" + ( i + 1 ) + " of " + bioAssays.size() + ")" );
            parse( bioAssay );
            i++;
        }
        convertResultsToMatrices();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.FileCombiningParser#parse(java.util.List)
     */
    public void parse( List<File> files ) throws IOException {
        int i = 1;
        for ( File file : files ) {
            log.info( "Parsing " + file.getAbsolutePath() + " (" + i + " of " + files.size() + ")" );
            parse( file );
            i++;
        }

        convertResultsToMatrices();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        return StringUtils.splitPreserveAllTokens( line, this.separator );
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
            log.info( "Parsing " + currentBioAssay.getName() + " (" + ( i + 1 ) + " of " + bioAssays.size() + ")" );
            this.parse( stream );
            i++;
        }

        convertResultsToMatrices();
    }

    /**
     * @param bioAssays The bioAssays to set.
     */
    public void setBioAssays( List<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    /**
     * @param dimensions The dimensions to set.
     */
    public void setDimensions( BioAssayDimensions dimensions ) {
        this.dimensions = dimensions;
    }

    /**
     * @param selector The selector to set.
     */
    public void setSelector( int selector ) {
        this.selector = selector;
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
        int i = 0;
        for ( QuantitationType quantitationType : qts ) {
            if ( selector >= 0 && i != selector ) {
                i++;
                continue;
            }
            convertResultToMatrix( quantitationType );
            i++;
        }
    }

    /**
     * @param quantitationType
     */
    private void convertResultToMatrix( QuantitationType quantitationType ) {
        log.info( "Generating matrix for quantitation type: " + quantitationType.getName() );
        RawDataMatrix matrix = qtData.getDataForQuantitationType( quantitationType );
        log.info( "...done" );
        results.add( matrix );
    }

    /**
     * Get the designElement for the current row being parsed.
     * 
     * @param hasDesignElements
     * @param designElements
     */
    private CompositeSequence getDesignElement( boolean hasDesignElements, List<CompositeSequence> designElements ) {
        CompositeSequence de;
        if ( hasDesignElements ) {
            if ( designElements.size() < linesParsed )
                throw new IllegalArgumentException(
                        "Number of lines in stream doesn't match number of DesignElements in the dimension. " );
            de = designElements.get( linesParsed );
            if ( de == null ) throw new NullPointerException( "DesignElement cannot be null" );
        } else {
            throw new UnsupportedOperationException( "Cannot handle reporter-level data" );
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
        assert this.expressionExperiment != null;

        boolean hasDesignElements = dimensions.getDesignElementDimension( currentBioAssay ) != null
                && dimensions.getDesignElementDimension( currentBioAssay ).size() > 0;

        log.info( "Parsing 'raw' data for bioAssay: " + currentBioAssay );

        List<CompositeSequence> designElements = dimensions.getDesignElementDimension( currentBioAssay );
        List<QuantitationType> quantitationTypes = dimensions.getQuantitationTypeDimension( currentBioAssay );

        if ( quantitationTypes == null )
            throw new NullPointerException( "Null quantitationTypeDimension for " + currentBioAssay );

        qtData.setUp( quantitationTypes, designElements );

        String line = null;
        linesParsed = 0;
        while ( ( line = br.readLine() ) != null ) {
            String[] fields = ( String[] ) parseOneLine( line );

            CompositeSequence de = getDesignElement( hasDesignElements, designElements );

            if ( selector >= 0 ) {
                if ( selector >= fields.length ) throw new NoMoreQuantitationTypesException();
                String field = fields[selector];
                if ( StringUtils.isBlank( field ) ) field = NAN_STRING;
                QuantitationType qt = quantitationTypes.get( selector );
                qtData.addData( qt, de, this.expressionExperiment, field );
            } else {
                for ( int i = 0; i < fields.length; i++ ) {
                    String field = fields[i];
                    if ( StringUtils.isBlank( field ) ) field = NAN_STRING;
                    QuantitationType qt = quantitationTypes.get( i );
                    qtData.addData( qt, de, this.expressionExperiment, field );
                }
            }

            linesParsed++;
            if ( linesParsed % ( PARSE_ALERT_FREQUENCY << 4 ) == 0 )
                log.info( "Read in " + linesParsed + " lines  for " + currentBioAssay.getName() + "..." );

        }
        log.info( "Read in " + linesParsed + " items..." );
    }

    /**
     * Encapsulates the relationship between QuantitationTypes, DesignElements and the Measurements (which will be
     * turned into DataVectors).
     */
    class QuantitationTypeData {

        // we specify LinkedHashMaps so things come out in predictable orders.
        LinkedHashMap<String, RawDataMatrix> dataMatrices = new LinkedHashMap<String, RawDataMatrix>();
        LinkedHashMap<String, CompositeSequence> designElementsForNames = new LinkedHashMap<String, CompositeSequence>();
        LinkedHashMap<String, QuantitationType> quantitationTypesForNames = new LinkedHashMap<String, QuantitationType>();

        /**
         * 
         */
        public QuantitationTypeData() {
            // must call set up.
        }

        public QuantitationTypeData( List<QuantitationType> quantitationTypes, List<CompositeSequence> designElements ) {
            setUp( quantitationTypes, designElements );
        }

        /**
         * Supports Boolean, String, Double and Integer data.
         * 
         * @param qt
         * @param de
         * @param data
         */
        public void addData( QuantitationType qt, CompositeSequence de, ExpressionExperiment exp, String data ) {

            String qtName = qt.getName();

            RawDataMatrix matrixToAddTo = dataMatrices.get( qtName );
            designElementsForNames.put( de.getName(), de );
            PrimitiveType pt = qt.getRepresentation();
            try {
                if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                    if ( data.equals( NAN_STRING ) )
                        matrixToAddTo.addDataToRow( de, qt, exp, Boolean.FALSE );
                    else
                        matrixToAddTo.addDataToRow( de, qt, exp, Boolean.parseBoolean( data ) );
                } else if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                    if ( data.equals( NAN_STRING ) )
                        matrixToAddTo.addDataToRow( de, qt, exp, Double.NaN );
                    else
                        matrixToAddTo.addDataToRow( de, qt, exp, Double.parseDouble( data ) );
                } else if ( pt.equals( PrimitiveType.INT ) ) {
                    if ( data.equals( NAN_STRING ) )
                        matrixToAddTo.addDataToRow( de, qt, exp, 0 );
                    else
                        matrixToAddTo.addDataToRow( de, qt, exp, Integer.parseInt( data ) );
                } else {
                    if ( data.equals( NAN_STRING ) )
                        matrixToAddTo.addDataToRow( de, qt, exp, "" );
                    else
                        matrixToAddTo.addDataToRow( de, qt, exp, data );
                }
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( e );
            }

        }

        /**
         * @param qt
         * @return
         */
        public RawDataMatrix getDataForQuantitationType( QuantitationType qt ) {
            return this.dataMatrices.get( qt.getName() );
        }

        /**
         * @param qt
         * @return
         */
        public List<CompositeSequence> getDesignElementsForQuantitationType( QuantitationType qt ) {
            List<CompositeSequence> result = new ArrayList<CompositeSequence>();

            for ( String deName : dataMatrices.get( qt.getName() ).rows.keySet() ) {
                CompositeSequence de = designElementsForNames.get( deName );
                if ( de == null ) {
                    throw new IllegalStateException( "Null design element for " + deName );
                }
                result.add( de );
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
            for ( String qtName : dataMatrices.keySet() ) {
                result.add( quantitationTypesForNames.get( qtName ) );
            }
            return result;
        }

        /**
         * @param quantitationTypes
         */
        void setUp( List<QuantitationType> quantitationTypes, List<CompositeSequence> designElements ) {
            for ( QuantitationType type : quantitationTypes ) {
                String qtName = type.getName();
                if ( !dataMatrices.containsKey( qtName ) ) {
                    dataMatrices.put( qtName, new RawDataMatrix( bioAssays, type ) );
                }

                if ( !quantitationTypesForNames.containsKey( qtName ) ) {
                    quantitationTypesForNames.put( qtName, type );
                }
            }

            for ( CompositeSequence de : designElements ) {
                designElementsForNames.put( de.getName(), de );
            }

        }

    }

    /**
     * @param expressionExperiment The expressionExperiment to set.
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

}

class NoMoreQuantitationTypesException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1520081075629386288L;
}

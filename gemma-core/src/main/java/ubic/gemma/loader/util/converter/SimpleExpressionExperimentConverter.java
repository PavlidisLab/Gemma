package ubic.gemma.loader.util.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A simple converter that prepares an expression experiment from a collection of data values. Useful if expression data
 * values are parsed from a tab delimited file.
 * 
 * @author keshav
 * @version $Id$
 */
public class SimpleExpressionExperimentConverter {
    private Log log = LogFactory.getLog( SimpleExpressionExperimentConverter.class );

    private ExpressionExperiment expressionExperiment = null;

    private ByteArrayConverter bArrayConverter = new ByteArrayConverter();

    private String[] header = null;

    private String[] rowNames = null;

    /**
     * Prepare the expression experiment. Gather basic information that is needed for the gemma system.
     * 
     * @param name
     * @param description
     * @param ownerName
     * @param ownerDescription
     */
    public SimpleExpressionExperimentConverter( String name, String description, String ownerName,
            String ownerDescription ) {
        expressionExperiment = ExpressionExperiment.Factory.newInstance();
        expressionExperiment.setName( name );
        expressionExperiment.setDescription( description );

        Contact expressionExperimentOwner = Contact.Factory.newInstance();
        expressionExperimentOwner.setName( ownerName );
        expressionExperimentOwner.setDescription( ownerDescription );
        expressionExperiment.setOwner( expressionExperimentOwner );

    }

    /**
     * Accepts a raw data set which doesn't have a header.
     * 
     * @param dataCollection
     * @return Expression Experiment;
     */
    public ExpressionExperiment convert( Collection<String[]> dataCollection ) {

        this.convert( dataCollection, false, false );

        return expressionExperiment;
    }

    /**
     * @param dataCollection
     * @param headerExists
     * @param placeHolder A place holder exists in header
     * @return ExpressionExperiment
     */
    public ExpressionExperiment convert( Collection<String[]> dataCollection, boolean headerExists, boolean placeHolder ) {
        log.debug( "converting data to gemma objects" );

        Collection<String[]> rawDataCollection = prepareData( dataCollection, headerExists, placeHolder );

        // TODO add support for this back in
        // Collection<BioAssay> assays = convertBioAssays( header, placeHolder );
        // Collection<DesignElementDataVector> vectors = convertDesignElementDataVectors( rawDataCollection );
        // assays = convertArrayDesign( rawData, assays );

        // expressionExperiment.setDesignElementDataVectors( vectors );
        // expressionExperiment.setBioAssays( assays );

        return expressionExperiment;
    }

    /**
     * @param dataCollection
     * @param headerExists
     * @return Collection<String>
     */
    public Collection<String[]> prepareData( Collection<String[]> dataCollection, boolean headerExists,
            boolean placeHolder ) {

        if ( headerExists ) dataCollection = prepareHeader( dataCollection, placeHolder );

        Collection<String[]> rawDataCollection = prepareRawData( dataCollection );

        log.info( "Raw data contains: " + rawDataCollection.size() + " rows." );
        return rawDataCollection;
    }

    /**
     * Store and strip out the header.
     * 
     * @param dataCollection
     * @param headerExists
     */
    private Collection<String[]> prepareHeader( Collection<String[]> dataCollection, boolean placeHolder ) {

        /* not using generics. easier to get header with iterator */
        Iterator iter = dataCollection.iterator();

        /* Deal with place holder, like 'probe' in top left corner. */
        String[] headerWithPlaceHolder = ( String[] ) iter.next();
        if ( placeHolder ) {
            header = new String[headerWithPlaceHolder.length - 1];
            for ( int i = 0; i < header.length; i++ ) {
                header[i] = headerWithPlaceHolder[i + 1];
            }
        } else {
            header = headerWithPlaceHolder;
        }

        /*
         * Note that we remove the headerWithPlaceHolder ... this is because this String[] in the collection contains
         * the extra element, like 'probe'
         */
        log.info( "size before header removal: " + dataCollection.size() );
        dataCollection.remove( headerWithPlaceHolder );

        log.info( "size after header removal: " + dataCollection.size() );

        log.info( "Column names are: " );
        for ( int i = 0; i < header.length; i++ ) {
            log.info( header[i] );
        }

        return dataCollection;
    }

    /**
     * Store and strip out the column names.
     * 
     * @param dataCol
     */
    private Collection<String[]> prepareRawData( Collection<String[]> dataCol ) {
        // FIXME this will be slow.

        Collection<String[]> rawDataCol = new ArrayList();
        rowNames = new String[dataCol.size()];
        int i = 0;
        log.info( "Row names are: " );
        for ( String[] row : dataCol ) {
            if ( StringUtils.isEmpty( row[0] ) ) {
                row[0] = null;
            }
            rowNames[i] = row[0];
            log.info( rowNames[i] );

            /* Strip out names, and put raw data in a raw data set. */
            String[] rawData = new String[row.length - 1];
            for ( int j = 0; j < rawData.length; j++ ) {
                rawData[j] = row[j + 1];
            }
            rawDataCol.add( rawData );
            i++;
        }
        return rawDataCol;
    }

    /**
     * @param rawDataCollection Converts the collection of data to a 2D double array.
     * @return double[][]
     */
    public double[][] convertRawData( Collection<String[]> rawDataCollection ) {
        if ( rawDataCollection == null ) throw new RuntimeException( "Data set is null." );
        double[][] data2D = new double[rawDataCollection.size()][];
        int i = 0;
        for ( String[] data : rawDataCollection ) {

            double[] ddata = new double[data.length];

            for ( int j = 0; j < ddata.length; j++ ) {
                try {
                    ddata[j] = new Double( data[j] );
                } catch ( NumberFormatException e ) {
                    log.error( data[j] + " is not a Double." );
                    return null; // FIXME deal with this and skip.
                }
                log.info( ddata[j] );
            }
            data2D[i] = ddata;
            i++;
        }
        return data2D;
    }

    // /**
    // * @param names
    // * @param placeHolder A place holder exists in header
    // * @return Collection<BioAssay>
    // */
    // private Collection<BioAssay> convertBioAssays( String[] names, boolean placeHolder ) {
    //
    // List<BioAssay> assays = new ArrayList();
    // BioAssay assay = null;
    // int i = 0;
    // // discards initial value like 'probe'
    // if ( placeHolder ) {
    // log.info( "place holder exists." );
    // i = 1;
    // }
    // while ( i < names.length ) {
    // assay = BioAssay.Factory.newInstance();
    // assay.setName( names[i] );
    //
    // log.debug( assay.getName() );
    // assays.add( assay );
    // i++;
    // }
    //
    // log.info( names.length + " bioassays." );
    // return assays;
    // }
    //
    // /**
    // * @param dataCollection
    // * @return Collection<DesignElementDataVector
    // */
    // private Collection<DesignElementDataVector> convertDesignElementDataVectors( Collection<String[]> dataCollection
    // ) {
    // List<DesignElementDataVector> vectors = new ArrayList<DesignElementDataVector>();
    //
    // double[][] data2D = convertRawData( dataCollection );
    //
    // for ( int i = 0; i < data2D.length; i++ ) {
    // byte[] bdata = bArrayConverter.doubleArrayToBytes( data2D[i] );
    //
    // DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
    // vector.setData( bdata );
    // vectors.add( vector );
    //
    // log.debug( "Row converted" );
    // }
    //
    // return vectors;
    // }
    //
    // /**
    // * @param dataCollection
    // * @param bioAssays
    // * @return Collection<BioAssay>
    // */
    // private Collection<BioAssay> convertArrayDesign( Collection<String[]> dataCollection, Collection<BioAssay>
    // bioAssays ) {
    //
    // List<BioAssay> updatedBioAssays = new ArrayList();
    // for ( BioAssay assay : bioAssays ) {
    // ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
    // arrayDesign.setName( "Custom " + assay.getName() );
    // arrayDesign.setDescription( "Custom array design from bioassay " + assay.getName() );
    //
    // List<CompositeSequence> csCol = new ArrayList<CompositeSequence>();
    // for ( String[] data : dataCollection ) {
    // CompositeSequence cs = CompositeSequence.Factory.newInstance();
    // cs.setName( data[0] );
    // cs.setDescription( "probe set " + data[0] );
    //
    // // cs.setDesignElementDataVectors( vectors );
    //
    // /* TODO: Step 3. This also either exists or is user supplied. For now I am creating this. */
    // BioSequence sequence = BioSequence.Factory.newInstance();
    // sequence.setSequence( RandomStringUtils.random( 40, "ATCG" ) );
    // cs.setBiologicalCharacteristic( sequence );
    //
    // cs.setArrayDesign( arrayDesign );
    //
    // csCol.add( cs );
    //
    // }
    //
    // arrayDesign.setCompositeSequences( csCol );
    //
    // updatedBioAssays.add( assay );
    // }
    //
    // return updatedBioAssays;
    // }

    /**
     * @return Returns the header.
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * @return Returns the rowNames.
     */
    public String[] getRowNames() {
        return rowNames;
    }
}

/**
 * 
 */
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Command Line tools for loading the expression experiment in flat files
 * 
 * @author xiangwan
 */
/**
 * @author xiangwan
 */
public class LoadSimpleExpressionDataCli extends AbstractSpringAwareCLI {

    private String fileName = null;
    private String dirName = "./";
    private SimpleExpressionDataLoaderService eeLoaderService = null;

    final static String SPLITCHAR = "|";
    final static int NAMEI = 0;
    final static int DESCRIPTIONI = NAMEI + 1;
    final static int ARRAYDESIGNI = DESCRIPTIONI + 1;
    final static int SPECIESI = ARRAYDESIGNI + 1;
    final static int DATAFILEI = SPECIESI + 1;
    final static int QNAMEI = DATAFILEI + 1;
    final static int QDESCRIPTIONI = QNAMEI + 1;
    final static int QTYPEI = QDESCRIPTIONI + 1;
    final static int QSCALEI = QTYPEI + 1;
    final static int TOTALFIELDS = QSCALEI + 1;
    final static String ALLQTYPE[] =
    {
        StandardQuantitationType.PRESENTABSENT.toString(),
        StandardQuantitationType.RATIO.toString(),
        StandardQuantitationType.FAILED.toString(),
        StandardQuantitationType.DERIVEDSIGNAL.toString(),
        StandardQuantitationType.CONFIDENCEINDICATOR.toString(),
        StandardQuantitationType.EXPECTEDVALUE.toString(),
        StandardQuantitationType.ERROR.toString(),
        StandardQuantitationType.CORRELATION.toString(),
        StandardQuantitationType.ODDS.toString(),
        StandardQuantitationType.ODDSRATIO.toString(),
        StandardQuantitationType.MEASUREDSIGNAL.toString(),
        StandardQuantitationType.COORDINATE.toString(),
        StandardQuantitationType.TIME.toString(),
        StandardQuantitationType.DURATION.toString(),
        StandardQuantitationType.OTHER.toString()
    };
    final static String ALLSCALETYPE[] =
    {
        ScaleType.LINEAR.toString(),
        ScaleType.LN.toString(),
        ScaleType.LOG2.toString(),
        ScaleType.LOG10.toString(),
        ScaleType.LOGBASEUNKNOWN.toString(),
        ScaleType.FOLDCHANGE.toString(),
        ScaleType.OTHER.toString(),
        ScaleType.UNSCALED.toString(),
        ScaleType.FRACTION.toString(),
        ScaleType.PERCENT.toString()
    };

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
        Option fileOption = OptionBuilder.isRequired().hasArg().withArgName( "File Name" ).withDescription(
                "the list of experiments in flat file" ).withLongOpt( "file" ).create( 'f' );
        addOption( fileOption );

        Option dirOption = OptionBuilder.hasArg().withArgName( "File Folder" ).withDescription(
                "The folder for containing the experiment files" ).withLongOpt( "dir" ).create( 'd' );
        addOption( dirOption );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            fileName = getOptionValue( 'f' );
        }

        if ( hasOption( 'd' ) ) {
            dirName = getOptionValue( 'd' );
        }
    }

    private boolean loadExperiment( String conf ) throws Exception {
        int i = 0;
        String oneLoad[] = StringUtils.split( SPLITCHAR );
        if ( oneLoad.length != TOTALFIELDS ) {
            log.info( "Field Missing Got[" + oneLoad.length + "]: " + conf );
            return false;
        }
        for(i = 0; i < oneLoad.length; i++)
            oneLoad[i] = StringUtils.trim( oneLoad[i] );
        
        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        metaData.setName( oneLoad[NAMEI] );
        metaData.setDescription( oneLoad[DESCRIPTIONI] );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( oneLoad[ARRAYDESIGNI] );
        metaData.setArrayDesign( ad );
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( oneLoad[SPECIESI] );
        metaData.setTaxon( taxon );

        InputStream data = this.getClass().getResourceAsStream( this.dirName + oneLoad[DATAFILEI] );

        metaData.setQuantitationTypeName( oneLoad[QNAMEI] );
        metaData.setQuantitationTypeDescription( oneLoad[QDESCRIPTIONI] );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );

        for(i = 0; i < ALLQTYPE.length; i++)
            if(ALLQTYPE[i].equalsIgnoreCase( oneLoad[QTYPEI] ))
                break;
        if(i == ALLQTYPE.length){
            log.info( "Quantitivate Type " + oneLoad[QTYPEI] + " is not defined" );
            return false;
        }
        StandardQuantitationType sQType = StandardQuantitationType.OTHER;
        switch(i)
        {
            case 0:
                sQType = StandardQuantitationType.PRESENTABSENT;break;
            case 1:
                sQType = StandardQuantitationType.RATIO;break;
            case 2:
                sQType = StandardQuantitationType.FAILED;break;
            case 3:
                sQType = StandardQuantitationType.DERIVEDSIGNAL;break;
            case 4:
                sQType = StandardQuantitationType.CONFIDENCEINDICATOR;break;
            case 5:
                sQType = StandardQuantitationType.EXPECTEDVALUE;break;
            case 6:
                sQType = StandardQuantitationType.ERROR;break;
            case 7:
                sQType = StandardQuantitationType.CORRELATION;break;
            case 8:
                sQType = StandardQuantitationType.ODDS;break;
            case 9:
                sQType = StandardQuantitationType.ODDSRATIO;break;
            case 10:
                sQType = StandardQuantitationType.MEASUREDSIGNAL;break;
            case 11:
                sQType = StandardQuantitationType.COORDINATE;break;
            case 12:
                sQType = StandardQuantitationType.TIME;break;
            case 13:
                sQType = StandardQuantitationType.DURATION;break;
            case 14:
                sQType = StandardQuantitationType.OTHER;break;

        }
        metaData.setType( sQType );
            
        ScaleType sType = ScaleType.OTHER;
        for(i = 0; i < ALLSCALETYPE.length; i++)
            if(ALLSCALETYPE[i].equalsIgnoreCase( oneLoad[QSCALEI] ))
                break;
        if(i == ALLSCALETYPE.length){
            log.info( "Quantitivate Scale Type " + oneLoad[QSCALEI] + " is not defined" );
            return false;
        }
        switch(i)
        {
            case 0:
                sType = ScaleType.LINEAR;break;
            case 1:
                sType = ScaleType.LN;break;
            case 2:
                sType = ScaleType.LOG2;break;
            case 3:
                sType = ScaleType.LOG10;break;
            case 4:
                sType = ScaleType.LOGBASEUNKNOWN;break;
            case 5:
                sType = ScaleType.FOLDCHANGE;break;
            case 6:
                sType = ScaleType.OTHER;break;
            case 7:
                sType = ScaleType.UNSCALED;break;
            case 8:
                sType = ScaleType.FRACTION;break;
            case 9:
                sType = ScaleType.PERCENT;break;
        }
        metaData.setScale( sType );

        ExpressionExperiment ee = eeLoaderService.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        Exception err = processCommandLine( "Expression Data loader", args );
        if ( err != null ) {
            return err;
        }
        try {
            this.eeLoaderService = ( SimpleExpressionDataLoaderService ) this.getBean( "simpleExpressionDataLoaderService" );
            if ( this.fileName != null ) {
                log.info( "Loading experiments from " + this.fileName );
                InputStream is = new FileInputStream( new File( this.dirName, this.fileName ) );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

                String conf = null;
                while ( ( conf = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( conf ) ) {
                        continue;
                    }
                    /** *****Comments in the list file**** */
                    if ( StringUtils.trim( conf ).charAt( 0 ) == '#' ) continue;

                    try {
                        if ( this.loadExperiment( conf ) )
                            log.info( "Successfully Load " + conf );
                        else
                            log.error( "No experiments loaded!" );
                    } catch ( Exception e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        LoadSimpleExpressionDataCli p = new LoadSimpleExpressionDataCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}

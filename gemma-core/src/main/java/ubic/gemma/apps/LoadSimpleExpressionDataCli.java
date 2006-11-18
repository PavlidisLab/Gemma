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

package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

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
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Command Line tools for loading the expression experiment in flat files
 * 
 * @author xiangwan
 * @version $Id$
 */
public class LoadSimpleExpressionDataCli extends AbstractSpringAwareCLI {

    private String fileName = null;
    private String dirName = "./";
    private SimpleExpressionDataLoaderService eeLoaderService = null;
    private ArrayDesignService adService = null;

    final static String SPLITCHAR = "\t{1}";
    final static int NAMEI = 0;
    final static int DESCRIPTIONI = NAMEI + 1;
    final static int ARRAYDESIGNI = DESCRIPTIONI + 1;
    final static int SPECIESI = ARRAYDESIGNI + 1;
    final static int DATAFILEI = SPECIESI + 1;
    final static int QNAMEI = DATAFILEI + 1;
    final static int QDESCRIPTIONI = QNAMEI + 1;
    final static int QTYPEI = QDESCRIPTIONI + 1;
    final static int QSCALEI = QTYPEI + 1;
    final static int IMAGECLONEI = QSCALEI + 1;
    final static int TOTALFIELDS = IMAGECLONEI + 1;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
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
        // String oneLoad[] = StringUtils.split( conf,SPLITCHAR );
        String oneLoad[] = conf.split( SPLITCHAR );
        if ( oneLoad.length != TOTALFIELDS ) {
            log.info( "Field Missing Got[" + oneLoad.length + "]: " + conf );
            return false;
        }
        for ( i = 0; i < oneLoad.length; i++ )
            oneLoad[i] = StringUtils.trim( oneLoad[i] );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        metaData.setName( oneLoad[NAMEI] );
        metaData.setDescription( oneLoad[DESCRIPTIONI] );
        ArrayDesign ad = adService.findByShortName( oneLoad[ARRAYDESIGNI] );
        if ( ad == null ) {
            log.info( "Array Design " + oneLoad[ARRAYDESIGNI] + " is not loaded" );
            return false;
        }

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );

        // ArrayDesign.Factory.newInstance();
        // ad.setName( oneLoad[ARRAYDESIGNI] );
        metaData.setArrayDesigns( ads );
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( oneLoad[SPECIESI] );
        metaData.setTaxon( taxon );

        // InputStream data = this.getClass().getResourceAsStream( this.dirName + oneLoad[DATAFILEI] );
        InputStream data = new FileInputStream( new File( this.dirName, oneLoad[DATAFILEI] ) );
        if ( data == null ) {
            log.info( "Data File " + this.dirName + oneLoad[DATAFILEI] + " doesn't exist" );
            return false;
        }

        metaData.setQuantitationTypeName( oneLoad[QNAMEI] );
        metaData.setQuantitationTypeDescription( oneLoad[QDESCRIPTIONI] );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        if ( oneLoad.length >= IMAGECLONEI - 1 ) {
            metaData.setProbeIdsAreImageClones( Boolean.parseBoolean( oneLoad[IMAGECLONEI] ) );
        }

        StandardQuantitationType sQType = StandardQuantitationType.fromString( oneLoad[QTYPEI] );
        metaData.setType( sQType );

        ScaleType sType = ScaleType.fromString( oneLoad[QSCALEI] );
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
            this.eeLoaderService = ( SimpleExpressionDataLoaderService ) this
                    .getBean( "simpleExpressionDataLoaderService" );
            this.adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
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
                            log.error( "No experiments loaded!" + conf );
                    } catch ( Exception e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } catch ( IOException e ) {
            return e;
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
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

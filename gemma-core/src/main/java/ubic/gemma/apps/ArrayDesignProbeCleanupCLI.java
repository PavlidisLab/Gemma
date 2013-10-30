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
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;

/**
 * Delete design elements (probes) that are invalid for one reason or another. The impetus for this was to delete probes
 * in the MG-U74 version 1 set, but this is of general use.
 * 
 * @author Paul
 * @version $Id$
 */
public class ArrayDesignProbeCleanupCLI extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignProbeCleanupCLI p = new ArrayDesignProbeCleanupCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private String file;

    private CompositeSequenceService compositeSequenceService;

    private DesignElementDataVectorService designElementDataVectorService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option fileOption = OptionBuilder.hasArg().isRequired().withArgName( "file" )
                .withDescription( "File (tabbed) with probe ids in the first column" ).withLongOpt( "file" )
                .create( 'f' );

        addOption( fileOption );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design probe cleanup", args );

        if ( err != null ) return err;

        File f = new File( file );
        if ( !f.canRead() ) {
            log.fatal( "Cannot read from " + file );
            bail( ErrorCode.INVALID_OPTION );
        }

        if ( this.arrayDesignsToProcess.size() > 1 ) {
            throw new IllegalArgumentException( "Cannot be applied to more than one platform given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.arrayDesignsToProcess.iterator().next();
        try (InputStream is = new FileInputStream( f );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) );) {

            String line = null;
            int count = 0;
            while ( ( line = br.readLine() ) != null ) {

                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                String[] fields = line.split( "\t" );
                String probe = fields[0];

                CompositeSequence cs = compositeSequenceService.findByName( arrayDesign, probe );
                if ( cs != null ) {
                    log.info( "Removing: " + cs );
                    designElementDataVectorService.removeDataForCompositeSequence( cs );
                    compositeSequenceService.remove( cs );
                    count++;
                }
            }
            log.info( "Deleted " + count + " probes" );
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.compositeSequenceService = getBean( CompositeSequenceService.class );
        this.designElementDataVectorService = getBean( DesignElementDataVectorService.class );
        if ( this.hasOption( 'f' ) ) {
            file = this.getOptionValue( 'f' );
        }
    }
}

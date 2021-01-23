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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.io.*;

/**
 * Delete design elements (probes) that are invalid for one reason or another. The impetus for this was to remove probes
 * in the MG-U74 version 1 set, but this is of general use. Probes to be removed are given in a file.
 *
 * @author Paul
 */
public class ArrayDesignProbeCleanupCLI extends ArrayDesignSequenceManipulatingCli {

    private CompositeSequenceService compositeSequenceService;
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    private String file;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option fileOption = Option.builder( "f" ).hasArg().required().argName( "file" )
                .desc( "File (tabbed) with element ids in the first column" ).longOpt( "file" )
                .build();

        options.addOption( fileOption );

    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        super.processOptions( commandLine );
        this.compositeSequenceService = this.getBean( CompositeSequenceService.class );
        this.rawExpressionDataVectorService = this.getBean( RawExpressionDataVectorService.class );
        this.processedExpressionDataVectorService = this.getBean( ProcessedExpressionDataVectorService.class );
        if ( commandLine.hasOption( 'f' ) ) {
            file = commandLine.getOptionValue( 'f' );
        }
    }

    @Override
    public String getCommandName() {
        return "deletePlatformElements";
    }

    @Override
    protected void doWork() throws Exception {
        File f = new File( file );
        if ( !f.canRead() ) {
            throw new RuntimeException( "Cannot read from " + file );
        }

        if ( this.getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one platform given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();
        try ( InputStream is = new FileInputStream( f );
              BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {

            String line;
            int count = 0;
            while ( ( line = br.readLine() ) != null ) {

                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                String[] fields = line.split( "\t" );
                String probe = fields[0];

                CompositeSequence cs = compositeSequenceService.findByName( arrayDesign, probe );
                if ( cs != null ) {
                    AbstractCLI.log.info( "Removing: " + cs );
                    rawExpressionDataVectorService.removeDataForCompositeSequence( cs );
                    processedExpressionDataVectorService.removeDataForCompositeSequence( cs );
                    compositeSequenceService.remove( cs );
                    count++;
                }
            }
            AbstractCLI.log.info( "Deleted " + count + " probes" );
        } catch ( Exception e ) {
            throw e;
        }
    }
}

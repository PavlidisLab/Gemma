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
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import javax.annotation.Nullable;
import java.io.*;

/**
 * Delete design elements (probes) that are invalid for one reason or another. The impetus for this was to remove probes
 * in the MG-U74 version 1 set, but this is of general use. Probes to be removed are given in a file.
 *
 * @author Paul
 */
public class ArrayDesignProbeCleanupCLI extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService;
    private String file;

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option fileOption = Option.builder( "f" ).hasArg().required().argName( "file" )
                .desc( "File (tabbed) with element ids in the first column" ).longOpt( "file" )
                .build();

        options.addOption( fileOption );

    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'f' ) ) {
            file = commandLine.getOptionValue( 'f' );
        }
    }

    @Override
    public String getCommandName() {
        return "deletePlatformElements";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return null;
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
            int removedProbes = 0;
            int removedVectors = 0;
            while ( ( line = br.readLine() ) != null ) {

                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                String[] fields = line.split( "\t" );
                String probe = fields[0];

                CompositeSequence cs = compositeSequenceService.findByName( arrayDesign, probe );
                if ( cs != null ) {
                    log.info( "Removing: " + cs );
                    removedVectors += rawAndProcessedExpressionDataVectorService.removeByCompositeSequence( cs );
                    compositeSequenceService.remove( cs );
                    removedProbes++;
                }
            }
            log.info( String.format( "Deleted %d probes and %d corresponding data vectors.", removedProbes, removedVectors ) );
        }
    }
}

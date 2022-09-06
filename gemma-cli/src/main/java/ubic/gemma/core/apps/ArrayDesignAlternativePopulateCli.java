/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This only needs to be re-run when the mappings change. Existing mappings are not changed.
 *
 * @author paul
 */
public class ArrayDesignAlternativePopulateCli extends AbstractCLIContextCLI {

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.METADATA;
    }

    @Override
    public String getCommandName() {
        return "affyAltsUpdate";
    }

    @Override
    protected void buildOptions( Options options ) {

    }

    @Override
    protected boolean requireLogin() {
        return true;
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws Exception {

    }

    @Override
    protected void doWork() throws Exception {
        ArrayDesignService arrayDesignService = this.getBean( ArrayDesignService.class );

        // Read in the mapping file, which is in the classpath. This is also used by GeoPlatform (and in the DataUpdater)
        InputStream r = this.getClass().getResourceAsStream( "/ubic/gemma/core/loader/affy.altmappings.txt" );
        try ( BufferedReader in = new BufferedReader( new InputStreamReader( r ) ) ) {
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                if ( line.isEmpty() )
                    continue;

                String[] split = StringUtils.split( line, "=" );

                String from = split[0];
                String to = split[1];
                ArrayDesign fromAD = arrayDesignService.findByShortName( from );
                ArrayDesign toAD = arrayDesignService.findByShortName( to );

                if ( fromAD == null ) {
                    log.info( "No loaded platform matches " + from + ", skipping" );
                    continue;
                }
                if ( toAD == null ) {
                    log.info( "No loaded platform matches alternative " + to + ", skipping" );
                    continue;
                }

                if ( fromAD.equals( toAD ) )
                    continue; // no need to self-map?

                arrayDesignService.thawLite( fromAD );
                if ( fromAD.getAlternativeTo() != null ) {
                    log.info( "Already has an alternative mapped: " + from );
                    continue;
                }
                fromAD.setAlternativeTo( toAD );
                arrayDesignService.update( fromAD );
                log.info( "Mapped " + from + " to " + to );

            }
        } catch ( IOException e ) {
            throw e;
        }
    }

    @Override
    public String getShortDesc() {
        return "Populate the 'alternative' information for Affymetrix platforms";
    }

}

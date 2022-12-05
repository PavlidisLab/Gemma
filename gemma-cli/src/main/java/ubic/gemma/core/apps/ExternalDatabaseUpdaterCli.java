package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractSpringAwareCLI;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.net.URL;
import java.util.Date;

import static java.util.Objects.requireNonNull;

@Component
public class ExternalDatabaseUpdaterCli extends AbstractSpringAwareCLI {

    private static final String NAME_OPTION = "n",
            DESCRIPTION_OPTION = "d",
            RELEASE_OPTION = "release",
            RELEASE_NOTE_OPTION = "releaseNote",
            RELEASE_VERSION_OPTION = "releaseVersion",
            RELEASE_URL_OPTION = "releaseUrl",
            LAST_UPDATED_OPTION = "lastUpdated";

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    private String name;
    private String description;
    private boolean release;
    private String releaseNote;
    private String releaseVersion;
    private URL releaseUrl;
    private Date lastUpdated;

    @Override
    public String getCommandName() {
        return "updateExternalDatabase";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.SYSTEM;
    }

    @Override
    protected boolean requireLogin() {
        return true;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( NAME_OPTION )
                .longOpt( "name" )
                .hasArg()
                .optionalArg( false )
                .desc( "External database name" ).build() );
        options.addOption( DESCRIPTION_OPTION, "description", true, "New description" );
        options.addOption( RELEASE_OPTION, "release", false, "Update the release (only affects last modified moment))" );
        options.addOption( RELEASE_VERSION_OPTION, "release-version", true, "Release version" );
        options.addOption( Option.builder( RELEASE_URL_OPTION )
                .longOpt( "release-url" )
                .hasArg()
                .desc( "Release URL (optional)" )
                .type( URL.class ).build() );
        options.addOption( RELEASE_NOTE_OPTION, "release-note", true, "Note to include in the audit event related to the new release" );
        options.addOption( Option.builder( LAST_UPDATED_OPTION )
                .longOpt( "last-updated" )
                .hasArg()
                .desc( "Moment the release was performed if known, otherwise the current time will be used." )
                .type( Date.class ).build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws Exception {
        name = commandLine.getOptionValue( NAME_OPTION );
        description = commandLine.getOptionValue( DESCRIPTION_OPTION );
        release = commandLine.hasOption( RELEASE_OPTION );
        releaseNote = commandLine.getOptionValue( RELEASE_NOTE_OPTION );
        releaseVersion = commandLine.getOptionValue( RELEASE_VERSION_OPTION );
        releaseUrl = ( URL ) commandLine.getParsedOptionValue( RELEASE_URL_OPTION );
        lastUpdated = ( Date ) commandLine.getParsedOptionValue( LAST_UPDATED_OPTION );
        if ( lastUpdated == null ) {
            lastUpdated = new Date();
        }
    }

    @Override
    protected void doWork() throws Exception {
        ExternalDatabase ed = requireNonNull( externalDatabaseService.findByNameWithAuditTrail( name ),
                String.format( "No database with name %s.", name ) );
        if ( description != null ) {
            ed.setDescription( description );
        }
        if ( release || releaseVersion != null ) {
            if ( releaseVersion != null ) {
                AbstractCLI.log.info( String.format( "Updating %s release version to %s.", name, releaseVersion ) );
                externalDatabaseService.updateReleaseDetails( ed, releaseVersion, releaseUrl, releaseNote, lastUpdated );
            } else {
                AbstractCLI.log.info( String.format( "Updating %s last updated moment to %s.", name, lastUpdated ) );
                externalDatabaseService.updateReleaseLastUpdated( ed, releaseNote, lastUpdated );
            }
        } else {
            AbstractCLI.log.info( String.format( "Updating %s. Use the --release flag to update last updated or release infos.", name ) );
            externalDatabaseService.update( ed ); /* only update description, etc. */
        }
    }
}

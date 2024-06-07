package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.net.URL;
import java.util.Date;

import static java.util.Objects.requireNonNull;

public class ExternalDatabaseUpdaterCli extends AbstractAuthenticatedCLI {

    private static final String NAME_OPTION = "n",
            DESCRIPTION_OPTION = "d",
            PARENT_DATABASE_OPTION = "parentDatabase",
            RELEASE_OPTION = "release",
            RELEASE_NOTE_OPTION = "releaseNote",
            RELEASE_VERSION_OPTION = "releaseVersion",
            RELEASE_URL_OPTION = "releaseUrl",
            LAST_UPDATED_OPTION = "lastUpdated";

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    private String name;
    private String description;
    private ExternalDatabase parentDatabase;
    private boolean release;
    private String releaseNote;
    private String releaseVersion;
    private URL releaseUrl;
    private Date lastUpdated;

    public ExternalDatabaseUpdaterCli() {
        setRequireLogin( true );
    }

    @Override
    public String getCommandName() {
        return "updateExternalDatabase";
    }

    @Override
    public String getShortDesc() {
        return "Update an external database and optionally perform a release.";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( NAME_OPTION )
                .longOpt( "name" )
                .hasArg()
                .optionalArg( false )
                .desc( "External database name" )
                .required( true ).build() );
        options.addOption( DESCRIPTION_OPTION, "description", true, "Update the description" );
        options.addOption( PARENT_DATABASE_OPTION, "parent-database", true, "Update the parent database either by ID or name" );
        // release-specific options
        options.addOption( RELEASE_OPTION, "release", false, "Update the release details" );
        options.addOption( RELEASE_VERSION_OPTION, "release-version", true, "Release version (optional)" );
        options.addOption( Option.builder( RELEASE_URL_OPTION )
                .longOpt( "release-url" )
                .hasArg()
                .desc( "Release URL (optional)" )
                .type( URL.class ).build() );
        options.addOption( RELEASE_NOTE_OPTION, "release-note", true, "Note to include in the audit event related to the new release" );
        options.addOption( Option.builder( LAST_UPDATED_OPTION )
                .longOpt( "last-updated" )
                .hasArg()
                .desc( "Moment the release was performed if known, otherwise the current time will be used" )
                .type( Date.class ).build() );

    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        name = commandLine.getOptionValue( NAME_OPTION );
        description = commandLine.getOptionValue( DESCRIPTION_OPTION );
        release = commandLine.hasOption( RELEASE_OPTION );
        releaseNote = commandLine.getOptionValue( RELEASE_NOTE_OPTION );
        releaseVersion = commandLine.getOptionValue( RELEASE_VERSION_OPTION );
        releaseUrl = ( URL ) commandLine.getParsedOptionValue( RELEASE_URL_OPTION );
        lastUpdated = ( Date ) commandLine.getParsedOptionValue( LAST_UPDATED_OPTION );
        if ( !release && ( releaseNote != null || releaseVersion != null || releaseUrl != null || lastUpdated != null ) ) {
            throw new IllegalArgumentException( "The -release,--release flag must be used to update release details." );
        }
        if ( lastUpdated == null ) {
            lastUpdated = new Date();
        }
        if ( commandLine.hasOption( PARENT_DATABASE_OPTION ) ) {
            String parentDatabaseStr = commandLine.getOptionValue( PARENT_DATABASE_OPTION );
            try {
                parentDatabase = externalDatabaseService.loadWithExternalDatabases( Long.parseLong( parentDatabaseStr ) );
            } catch ( NumberFormatException nfe ) {
                parentDatabase = externalDatabaseService.findByNameWithExternalDatabases( parentDatabaseStr );
            }
            if ( parentDatabase == null ) {
                throw new IllegalArgumentException( String.format( "The parent database %s does not refer to any known external database.",
                        parentDatabaseStr ) );
            }
        }
    }

    @Override
    protected void doWork() throws Exception {
        ExternalDatabase ed = requireNonNull( externalDatabaseService.findByNameWithAuditTrail( name ),
                String.format( "No database with name %s.", name ) );
        if ( description != null ) {
            ed.setDescription( description );
        }
        if ( release ) {
            if ( releaseVersion != null ) {
                AbstractCLI.log.info( String.format( "Updating %s release version to %s.", name, releaseVersion ) );
                externalDatabaseService.updateReleaseDetails( ed, releaseVersion, releaseUrl, releaseNote, lastUpdated );
            } else {
                AbstractCLI.log.info( String.format( "Updating %s last updated moment to %s.", name, lastUpdated ) );
                externalDatabaseService.updateReleaseLastUpdated( ed, releaseNote, lastUpdated );
            }
        } else {
            AbstractCLI.log.info( String.format( "Updating %s. Use the --release/--release-version flags to update last updated or release infos.", name ) );
            externalDatabaseService.update( ed ); /* only update description, etc. */
        }
        if ( parentDatabase != null ) {
            AbstractCLI.log.info( String.format( "Updating the parent database of %s to %s.", name, parentDatabase ) );
            parentDatabase.getExternalDatabases().add( ed );
            externalDatabaseService.update( parentDatabase );
        }
    }
}

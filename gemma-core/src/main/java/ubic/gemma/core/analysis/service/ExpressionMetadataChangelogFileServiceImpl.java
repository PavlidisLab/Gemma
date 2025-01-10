package ubic.gemma.core.analysis.service;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getEEFolderName;

@Service
@CommonsLog
public class ExpressionMetadataChangelogFileServiceImpl implements ExpressionMetadataChangelogFileService {

    private static final DateTimeFormatter CHANGELOG_DATE_FORMAT = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

    @Autowired
    private UserManager userManager;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    @Override
    public String readChangelog( ExpressionExperiment expressionExperiment ) throws IOException {
        Path changelogFile = getChangelogFile( expressionExperiment );
        if ( Files.exists( changelogFile ) ) {
            return PathUtils.readString( changelogFile, StandardCharsets.UTF_8 );
        } else {
            return "";
        }
    }

    @Override
    public void addChangelogEntry( ExpressionExperiment expressionExperiment, String changelogEntry ) throws IOException {
        addChangelogEntry( expressionExperiment, changelogEntry, LocalDate.now() );
    }

    @Override
    public void addChangelogEntry( ExpressionExperiment expressionExperiment, String changelogEntry, LocalDate date ) throws IOException {
        User author = userManager.getCurrentUser();
        Path changelogFile = getChangelogFile( expressionExperiment );
        PathUtils.createParentDirectories( changelogFile );
        boolean exists = Files.exists( changelogFile );
        try ( BufferedWriter writer = Files.newBufferedWriter( changelogFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND ) ) {
            if ( exists ) {
                writer.append( "\n" );
            }
            String authorName;
            if ( StringUtils.isNotBlank( author.getName() ) ) {
                authorName = author.getName();
            } else {
                authorName = author.getUserName();
            }
            writer
                    .append( CHANGELOG_DATE_FORMAT.format( date ) ).append( "  " ).append( authorName ).append( "  <" ).append( author.getEmail() ).append( ">" ).append( "\n" )
                    .append( "\n" )
                    .append( "\t" ).append( StringUtils.strip( changelogEntry.replaceAll( "\n", "\n\t" ) ) )
                    .append( "\n" );
            log.info( "Added an entry to the changelog for " + expressionExperiment.getShortName() + "." );
        }
    }

    private Path getChangelogFile( ExpressionExperiment expressionExperiment ) {
        return metadataDir.resolve( getEEFolderName( expressionExperiment ) ).resolve( "CHANGELOG.md" );
    }
}

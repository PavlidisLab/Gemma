package ubic.gemma.persistence.service.blacklist;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.blacklist.BlacklistedValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.model.blacklist.BlacklistedExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

import java.util.HashSet;
import java.util.Set;

@Service
public class BlacklistedEntityServiceImpl extends AbstractVoEnabledService<BlacklistedEntity, BlacklistedValueObject> implements BlacklistedEntityService {

    private final BlacklistedEntityDao blacklistedEntityDao;

    @Autowired
    public BlacklistedEntityServiceImpl( BlacklistedEntityDao voDao ) {
        super( voDao );
        this.blacklistedEntityDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted( String accession ) {
        return blacklistedEntityDao.isBlacklisted( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted( ArrayDesign platform ) {
        return blacklistedEntityDao.isBlacklisted( platform );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted( ExpressionExperiment dataset ) {
        return blacklistedEntityDao.isBlacklisted( dataset );
    }

    @Override
    @Transactional(readOnly = true)
    public BlacklistedEntity findByAccession( String accession ) {
        return blacklistedEntityDao.findByAccession( accession );
    }

    @Override
    @Transactional
    public BlacklistedExperiment blacklistExpressionExperiment( ExpressionExperiment dataset, String reason ) {
        if ( StringUtils.isBlank( reason ) ) {
            throw new IllegalArgumentException( "Reason for blacklisting an experiment cannot be empty or null." );
        }
        BlacklistedExperiment be = doBlacklistExpressionExperiment( dataset, reason );
        log.info( String.format( "%s has been blacklisted.", dataset ) );
        return be;
    }

    @Override
    @Transactional
    public BlacklistedPlatform blacklistPlatform( ArrayDesign platform, String reason ) {
        if ( StringUtils.isBlank( reason ) ) {
            throw new IllegalArgumentException( "Reason for blacklisting an experiment cannot be empty or null." );
        }

        BlacklistedPlatform bp = null;
        if ( platform.getExternalReferences().size() > 1 ) {
            for ( DatabaseEntry de : platform.getExternalReferences() ) {
                bp = new BlacklistedPlatform();
                bp.setShortName( platform.getShortName() );
                bp.setReason( reason );
                bp.setExternalAccession( de );
                bp = ( BlacklistedPlatform ) blacklistedEntityDao.save( bp );
            }
        } else {
            BlacklistedPlatform bp2 = new BlacklistedPlatform();
            bp2.setShortName( platform.getShortName() );
            bp2.setReason( reason );
            platform.getExternalReferences().stream()
                    // FIXME: have some preference for picking
                    .findAny()
                    .ifPresent( databaseEntry -> {
                        bp2.setExternalAccession( copyDatabaseEntry( databaseEntry ) );
                    } );
            log.info( String.format( "%s has been blacklisted.", bp ) );
            bp = ( BlacklistedPlatform ) blacklistedEntityDao.save( bp2 );
        }

        // just to make sure since bp is not guaranteed to have been assigned (as per Java, but logically for sure)
        assert bp != null;

        // cascade through all associated experiments
        Set<BlacklistedExperiment> blacklistedExperiments = new HashSet<>();
        for ( ExpressionExperiment ee : blacklistedEntityDao.getNonBlacklistedExpressionExperiments( platform ) ) {
            blacklistedExperiments.add( doBlacklistExpressionExperiment( ee, String.format( "%s (via blacklisting of %s)", reason, platform ) ) );
        }

        if ( blacklistedExperiments.isEmpty() ) {
            log.info( String.format( "%s has been blacklisted.", platform ) );
        } else {
            log.info( String.format( "%s has been blacklisted. In addition, %d associated datasets have also been blacklisted.",
                    platform, blacklistedExperiments.size() ) );
        }

        return bp;
    }

    @Override
    @Transactional
    public int removeAll() {
        return blacklistedEntityDao.removeAll();
    }

    private BlacklistedExperiment doBlacklistExpressionExperiment( ExpressionExperiment dataset, String reason ) {
        BlacklistedExperiment be = new BlacklistedExperiment();
        be.setShortName( dataset.getShortName() );
        be.setReason( reason );
        if ( dataset.getAccession() != null ) {
            DatabaseEntry databaseEntry = dataset.getAccession();
            be.setExternalAccession( copyDatabaseEntry( databaseEntry ) );
        }
        return ( BlacklistedExperiment ) blacklistedEntityDao.save( be );
    }

    private static DatabaseEntry copyDatabaseEntry( DatabaseEntry de ) {
        DatabaseEntry de2 = new DatabaseEntry();
        de2.setAccession( de.getAccession() );
        de2.setExternalDatabase( de.getExternalDatabase() );
        return de2;
    }
}

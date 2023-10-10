package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.experiment.BlacklistedExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseImmutableService;
import ubic.gemma.persistence.service.BaseVoEnabledService;

public interface BlacklistedEntityService extends BaseImmutableService<BlacklistedEntity>, BaseVoEnabledService<BlacklistedEntity, BlacklistedValueObject> {

    /**
     * @see BlacklistedEntityDao#isBlacklisted(String)
     */
    boolean isBlacklisted( String accession );

    /**
     * @see BlacklistedEntityDao#isBlacklisted(ArrayDesign)
     */
    boolean isBlacklisted( ArrayDesign platform );

    /**
     * @see BlacklistedEntityDao#isBlacklisted(ExpressionExperiment)
     */
    boolean isBlacklisted( ExpressionExperiment dataset );

    /**
     * @see BlacklistedEntityDao#findByAccession(String)
     */
    BlacklistedEntity findByAccession( String accession );

    /**
     * Blacklist a given dataset.
     * @param dataset the dataset to blacklist
     * @param reason a reason, which must be non-empty
     */
    @Secured({ "GROUP_ADMIN", "ACL_SECURABLE_EDIT" })
    BlacklistedExperiment blacklistExpressionExperiment( ExpressionExperiment dataset, String reason );

    /**
     * Blacklist a given platform.
     * <p>
     * If the platform has associated datasets, those are blacklisted as well as per {@link #blacklistExpressionExperiment(ExpressionExperiment, String)}.
     * The reason will be adjusted to reflect the cascading through the platform.
     * @param platform the platform to blacklist
     * @param reason a reason, which must be non-empty
     */
    @Secured({ "GROUP_ADMIN", "ACL_SECURABLE_EDIT" })
    BlacklistedPlatform blacklistPlatform( ArrayDesign platform, String reason );

    /**
     * Remove all blacklisted platforms and experiments.
     */
    @Secured({ "GROUP_ADMIN" })
    int removeAll();
}

package ubic.gemma.core.apps;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.model.Securable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import javax.annotation.Nullable;
import java.util.List;

@Component
public class AclLinterCli extends AbstractCLI {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    protected void buildOptions( Options options ) {

    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {

    }

    @Override
    protected void doWork() throws Exception {
        Session session = sessionFactory.openSession();
        try {
            warnEntitiesLackingObjectIdentity( session, ArrayDesign.class );
            warnEntitiesLackingObjectIdentity( session, ExpressionExperiment.class );
            warnEntitiesLackingObjectIdentity( session, ExpressionExperimentSubSet.class );
            warnEntitiesLackingObjectIdentity( session, BioMaterial.class );
            warnEntitiesLackingObjectIdentity( session, BioAssay.class );
            warnEntitiesLackingObjectIdentity( session, ExperimentalDesign.class );
            warnEntitiesLackingObjectIdentity( session, ExperimentalFactor.class );
            warnEntitiesLackingObjectIdentity( session, FactorValue.class );

            warnEntitiesWhoseParentObjectIsMissing( session, ExpressionExperimentSubSet.class, ExpressionExperiment.class );
            warnEntitiesWhoseParentObjectIsMissing( session, BioAssay.class, ExpressionExperiment.class );
            warnEntitiesWhoseParentObjectIsMissing( session, ExperimentalDesign.class, ExpressionExperiment.class );
            warnEntitiesWhoseParentObjectIsMissing( session, ExperimentalFactor.class, ExpressionExperiment.class );
            warnEntitiesWhoseParentObjectIsMissing( session, FactorValue.class, ExpressionExperiment.class );
        } finally {
            session.close();
        }
    }

    private void warnEntitiesLackingObjectIdentity( Session session, Class<? extends Securable> clazz ) {
        //language=HQL
        //noinspection unchecked
        List<? extends Securable> list = session
                .createQuery( "select e from " + clazz.getName() + " e "
                        + "where e.id not in ( " + "select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type" + ")" )
                .setParameter( "type", clazz.getName() )
                .list();
        for ( Securable ee : list ) {
            addErrorObject( ee, String.format( "Object with ID %d lacks an ACL identity of type %s", ee.getId(), clazz.getName() ) );
        }
    }

    private void warnEntitiesWhoseParentObjectIsMissing( Session session, Class<? extends Securable> clazz, Class<? extends Securable> parentType ) {
        //noinspection unchecked
        List<Object[]> list = session
                .createQuery( "select e, aoi.parentObject from " + clazz.getName() + " e, AclObjectIdentity aoi "
                        + "where aoi.identifier = e.id and aoi.type = :type "
                        + "and aoi.parentObject is not null "
                        + "and aoi.parentObject.identifier not in (select p.id from " + parentType.getName() + " p)" )
                .setParameter( "type", clazz.getName() )
                .list();
        for ( Object[] ee : list ) {
            Securable s = ( Securable ) ee[0];
            AclObjectIdentity parentAoi = ( AclObjectIdentity ) ee[1];
            addErrorObject( s, String.format( "Parent of object with ID %d does not exist; parent ACL identity is %s", s.getId(), parentAoi ) );
        }
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "lintAcls";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.SYSTEM;
    }
}

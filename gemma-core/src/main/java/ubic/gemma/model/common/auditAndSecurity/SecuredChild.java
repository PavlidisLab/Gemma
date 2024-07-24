package ubic.gemma.model.common.auditAndSecurity;

public interface SecuredChild extends Securable, gemma.gsec.model.SecuredChild {

    @Override
    Securable getSecurityOwner();
}

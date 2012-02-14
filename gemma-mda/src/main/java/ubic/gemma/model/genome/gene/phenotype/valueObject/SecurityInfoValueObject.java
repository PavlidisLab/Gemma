package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class SecurityInfoValueObject {

    private boolean currentUserHasWritePermission = false;

    private boolean currentUserIsOwner = false;

    private boolean isPublic = true;

    private boolean isShared = false;

    private String owner = null;

    public SecurityInfoValueObject( boolean currentUserHasWritePermission, boolean currentUserIsOwner,
            boolean isPublic, boolean isShared, String owner ) {
        super();
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.currentUserIsOwner = currentUserIsOwner;
        this.isPublic = isPublic;
        this.isShared = isShared;
        this.owner = owner;
    }

    public boolean isCurrentUserHasWritePermission() {
        return this.currentUserHasWritePermission;
    }

    public void setCurrentUserHasWritePermission( boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    public boolean isCurrentUserIsOwner() {
        return this.currentUserIsOwner;
    }

    public void setCurrentUserIsOwner( boolean currentUserIsOwner ) {
        this.currentUserIsOwner = currentUserIsOwner;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public void setPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    public boolean isShared() {
        return this.isShared;
    }

    public void setShared( boolean isShared ) {
        this.isShared = isShared;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner( String owner ) {
        this.owner = owner;
    }

}

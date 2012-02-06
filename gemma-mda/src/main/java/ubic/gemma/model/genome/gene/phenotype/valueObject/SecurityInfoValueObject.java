package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class SecurityInfoValueObject {

    private Boolean currentUserHasWritePermission = false;

    private Boolean currentUserIsOwner = false;

    private Boolean isPublic = true;

    private Boolean isShared = false;

    private String owner = null;

    public SecurityInfoValueObject( Boolean currentUserHasWritePermission, Boolean currentUserIsOwner,
            Boolean isPublic, Boolean isShared, String owner ) {
        super();
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.currentUserIsOwner = currentUserIsOwner;
        this.isPublic = isPublic;
        this.isShared = isShared;
        this.owner = owner;
    }

    public Boolean getCurrentUserHasWritePermission() {
        return this.currentUserHasWritePermission;
    }

    public void setCurrentUserHasWritePermission( Boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    public Boolean getCurrentUserIsOwner() {
        return this.currentUserIsOwner;
    }

    public void setCurrentUserIsOwner( Boolean currentUserIsOwner ) {
        this.currentUserIsOwner = currentUserIsOwner;
    }

    public Boolean getIsPublic() {
        return this.isPublic;
    }

    public void setIsPublic( Boolean isPublic ) {
        this.isPublic = isPublic;
    }

    public Boolean getIsShared() {
        return this.isShared;
    }

    public void setIsShared( Boolean isShared ) {
        this.isShared = isShared;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner( String owner ) {
        this.owner = owner;
    }

}

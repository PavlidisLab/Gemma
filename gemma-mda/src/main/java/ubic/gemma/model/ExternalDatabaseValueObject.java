package ubic.gemma.model;

import ubic.gemma.model.common.description.ExternalDatabase;

public class ExternalDatabaseValueObject {
    
	private java.lang.String localInstallDbName;
    private java.lang.String webUri;
    private java.lang.String ftpUri;
    private ubic.gemma.model.common.description.DatabaseType type;
    private ubic.gemma.model.common.auditAndSecurity.Contact databaseSupplier;
    private java.util.Collection<ubic.gemma.model.common.description.LocalFile> flatFiles = new java.util.HashSet<ubic.gemma.model.common.description.LocalFile>();
	
    public java.lang.String getLocalInstallDbName()
    {
        return this.localInstallDbName;
    }

    public void setLocalInstallDbName(java.lang.String localInstallDbName)
    {
        this.localInstallDbName = localInstallDbName;
    }

    public java.lang.String getWebUri()
    {
        return this.webUri;
    }

    public void setWebUri(java.lang.String webUri)
    {
        this.webUri = webUri;
    }

    public java.lang.String getFtpUri()
    {
        return this.ftpUri;
    }

    public void setFtpUri(java.lang.String ftpUri)
    {
        this.ftpUri = ftpUri;
    }

    public ubic.gemma.model.common.description.DatabaseType getType()
    {
        return this.type;
    }

    public void setType(ubic.gemma.model.common.description.DatabaseType type)
    {
        this.type = type;
    }

//    public ubic.gemma.model.common.auditAndSecurity.Contact getDatabaseSupplier()
//    {
//        return this.databaseSupplier;
//    }
//
//    public void setDatabaseSupplier(ubic.gemma.model.common.auditAndSecurity.Contact databaseSupplier)
//    {
//        this.databaseSupplier = databaseSupplier;
//    }
//
//    public java.util.Collection<ubic.gemma.model.common.description.LocalFile> getFlatFiles()
//    {
//        return this.flatFiles;
//    }
//
//    public void setFlatFiles(java.util.Collection<ubic.gemma.model.common.description.LocalFile> flatFiles)
//    {
//        this.flatFiles = flatFiles;
//    }

    public static ExternalDatabaseValueObject fromEntity(ExternalDatabase ed) {
    	ExternalDatabaseValueObject vo = new ExternalDatabaseValueObject();
    	//ed.getDescription();
    	vo.setFtpUri( ed.getFtpUri() );
    	vo.setLocalInstallDbName( ed.getLocalInstallDbName() );
    	vo.setType( ed.getType() );
    	vo.setWebUri( ed.getWebUri() );
    	
    	return vo;
    }
    
}

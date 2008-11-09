package ubic.gemma.model.genome;

public class PhysicalLocationServiceImpl implements PhysicalLocationService {

    private PhysicalLocationDao physicalLocationDao;

    public PhysicalLocationDao getPhysicalLocationDao() {
        return physicalLocationDao;
    }

    public void setPhysicalLocationDao( PhysicalLocationDao physicalLocationDao ) {
        this.physicalLocationDao = physicalLocationDao;
    }

    public void thaw( PhysicalLocation physicalLocation ) {
        this.getPhysicalLocationDao().thaw( physicalLocation );

    }

}

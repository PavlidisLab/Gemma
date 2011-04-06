package ubic.gemma.persistence;

import ubic.gemma.model.Reference;

public interface GemmaSessionBackedValueObject {
	
	public Reference getReference();
	
    public void setReference(Reference reference);

}

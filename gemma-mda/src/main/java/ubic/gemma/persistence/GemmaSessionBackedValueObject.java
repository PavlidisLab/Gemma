package ubic.gemma.persistence;

public interface GemmaSessionBackedValueObject {
	
	public Long getSessionId();
	
	public void setSessionId(Long l);
	
	public boolean isSession();
	
	public void setSession(boolean b);
	
	public Long getId();

}

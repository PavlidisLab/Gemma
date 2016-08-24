package ubic.gemma.model.expression.biomaterial;

import java.io.Serializable;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;


public class BioMatFactorCountObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6423320688701495103L;
	
	private long eeId;
	
	private String factor;
	 
	private int count;
	
	

	public BioMatFactorCountObject(Long eeId, String factor, int count){
		
		this.eeId = eeId;
		this.factor = factor;
		this.count = count;
	}



	public long getEeId() {
		return eeId;
	}



	public void setEeId(long eeId) {
		this.eeId = eeId;
	}



	public String getFactor() {
		return factor;
	}



	public void setFactor(String factor) {
		this.factor = factor;
	}



	public int getCount() {
		return count;
	}



	public void setCount(int count) {
		this.count = count;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
		result = prime * result + (int) (eeId ^ (eeId >>> 32));
		result = prime * result + ((factor == null) ? 0 : factor.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BioMatFactorCountObject other = (BioMatFactorCountObject) obj;
		if (count != other.count)
			return false;
		if (eeId != other.eeId)
			return false;
		if (factor == null) {
			if (other.factor != null)
				return false;
		} else if (!factor.equals(other.factor))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "BioMatFactorCountObject [eeId=" + eeId + ", factor=" + factor
				+ ", count=" + count + "]";
	}


	
	
}
	

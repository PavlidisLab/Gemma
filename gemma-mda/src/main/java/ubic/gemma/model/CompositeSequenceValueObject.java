package ubic.gemma.model;

import ubic.gemma.model.expression.designElement.CompositeSequence;

public class CompositeSequenceValueObject {
	private Long id;
	private String name;
	private String description;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public static CompositeSequenceValueObject fromEntity (CompositeSequence cs) {
		CompositeSequenceValueObject vo = new CompositeSequenceValueObject();
		vo.setDescription(cs.getDescription());
		vo.setId(cs.getId());
	    vo.setName(cs.getName());
	    return vo;
	}
	
}

Ext.namespace('Gemma.BibliographicReference');

Gemma.BibliographicReference.DetailsPanel  = Ext.extend(Ext.FormPanel, {
	title: 'Bibliographic Reference Details',
	autoScroll:true,
	padding: 20,
	updateFields: function(bibRefRecord){
			
					
		this.items.each(function(field){
			field.show();
		});
		//this.setTitle(bibRefRecord.get('title'));
		
		this.bibtitle.setValue(bibRefRecord.get('title'));
		this.abstractBibli.setValue(bibRefRecord.get('abstractText'));
		this.authors.setValue(bibRefRecord.get('authorList'));
		this.publication.setValue(bibRefRecord.get('publication'));
		if (bibRefRecord.get('publicationDate').format) {
			this.date.setValue(bibRefRecord.get('publicationDate').format('F j, Y'));
		} else {
			this.date.setValue(bibRefRecord.get('publicationDate'));
		}
		if (bibRefRecord.get('citation')) {
			this.citation.setValue(bibRefRecord.get('citation').citation);
		}
		
		var allExperiments = '';
		var i;
		for (i = 0; i <
		bibRefRecord.get('experiments').length; i++) {
			allExperiments += bibRefRecord.get('experiments')[i].shortName +
			" : ";
			allExperiments += bibRefRecord.get('experiments')[i].name +
			"\n";
		}
		this.experiments.setValue(allExperiments);
		
		var allMeshTerms = "";
		
		for (i = 0; i <
		bibRefRecord.get('meshTerms').length; i++) {
		
			allMeshTerms += bibRefRecord.get('meshTerms')[i] +
			"\n";
		}
		this.pages.setValue(bibRefRecord.get('pages'));
		this.pubmed.setValue(bibRefRecord.get('pubAccession'));
		this.mesh.setValue(allMeshTerms);
		
		var allChemicalsTerms = "";
		
		for (i = 0; i <
		bibRefRecord.get('chemicalsTerms').length; i++) {
			allChemicalsTerms += bibRefRecord.get('chemicalsTerms')[i] +
			"\n";
		}
		this.chemicals.setValue(allChemicalsTerms);
		
		
		this.items.each(function(field){
			// trick textareas into resizing themselves
			field.fireEvent('change', field, field.getValue, field.getValue);
			field.disable();
		});
	},
	defaults:{
		style: "width:100%;color:black",
		hidden: true,
		grow: true,
		growMin:1,
		growMax: 60,
		growAppend:'',
		readOnly: true,
		value: 'placeholder'
	},
	initComponent: function(){
		
		Gemma.BibliographicReference.DetailsPanel.superclass.initComponent.call(this);
		
		this.bibtitle = new Ext.form.TextArea({
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Title'
		});
		
		this.abstractBibli = new Ext.form.TextArea({
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Abstract'
		});
		
		this.authors = new Ext.form.TextArea({
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Authors',
			readOnly: true
		});
		
		this.publication = new Ext.form.TextArea({
			enableKeyEvents: true,
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Publication',
			readOnly: true
		});
		
		this.date = new Ext.form.TextArea({
			enableKeyEvents: true,
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Date',
			readOnly: true
		});
		
		this.pages = new Ext.form.TextArea({
			enableKeyEvents: true,
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Pages',
			readOnly: true
		});
		
		this.experiments = new Ext.form.TextArea({
			enableKeyEvents: true,
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Experiments',
			readOnly: true
		});
		
		this.citation = new Ext.form.TextArea({
			enableKeyEvents: true,
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Citation',
			readOnly: true
		});
		
		this.pubmed = new Ext.form.TextArea({
			enableKeyEvents: true,
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Pubmed',
			readOnly: true
		});
		
		this.mesh = new Ext.form.TextArea({
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Mesh',
			readOnly: true,
			grow: true
		});
		
		this.chemicals = new Ext.form.TextArea({
			disabledClass: 'disabled-plain',
			fieldClass: 'x-bare-field',
			fieldLabel: 'Chemicals',
			readOnly: true,
			grow: true
		});
		
		this.add([this.bibtitle, this.abstractBibli, this.authors, this.publication, this.date, this.pages, this.citation, this.experiments, this.pubmed, this.mesh, this.chemicals]);
		
		// can't do this in the definition b/c it gets overidden by defaults when it's added to parent
		Ext.apply(this.abstractBibli, {
			style: "width:100%;color:black;background-color: #fcfcfc; border: 1px solid #cccccc;",
		});
		
	}// eo initComponent
});
 
    
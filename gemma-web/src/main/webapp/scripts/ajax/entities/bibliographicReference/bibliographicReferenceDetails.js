Ext.namespace('Gemma.BibliographicReference');

// Originally, this panel extended from Ext.FormPanel. It should not do so because we may put it inside  
// another FormPanel, but FormPanel cannot be put inside another FormPanel. To be able to layout components
// in the same way as in FormPanel, we make it to have form layout by using the config:
// 		layout: 'form'
Gemma.BibliographicReference.DetailsPanel  = Ext.extend(Ext.Panel, {
	genePhenotypeSeparator: '&hArr;',
	layout: 'form',
	title: 'Bibliographic Reference Details',
	collapseByDefault: false,	
	defaults: {
		hidden: true,
		labelWidth: 120
	},
	initComponent: function(){
		var getPudmedAnchor = function(pudmedUrl) {
		    return '<a target="_blank" href="' +
		        pudmedUrl +
		        '"><img ext:qtip="Go to PubMed (in new window)"  src="/Gemma/images/pubmed.gif" width="47" height="15" /></a>';
		}; 
		
		Ext.apply(this, {
			updateFields: function(bibRefRecord) {
				this.citation.show();		
				this.detailsFieldset.show();		
				this.bibtitle.setValue(bibRefRecord.get('title'));
				this.abstractBibli.setValue(bibRefRecord.get('abstractText'));
				this.authors.setValue(bibRefRecord.get('authorList'));
				if (bibRefRecord.get('citation')) {
					this.citation.setValue(bibRefRecord.get('citation').citation + ' ' +
						getPudmedAnchor(bibRefRecord.get('citation').pubmedURL));
				}
				
				var allExperiments = '';
				var i;
				for (i = 0; i <	bibRefRecord.get('experiments').length; i++) {
					allExperiments += bibRefRecord.get('experiments')[i].shortName +
					" : ";
					allExperiments += bibRefRecord.get('experiments')[i].name +
					"<br />";
				}
				this.experiments.setValue(allExperiments);
				
				var allMeshTerms = "";
				
				for (i = 0; i <	bibRefRecord.get('meshTerms').length; i++) {
					allMeshTerms += bibRefRecord.get('meshTerms')[i];
					
					if (i < bibRefRecord.get('meshTerms').length - 1) {
						allMeshTerms += "; ";
					}
				}
				this.pubmed.setValue(bibRefRecord.get('pubAccession'));
				this.mesh.setValue(allMeshTerms);
				
				var allChemicalsTerms = "";
				for (i = 0; i <	bibRefRecord.get('chemicalsTerms').length; i++) {
					allChemicalsTerms += bibRefRecord.get('chemicalsTerms')[i];
		
					if (i < bibRefRecord.get('chemicalsTerms').length - 1) {
						allChemicalsTerms += "; ";
					}
				}
				this.chemicals.setValue(allChemicalsTerms);
				
				var allgenePhenotypeAssociations = "";
				if (bibRefRecord.get('bibliographicPhenotypes') != null) {
					for (i = 0; i <	bibRefRecord.get('bibliographicPhenotypes').length; i++) {
						allgenePhenotypeAssociations += bibRefRecord.get('bibliographicPhenotypes')[i].geneName + ' ' + this.genePhenotypeSeparator + ' ';
						for (j = 0; j <	bibRefRecord.get('bibliographicPhenotypes')[i].phenotypesValues.length; j++) {
							allgenePhenotypeAssociations += bibRefRecord.get('bibliographicPhenotypes')[i].phenotypesValues[j].value;
							
							if (j < bibRefRecord.get('bibliographicPhenotypes')[i].phenotypesValues.length - 1) {
								allgenePhenotypeAssociations += '; ';
							}
						}
						allgenePhenotypeAssociations += '<br />';
					}
				}
				this.genePhenotypeAssociation.setValue(allgenePhenotypeAssociations);
						
				this.detailsFieldset.items.each(function(field) {
					if (field.getValue() == "") {
						field.hide();
					} else {
						field.show();
					}
				});
				if (this.experiments.getValue() == "" && this.genePhenotypeAssociation.getValue() == "") {
					this.annotationsFieldset.hide();
				} else {
					this.annotationsFieldset.show();
				}
				this.annotationsFieldset.items.each(function(field) {
					if (field.getValue() == "") {
						field.hide();
					} else {
						field.show();
					}
				});
			}
		});
		
		Gemma.BibliographicReference.DetailsPanel.superclass.initComponent.call(this);
		
		this.citation = new Ext.form.DisplayField({
			hideLabel: true
		});
		
		this.bibtitle = new Ext.form.DisplayField({
			fieldLabel: 'Title'
		});
		
		this.abstractBibli = new Ext.form.TextArea({
			anchor: '100%',
			grow: true,
			growMin:1,
			growMax: 62,
			fieldLabel: 'Abstract',
			disabledClass: 'disabled-plain',
			disabled: true
		});
		
		this.authors = new Ext.form.DisplayField({
			fieldLabel: 'Authors'
		});
		
		this.pubmed = new Ext.form.DisplayField({
			fieldLabel: 'PubMed Id'
		});
		
		this.mesh = new Ext.form.DisplayField({
			fieldLabel: 'MeSH'
		});
		
		this.chemicals = new Ext.form.DisplayField({
			fieldLabel: 'Chemicals'
		});

		this.detailsFieldset =	new Ext.form.FieldSet({
			defaults: {
				labelStyle: 'padding-top: 1px;'
			},
	        collapsed: this.collapseByDefault,
			cls : 'no-collapsed-border',
			anchor: '100%',
			title: 'Publication Details',
			collapsible: true,
			style: "margin-bottom: 3px;",
			items: [
				this.bibtitle, this.abstractBibli, this.authors, this.pubmed, this.mesh, this.chemicals	
			]
		});

		this.experiments = new Ext.form.DisplayField({
			fieldLabel: 'Experiments'
		});
		
		this.genePhenotypeAssociation = new Ext.form.DisplayField({
			fieldLabel: 'Gene ' + this.genePhenotypeSeparator + ' Phenotype'
		});
		
		this.annotationsFieldset =	new Ext.form.FieldSet({
			defaults: {
				labelStyle: 'padding-top: 1px;'
			},
			cls: 'no-collapsed-border',
			anchor: '100%',
			title: 'Current Annotations',
			collapsible: true,
			style: "margin-bottom: 3px;",
			items: [
				this.experiments, this.genePhenotypeAssociation
			]
		});
		
		this.add(this.citation, this.detailsFieldset, this.annotationsFieldset);
	} // initComponent
});

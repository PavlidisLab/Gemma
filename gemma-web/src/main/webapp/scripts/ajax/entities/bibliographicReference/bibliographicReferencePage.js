Ext.namespace('Gemma.BibliographicReference');

Gemma.BibliographicReference.Page =  Ext.extend(Ext.Panel, {
	// would use vbox, but then user can't resize panels
	layout: 'border',
	initComponent: function(){
		
		Gemma.BibliographicReference.Page.superclass.initComponent.call(this);
		
		var grid = new Gemma.BibliographicReference.PagingGrid({
			region:'center',
			title:'Bibliographic References'
		});
		
		var details = new Gemma.BibliographicReference.DetailsPanel({
			region:'south',
			split: true,
			title:'Details',
			collapsible: true,
			collapsed: true
		});
		
		this.add(grid);
		this.add(details);
		this.doLayout();
		
		grid.on('bibRefSelected', function(record){
			details.expand();
			details.updateFields(record);
		}, this);
		
		this.on('render', function(){
			details.setHeight(Ext.getBody().getViewSize().height / 2);
		},this);
	}
	
});
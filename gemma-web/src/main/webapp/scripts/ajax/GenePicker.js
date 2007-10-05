function GenePicker(div, result) {

	this.div = div;
	this.result = result;
	this.memStore = [];
	this.GeneRecord = Ext.data.Record.create( [
		{ name: "id", type: "int" },
		{ name: "name", type: "string" },
		{ name: "officialSymbol", type: "string" },
		{ name: "officialName", type: "string" }
	] );
	this.TaxonRecord = Ext.data.Record.create( [
		{ name: "id", type: "int" },
		{ name: "abbreviation", type: "string" },
		{ name: "commonName", type: "string" },
		{ name: "scientificName", type: "string" }
	] );
	this.populateHiddenField = function() {
		value = '';
		this.gridDs.each( function( record ) {
			value = value + record.data.id + ',';
		} );
		if ( value.length > 0 ) {
			this.result = value.substring(0, value.length-1);
		}
	};
	
	/* set up the grid that stores selected genes... */
	this.gridDs = new Ext.data.Store( {
		proxy : new Ext.data.MemoryProxy( this.memStore ),
		reader : new Ext.data.ArrayReader( {}, this.GeneRecord )
	} );
	this.gridCm = new Ext.grid.ColumnModel( [
		{ header: "Name", dataIndex: "name" },
		{ header: "Official Symbol", dataIndex: "officialSymbol" },
		{ header: "Official Name", dataIndex: "officialName", id: 'name' }
	] );
	this.grid = new Ext.grid.Grid( this.div, {
		ds : this.gridDs,
		cm : this.gridCm,
		loadMask : true,
		autoExpandColumn : 'name'
	} );
	this.geneSelected = function( record ) {
        this.gridDs.add( record );
        this.populateHiddenField();
        //this.grid.getView().refresh( true );
        this.geneCombo.collapse();
	};
	
	/* set up the form from which new genes are selected... */
	this.taxonComboDs = new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( GenePickerController.getTaxa ),
		reader : new Ext.data.ListRangeReader( { id: "id" }, this.TaxonRecord )
	} );
	this.taxonComboDs.load();
	this.taxonCombo = new Ext.form.ComboBox( {
        fieldLabel : 'Select a gene',
		emptyText: 'Select a taxon',
		mode : 'local',
		store : this.taxonComboDs,
        displayField : 'scientificName',
        valueField : 'id',
        editable : false,
		hideTrigger : false,
		selectOnFocus : true,
		triggerAction : 'all'
	} );
	this.getSearchParams = function( query ) {
		return [ query, this.taxonCombo.getValue() ];
	};
	
	this.geneComboDs = new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( GenePickerController.searchGenes ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, this.GeneRecord ),
        remoteSort : true 
	} );
	this.geneCombo = new Ext.form.ComboBox( {
        fieldLabel : 'Search for a gene',
        emptyText : 'Search for a gene',
        loadingText: 'Searching...',
		store : this.geneComboDs,
        displayField : 'name',
        hideTrigger : true,
        minChars : 3,
        pageSize : 0,
 		// custom rendering template tpl: new Ext.Template( '' ),
        typeAhead : false,
        onSelect : this.geneSelected.bind( this ),
        getParams : this.getSearchParams.bind( this )
	} );
	
	this.geneForm = new Ext.form.Form( {
	} );
	this.geneForm.add( this.taxonCombo );
	this.geneForm.add( this.geneCombo );
	
	this.init = function() {
		this.grid.render();
		this.geneForm.render( this.grid.getView().getHeaderPanel( true ) );
	}
}

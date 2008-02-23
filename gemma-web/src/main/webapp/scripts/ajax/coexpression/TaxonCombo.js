Ext.namespace('Ext.Gemma');

/* Ext.Gemma.TaxonCombo constructor...
 */
Ext.Gemma.TaxonCombo = function ( config ) {
	
	this.taxonId = config.taxonId; delete config.taxonId;
	
	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'commonName',
		valueField : 'id',
		editable : false,
		lazyInit : false,
		mode : 'local',
		selectOnFocus : false,
		triggerAction : 'all',
		store : new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( GenePickerController.getTaxa ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.TaxonCombo.getRecord() ),
			remoteSort : true
		} ),
		tpl : Ext.Gemma.TaxonCombo.getTemplate()
	};
	superConfig.store.load( { params: [], callback: function() {
		this.setValue( this.getValue() );	// make sure the text of the selected item gets picked up
	}.bind( this ) } );
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.TaxonCombo.superclass.constructor.call( this, superConfig );
};

/* static methods
 */
Ext.Gemma.TaxonCombo.getRecord = function() {
	if ( Ext.Gemma.TaxonCombo.record === undefined ) {
		Ext.Gemma.TaxonCombo.record = Ext.data.Record.create( [
			{ name: "id", type: "int" },
			{ name: "commonName", type: "string" },
			{ name: "scientificName", type: "string" }
		] );
	}
	return Ext.Gemma.TaxonCombo.record;
};

Ext.Gemma.TaxonCombo.getTemplate = function() {
	if ( Ext.Gemma.TaxonCombo.template === undefined ) {
		Ext.Gemma.TaxonCombo.template = new Ext.XTemplate(
			'<tpl for="."><div class="x-combo-list-item">{commonName} ({scientificName})</div></tpl>'
		);
	}
	return Ext.Gemma.TaxonCombo.template;
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.TaxonCombo, Ext.form.ComboBox, {

	initComponent : function() {
        Ext.Gemma.TaxonCombo.superclass.initComponent.call(this);
        
        this.addEvents(
            'taxonchanged'
        );
    },
	
	onSelect : function ( record, index ) {
		Ext.Gemma.TaxonCombo.superclass.onSelect.call( this, record, index );
		
		if ( record.data != this.selectedTaxon ) {
			this.selectedTaxon = record.data;
			this.fireEvent( 'taxonchanged', this, this.selectedTaxon );
		}
	}, 
	
	getTaxon : function () {
		return this.selectedTaxon;
	},
	
	setTaxon : function ( taxon ) {
		this.setValue( taxon.id );
	}
	
} );

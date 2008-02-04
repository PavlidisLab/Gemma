Ext.namespace('Ext.Gemma');

/* Ext.Gemma.GeneCombo constructor...
 */
Ext.Gemma.GeneCombo = function ( config ) {
	Ext.QuickTips.init();
	
	this.taxonId = config.taxonId; delete config.taxonId;
	
	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'officialSymbol',
		loadingText : 'Searching...',
		minChars : 2,
		selectOnFocus : true,
		store : new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( GenePickerController.searchGenes ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.GeneCombo.getRecord() ),
			remoteSort : true
		} ),
		tpl : Ext.Gemma.GeneCombo.getTemplate()
	};
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.GeneCombo.superclass.constructor.call( this, superConfig );
	
	this.on( "select", function ( combo, record, index ) {
		this.selectedGene = record.data;
	} );
};

/* static methods
 */
Ext.Gemma.GeneCombo.getRecord = function() {
	if ( Ext.Gemma.GeneCombo.record === undefined ) {
		Ext.Gemma.GeneCombo.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"taxon", type:"string", convert: function ( t ) { return t.scientificName; } },
			{ name:"officialSymbol", type:"string" },
			{ name:"officialName", type:"string" }
		] );
	}
	return Ext.Gemma.GeneCombo.record;
};

Ext.Gemma.GeneCombo.getTemplate = function() {
	if ( Ext.Gemma.GeneCombo.template === undefined ) {
		Ext.Gemma.GeneCombo.template = new Ext.XTemplate(
			'<tpl for="."><div ext:qtip="{officialName}" class="x-combo-list-item">{officialSymbol} ({taxon})</div></tpl>'
		);
	}
	return Ext.Gemma.GeneCombo.template;
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.GeneCombo, Ext.form.ComboBox, {

	getParams : function ( query ) {
			return [ query, this.taxonId || -1 ];
	},
	
	getGene : function () {
		return this.selectedGene;
	},
	
	setTaxonId : function ( id ) {
		this.taxonId = id;
	}
	
} );

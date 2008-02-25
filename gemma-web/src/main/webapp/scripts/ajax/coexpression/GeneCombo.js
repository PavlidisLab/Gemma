Ext.namespace('Ext.Gemma');

/* Ext.Gemma.GeneCombo constructor...
 */
Ext.Gemma.GeneCombo = function ( config ) {
	Ext.QuickTips.init();
	
	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'officialSymbol',
		valueField : 'id',
		loadingText : 'Searching...',
		minChars : 2,
		selectOnFocus : true,
		store : new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( GenePickerController.searchGenes ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.GeneCombo.getRecord() ),
			sortInfo : { field: "officialSymbol", dir: "ASC" }
		} ),
		tpl : Ext.Gemma.GeneCombo.getTemplate()
	};
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.GeneCombo.superclass.constructor.call( this, superConfig );
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

	onSelect : function ( record, index ) {
		Ext.Gemma.GeneCombo.superclass.onSelect.call( this, record, index );
		
		if ( record.data != this.selectedGene ) {
			this.setGene( record.data );
		}
	},
	
	getParams : function ( query ) {
			return [ query, this.taxon ? this.taxon.id : -1 ];
	},
	
	getGene : function () {
		return this.selectedGene;
	},
	
	setGene : function ( gene ) {
		this.selectedGene = gene;
		if ( this.tooltip ) {
			this.tooltip.destroy();
		}
		if ( gene ) {
			this.tooltip = new Ext.ToolTip( {
				target: this.getEl(),
				html: String.format( '{0} ({1})', gene.officialName || "no description", gene.taxon )
			} );
		}
	},
	
	setTaxon : function ( taxon ) {
		this.taxon = taxon;
		if ( this.selectedGene && this.selectedGene.taxon != taxon.scientificName ) {
			this.setGene( null );
			this.reset();
			this.lastQuery = ''; // force a reload with the new taxon...
		}
	}
	
} );

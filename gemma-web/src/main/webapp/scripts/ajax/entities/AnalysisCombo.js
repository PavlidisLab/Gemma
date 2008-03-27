/*
 * Combo that shows lists of coexpression analyses. 
 */

/* Ext.Gemma.AnalysisCombo constructor...
 */
Ext.Gemma.AnalysisCombo = function ( config ) {
	Ext.QuickTips.init();
	
	this.showCustomOption = config.showCustomOption; delete config.showCustomOption;
	this.isStoreLoaded = false;
	
	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'name',
		valueField : 'id',
		disabled : true,
		editable : false,
		forceSelection : true,
		lazyInit : false,
		lazyRender : false,
		mode : 'local',
		selectOnFocus : true,
		triggerAction : 'all',
		store : new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( ExtCoexpressionSearchController.getCannedAnalyses ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.AnalysisCombo.getRecord() ),
			remoteSort : true
		} ),
		tpl : Ext.Gemma.AnalysisCombo.getTemplate()
	};
	var options = { params : [] };
	if ( this.showCustomOption ) {
		options.callback = function () {
			var Constructor = Ext.Gemma.AnalysisCombo.getRecord();
			var record = new Constructor( {
				id : -1,
				name : "Custom analysis",
				description : "Select specific datasets to search against"
			} );
			superConfig.store.add( record );
			this.storeLoaded();
		}.bind( this );
	} else {
		options.callback = function () {
			this.storeLoaded();
		}.bind( this );
	}
	superConfig.store.load( options );
	
	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.AnalysisCombo.superclass.constructor.call( this, superConfig );
	
	// call doQuery or the record filtering done in taxonChanged() below doesn't work...
	this.doQuery();
};



/* static methods
 */
Ext.Gemma.AnalysisCombo.getRecord = function() {
	if ( Ext.Gemma.AnalysisCombo.record === undefined ) {
		Ext.Gemma.AnalysisCombo.record = Ext.data.Record.create( [
			{ name: "id", type: "int" },
			{ name: "name", type: "string" },
			{ name: "description", type: "string" },
			{ name: "taxon" },
			{ name: "numDatasets", type: "int" },
			{ name: "datasets" }
		] );
	}
	return Ext.Gemma.AnalysisCombo.record;
};



Ext.Gemma.AnalysisCombo.getTemplate = function() {
	if ( Ext.Gemma.AnalysisCombo.template === undefined ) {
		Ext.Gemma.AnalysisCombo.template = new Ext.XTemplate(
			'<tpl for="."><div ext:qtip="{description}" class="x-combo-list-item">{name}{[ values.taxon ? " (" + values.taxon.scientificName + ")" : "" ]}</div></tpl>'
		);
	}
	return Ext.Gemma.AnalysisCombo.template;
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.AnalysisCombo, Ext.form.ComboBox, {

	initComponent : function() {
        Ext.Gemma.AnalysisCombo.superclass.initComponent.call(this);
        
        this.addEvents(
            'analysischanged'
        );
    },
    
    render : function ( container, position ) {
		Ext.Gemma.AnalysisCombo.superclass.render.apply(this, arguments);
    	
		if ( ! this.isStoreLoaded ) {
			this.setRawValue( "Loading..." );
		}
	},

	onSelect : function ( record, index ) {
		Ext.Gemma.AnalysisCombo.superclass.onSelect.call( this, record, index );
		
		if ( record.data != this.selectedAnalysis ) {
			this.analysisChanged( record.data );
		}
	},
	
	reset : function() {
		Ext.Gemma.AnalysisCombo.superclass.reset.call(this);
		
		if ( this.selectedAnalysis !== null ) {
			this.analysisChanged( null );
		}
	},
	
	setValue : function( v ) {
		if ( this.isStoreLoaded ) {
			Ext.Gemma.AnalysisCombo.superclass.setValue.call( this, v );
			
			var r = this.findRecord( this.valueField, v );
			this.analysisChanged( r ? r.data : null );
		} else {
			this.delayedSetValue = v;
		}
	},
	
	storeLoaded : function() {
		this.isStoreLoaded = true;
		if ( this.delayedSetValue !== undefined && this.delayedSetValue !== "" ) {
			this.setValue( this.delayedSetValue );
		} else {
			this.reset(); // clear the loading message...
		}
		this.enable();
	},
	
	analysisChanged : function ( analysis ) {
		this.selectedAnalysis = analysis;
		this.fireEvent( 'analysischanged', this, this.selectedAnalysis );
	},

	taxonChanged : function ( taxon ) {
		if ( this.selectedAnalysis && this.selectedAnalysis.taxon.id != taxon.id ) {
			this.reset();
		}
		this.store.filterBy( function( record, id ) {
			if ( ! record.data.taxon ) {
				return true; 
			} else if ( record.data.taxon.id == taxon.id ) {
				return true;
			}
		} );
	}
	
} );
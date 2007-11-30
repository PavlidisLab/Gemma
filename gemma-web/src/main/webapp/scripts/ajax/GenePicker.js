function GenePicker(div, result) {
	var GeneRecord;
	function getGeneRecord() {
		if ( true ) {
			GeneRecord = Ext.data.Record.create( [
				{ name: "id", type: "int" },
				{ name: "name", type: "string" },
				{ name: "officialSymbol", type: "string" },
				{ name: "officialName", type: "string" }
			] );
		}
		return GeneRecord;
	}
	
	var TaxonRecord;
	function getTaxonRecord() {
		if ( true ) {
			TaxonRecord = Ext.data.Record.create( [
				{ name: "id", type: "int" },
				{ name: "abbreviation", type: "string" },
				{ name: "commonName", type: "string" },
				{ name: "scientificName", type: "string" }
			] );
		}
		return TaxonRecord;
	}
	
	return {
		div : div,
		result : result,
		memStore : [],
		geneGridDs : null,
		geneGrid : null,
		geneComboDs : null,
		geneCombo : null,
		taxonComboDs : null,
		taxonCombo : null,
		saveButton : null,
		deleteButton : null,
		selectedGene : null,
		populateHiddenField : function() {
			value = '';
			this.geneGridDs.each( function( record ) {
				value = value + record.data.id + ',';
			} );
			if ( value.length > 0 ) {
				this.result.value = value.substring(0, value.length-1);
			}
		},
		geneSelected : function( record ) {
			this.selectedGene = record;
			this.geneCombo.collapse();
			this.saveButton.enable();
		},
		saveSelected : function() {
			this.saveButton.disable();
			this.deleteButton.enable();
			this.geneCombo.reset();
			this.geneGridDs.add( this.selectedGene );
			this.populateHiddenField();
			this.geneGrid.getView().refresh( true );
		},
		deleteSelected : function() {
			var records = this.geneGrid.getSelectionModel().getSelections();
			if ( records.length > 0 ) {
				for ( i=0; i<records.length; ++i ) {
					this.geneGridDs.remove( records[i] );
				}
				if ( this.geneGridDs.getTotalCount() == 0 ) {
					this.deleteButton.disable();
				}
				this.populateHiddenField();
			}
		},
		getSearchParams : function( query ) {
			return [ query, this.taxonCombo.getValue() ];
		},
		init : function() {
			
			this.taxonComboDs = new Ext.data.Store( {
				proxy : new Ext.data.DWRProxy( GenePickerController.getTaxa ),
				reader : new Ext.data.ListRangeReader( { id: "id" }, getTaxonRecord() )
			} );
			this.taxonComboDs.load();
			this.taxonCombo = new Ext.form.ComboBox( {
		        fieldLabel : 'Taxon',
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
			
			this.geneComboDs = new Ext.data.Store( {
				proxy : new Ext.data.DWRProxy( GenePickerController.searchGenes ),
				reader : new Ext.data.ListRangeReader( {id:"id"}, getGeneRecord() ),
		        remoteSort : true 
			} );
			this.geneCombo = new Ext.form.ComboBox( {
		        fieldLabel : 'Gene',
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
			
			this.geneGridDs = new Ext.data.Store( {
				proxy : new Ext.data.MemoryProxy( this.memStore ),
				reader : new Ext.data.ArrayReader( {}, getGeneRecord() )
			} );
			this.geneGrid = new Ext.grid.Grid( this.div, {
				ds : this.geneGridDs,
				cm : new Ext.grid.ColumnModel( [
					{ header: "Name", dataIndex: "name" },
					{ header: "Official Symbol", dataIndex: "officialSymbol" },
					{ header: "Official Name", dataIndex: "officialName", id: 'name' }
				] ),
				loadMask : true,
				autoExpandColumn : 'name'
			} );
			this.geneGrid.render();
			
			var toolbar = new Ext.Toolbar( this.geneGrid.getView().getHeaderPanel( true) );
			toolbar.add( this.taxonCombo );
			toolbar.add( this.geneCombo );
			this.saveButton = toolbar.addButton( {
				text : 'Add to query',
				tooltip : 'Add the selected gene to the list of genes to search for',
				handler : this.saveSelected.bind( this ),
				disabled : true
			} );
			this.deleteButton = toolbar.addButton( {
				text : 'Remove from query',
				tooltip : 'Remove the selected gene to the list of genes to search for',
				handler : this.deleteSelected.bind( this ),
				disabled : true
			} );
		}
	};
}

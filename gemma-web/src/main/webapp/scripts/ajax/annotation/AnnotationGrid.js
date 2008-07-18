Ext.namespace('Gemma');

/**
 * The 'Characteristic browser' grid, also used for the basic Annotation view.
 * 
 * Gemma.AnnotationGrid constructor... div is the name of the div in which to render the grid. config is a hash with the
 * following options: readMethod : the DWR method that returns the list of AnnotationValueObjects ( e.g.:
 * ExpressionExperimentController.getAnnotation ) readParams : an array of parameters that will be passed to the
 * readMethod ( e.e.: [ { id:x, classDelegatingFor:"ExpressionExperimentImpl" } ] ) or a pointer to a function that will
 * return the array of parameters editable : if true, the annotations in the grid will be editable showParent : if true,
 * a link to the parent object will appear in the grid noInitialLoad : if true, the grid will not be loaded immediately
 * upon creation pageSize : if defined, the grid will be paged on the client side, with the defined page size
 */
Gemma.AnnotationGrid = Ext.extend(Gemma.GemmaGridPanel, {

	autoHeight : true,
	width : 500,
	maxHeight : 200,
	loadMask : true,

	viewConfig : {
		enableRowBody : true,
		showDetails : false,
		getRowClass : function(record, index, p, store) {
			if (this.showDetails) {
				p.body = "<p class='characteristic-body' >" + String.format("From {0}", record.data.parentOfParentLink)
						+ "</p>";
			}
			return '';
		}
	},

	useDefaultToolbar : true,

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "classUri",
		type : "string"
	}, {
		name : "className",
		type : "string"
	}, {
		name : "termUri",
		type : "string"
	}, {
		name : "termName",
		type : "string"
	}, {
		name : "parentLink",
		type : "string"
	}, {
		name : "parentDescription",
		type : "string"
	}, {
		name : "parentOfParentLink",
		type : "string"
	}, {
		name : "parentOfParentDescription",
		type : "string"
	}, {
		name : "evidenceCode",
		type : "string"
	}]),

	parentStyler : function(value, metadata, record, row, col, ds) {
		return this.formatParentWithStyle(record.id, record.expanded, record.data.parentLink,
				record.data.parentDescription, record.data.parentOfParentLink, record.data.parentOfParentDescription);
		// return parentLink;
	},

	formatParentWithStyle : function(id, expanded, parentLink, parentDescription, parentOfParentLink,
			parentOfParentDescription) {
		var value;
		// if (parentOfParentLink) {
		// value = String.format("{0}<br> from {1}", parentLink, parentOfParentLink);
		// } else {
		value = parentLink;
		// }
		return expanded
				? value.concat(String.format("<div style='white-space: normal;'>{0}</div>", parentDescription))
				: value;
	},

	termStyler : function(value, metadata, record, row, col, ds) {
		return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.data.termUri);
	},

	convertToCharacteristic : function(record) {
		var c = {
			id : record.id,
			category : record.className,
			value : record.termName
		};
		/*
		 * if we don't have a valueURI set, don't return URI fields or a VocabCharacteristic will be created when we
		 * only want a Characteristic...
		 */
		if (record.termUri || record.classUri) {
			c.categoryUri = record.classUri;
			c.valueUri = record.termUri;
		}
		return c;
	},

	initComponent : function() {

		Ext.apply(this, {
			columns : [{
				header : "Class",
				dataIndex : "className"
			}, {
				header : "Term",
				dataIndex : "termName",
				renderer : this.termStyler.createDelegate(this)
			}, {
				header : "Annotation belongs to:",
				dataIndex : "parentLink",
				renderer : this.parentStyler.createDelegate(this),
				hidden : this.showParent ? false : true
			}, {
				header : "Evidence",
				dataIndex : "evidenceCode"
			}]

		});

		if (this.pageSize) {
			Ext.apply(this, {
				store : new Gemma.PagingDataStore({
					proxy : new Ext.data.DWRProxy(this.readMethod),
					reader : new Ext.data.ListRangeReader({
						id : "id"
					}, this.record),
					pageSize : this.pageSize
				})
			});
			Ext.apply(this, {
				bbar : new Gemma.PagingToolbar({
					pageSize : this.pageSize,
					store : this.getStore()
				})
			});

		} else {
			Ext.apply(this, {
				store : new Ext.data.Store({
					proxy : new Ext.data.DWRProxy(this.readMethod),
					reader : new Ext.data.ListRangeReader({
						id : "id"
					}, this.record)
				})
			});
		}

		if (this.editable && this.useDefaultToolbar) {
			Ext.apply(this, {
				tbar : new Gemma.AnnotationToolBar({
					annotationGrid : this,
					createHandler : function(characteristic, callback) {
						OntologyService.saveExpressionExperimentStatement(characteristic, [this.eeId], callback);
					},
					deleteHandler : function(ids, callback) {
						OntologyService.removeExpressionExperimentStatement(ids, [this.eeId], callback);
					},
					mgedTermKey : "experiment"
				})
			});
		}
		Gemma.AnnotationGrid.superclass.initComponent.call(this);

		this.getStore().setDefaultSort('className');

		this.autoExpandColumn = this.showParent ? 2 : 1;

		this.getColumnModel().defaultSortable = true;
		if (this.editable) {

			var CATEGORY_COLUMN = 0;
			var VALUE_COLUMN = 1;
			var PARENT_COLUMN = 2;
			var EVIDENCE_COLUMN = 3;
			this.categoryCombo = new Gemma.MGEDCombo({
				lazyRender : true,
				termKey : this.mgedTermKey
			});
			var categoryEditor = new Ext.grid.GridEditor(this.categoryCombo);
			this.categoryCombo.on("select", function(combo, record, index) {
				categoryEditor.completeEdit();
			});
			this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);

			this.valueCombo = new Gemma.CharacteristicCombo({
				lazyRender : true
			});
			var valueEditor = new Ext.grid.GridEditor(this.valueCombo);
			this.valueCombo.on("select", function(combo, record, index) {
				valueEditor.completeEdit();
			});
			this.getColumnModel().setEditor(VALUE_COLUMN, valueEditor);

			this.on("beforeedit", function(e) {
				var row = e.record.data;
				var col = this.getColumnModel().getColumnId(e.column);
				if (col == VALUE_COLUMN) {
					this.valueCombo.setCategory.call(this.valueCombo, row.className, row.classUri);
				}
			});
			this.on("afteredit", function(e) {
				var col = this.getColumnModel().getColumnId(e.column);
				if (col == CATEGORY_COLUMN) {
					var term = this.categoryCombo.getTerm.call(this.categoryCombo);
					e.record.set("className", term.term);
					e.record.set("classUri", term.uri);
				} else if (col == VALUE_COLUMN) {
					var c = this.valueCombo.getCharacteristic.call(this.valueCombo);
					e.record.set("termName", c.value);
					e.record.set("termUri", c.valueUri);
				}
				this.getView().refresh();
			});
		}

		this.on("celldblclick", function(grid, rowIndex, cellIndex) {
			var record = grid.getStore().getAt(rowIndex);
			var column = grid.getColumnModel().getColumnId(cellIndex);
			if (column == PARENT_COLUMN) {
				record.expanded = record.expanded ? 0 : 1;
				grid.getView().refresh(true);
			}
		}, this);

		this.getStore().on("load", function() {
			this.doLayout();
		}, this);

		if (!this.noInitialLoad) {
			this.getStore().load({
				params : this.getReadParams()
			});
		}
	},

	getReadParams : function() {
		return (typeof this.readParams == "function") ? this.readParams() : this.readParams;
	},

	getSelectedCharacteristics : function() {
		var selected = this.getSelectionModel().getSelections();
		var chars = [];
		for (var i = 0; i < selected.length; ++i) {
			var row = selected[i].data;
			chars.push(this.convertToCharacteristic(row));
		}
		return chars;
	},

	getEditedCharacteristics : function() {
		var chars = [];
		this.getStore().each(function(record) {
			if (record.dirty) {
				var row = record.data;
				chars.push(this.convertToCharacteristic(row));
			}
		}.createDelegate(this), this);
		return chars;
	}

});
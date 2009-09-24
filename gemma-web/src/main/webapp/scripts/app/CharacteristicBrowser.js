Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.namespace('Gemma.CharacteristicBrowser');
Ext
		.onReady( function() {
			Ext.QuickTips.init();

			browsergrid = new Gemma.AnnotationGrid(
					{
						renderTo : "characteristicBrowser",
						readMethod : CharacteristicBrowserController.findCharacteristics,
						readParams : [],
						editable : true,
						tbar : new Ext.Toolbar(),
						useDefaultToolbar : false,
						showParent : true,
						viewConfig : {
							showDetails : true
						},
						width : 1200, 
						height : 500,
						noInitialLoad : true,
						pageSize : 20
					});

			Gemma.CharacteristicBrowser.handleError = function(msg, e) {
				Ext.DomHelper.overwrite("messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
				Ext.DomHelper.append("messages", {
					tag : 'span',
					html : "&nbsp;&nbsp;" + msg
				});
				browsergrid.loadMask.hide();
				saveButton.enable();
			};

			var pagingToolbar = browsergrid.getBottomToolbar();
			
			var charCombo = new Gemma.CharacteristicCombo();

			var searchButton = new Ext.Toolbar.Button(
					{
						text : "search",
						tooltip : "Find matching characteristics in the database",
						handler : function() {
							Ext.DomHelper.overwrite("messages", "");
							var query = charCombo.getCharacteristic().value;
							if (!query) {
								Ext.DomHelper.overwrite("messages",
										"Please enter a query");
								return;
							}
							browsergrid.loadMask.msg = "Searching ...";
							var searchEEs = eeCheckBox.getValue();
							var searchBMs = bmCheckBox.getValue();
							var searchFVs = fvCheckBox.getValue();
							var searchNos = noCheckBox.getValue();
							var searchFactorsValueValues = fvvCheckBox
									.getValue();
							browsergrid.refresh( [ query,
									searchNos, searchEEs, searchBMs, searchFVs,
									searchFactorsValueValues ]);
						}
					});

			var saveButton = new Ext.Toolbar.Button(
					{
						text : "save",
						tooltip : "Saves your changes to the database",
						disabled : true,
						handler : function() {
							saveButton.disable();
							browsergrid.loadMask.msg = "Saving ...";
							browsergrid.loadMask.show();
							Ext.DomHelper.overwrite("messages", "");
							var chars = browsergrid
									.getEditedCharacteristics();
							
//							var callback = browsergrid.getBottomToolbar().doRefresh
//									.createDelegate(browsergrid.getBottomToolbar());
									
							var callback = browsergrid.refresh.createDelegate(browsergrid);
							var errorHandler = Gemma.CharacteristicBrowser.handleError
									.createDelegate(this, [], true);
							CharacteristicBrowserController
									.updateCharacteristics(chars, {
										callback : callback,
										errorHandler : errorHandler
									});
						}
					});

			browsergrid.on("afteredit", function(e) {
				saveButton.enable();
			});
			
//			browsergrid.on("loadexception", function(e) {
//				Ext.DomHelper.overwrite("messages", "Sorry, there was an error.");
//			});

			var deleteButton = new Ext.Toolbar.Button(
					{
						text : "delete",
						tooltip : "Delete selected characteristics",
						disabled : true,
						handler : function() {
							Ext.DomHelper.overwrite("messages", "");
							browsergrid.loadMask.msg = "Deleting ...";
							browsergrid.loadMask.show();
							var chars = browsergrid
									.getSelectedCharacteristics();
							CharacteristicBrowserController
									.removeCharacteristics(
											chars,
											function() {

												/*
												 * remove the records from the
												 * data store manually instead
												 * of just refreshing so that we
												 * don't lose any edits that are
												 * in progress...
												 */
												var selected = browsergrid
														.getSelectionModel()
														.getSelections();
												for ( var i = 0; i < selected.length; ++i) {
													browsergrid
															.getStore()
															.remove(selected[i]);
												}
												browsergrid
														.getView().refresh();
												browsergrid.loadMask
														.hide();
											});

							// var callback =
							// browsergrid.refresh.bind(
							// browsergrid );
							// CharacteristicBrowserController.removeCharacteristics(
							// chars,
							// callback );
						}
					});
			browsergrid.getSelectionModel().on(
					"selectionchange", function(model) {
						var selected = model.getSelections();
						Ext.DomHelper.overwrite("messages", "");
						if (selected.length > 0) {
							deleteButton.enable();
						} else {
							deleteButton.disable();
						}
					});

			var revertButton = new Ext.Toolbar.Button( {
				text : "revert",
				tooltip : "Undo changes to selected characteristics",
				disabled : true,
				handler : function() {
					var selected = browsergrid
							.getSelectionModel().getSelections();
					for ( var i = 0; i < selected.length; ++i) {
						var record = selected[i];
						record.reject();
					}
					browsergrid.getView().refresh();
				}
			});
			browsergrid.getSelectionModel().on(
					"selectionchange", function(model) {
						var selected = model.getSelections();
						revertButton.disable();
						for ( var i = 0; i < selected.length; ++i) {
							if (selected[i].dirty) {
								revertButton.enable();
								break;
							}
						}
					});
			browsergrid.on("afteredit", function(e) {
				revertButton.enable();
			});

			var savedCharacteristic;
			var copyHandler = function() {
				var selected = browsergrid
						.getSelectionModel().getSelections();
				for ( var i = 0; i < selected.length; ++i) {
					var record = selected[i];
					savedCharacteristic = record.data;
					break;
				}
				pasteButton.enable();
				pasteCategoryButton.enable();
			};

			var copyButton = new Ext.Toolbar.Button( {
				text : "copy",
				tooltip : "Copy values from the selected characteristic",
				disabled : true,
				handler : copyHandler
			});

			browsergrid.getSelectionModel().on(
					"selectionchange", function(model) {
						var selected = model.getSelections();
						if (selected.length > 0) {
							copyButton.enable();
						} else {
							copyButton.disable();
						}
					});

			var pasteHandler = function() {
				var selected = browsergrid
						.getSelectionModel().getSelections();
				for ( var i = 0; i < selected.length; ++i) {
					var record = selected[i];
					record.set("classUri", savedCharacteristic.classUri);
					record.set("className", savedCharacteristic.className);
					record.set("termUri", savedCharacteristic.termUri);
					record.set("termName", savedCharacteristic.termName);
				}
				browsergrid.getView().refresh();
				saveButton.enable();
			};

			var pasteCategoryHandler = function() {
				var selected = browsergrid
						.getSelectionModel().getSelections();
				for ( var i = 0; i < selected.length; ++i) {
					var record = selected[i];
					record.set("classUri", savedCharacteristic.classUri);
					record.set("className", savedCharacteristic.className);
				}
				browsergrid.getView().refresh();
				saveButton.enable();
			};

			var toggleDetails = function(btn, e) {
				var view = browsergrid.getView();
				view.showDetails = btn.pressed;
				view.refresh();
			};

			var pasteButton = new Ext.Toolbar.Button(
					{
						text : "paste",
						tooltip : "Paste copied values onto the selected characteristics; both Class and Term will be updated.",
						disabled : true,
						handler : pasteHandler
					});

			var pasteCategoryButton = new Ext.Toolbar.Button(
					{
						text : "paste category",
						tooltip : "Paste copied Class values onto the selected characteristics. Term will be left alone.",
						disabled : true,
						handler : pasteCategoryHandler
					});

			var toggleDetailsButton = new Ext.Toolbar.Button( {
				text : "Details",
				enableToggle : true,
				tooltip : "Show/hide more information",
				disabled : false,
				handler : toggleDetails
			});

			browsergrid.on("keypress", function(e) {
				if (e.ctrlKey) {
					if (e.getCharCode() == 99) { // 'c'
						copyHandler();
					} else if (e.getCharCode() == 118) { // 'v'
						pasteHandler();
					}
				}
			});

			var eeCheckBox = new Ext.form.Checkbox( {
				boxLabel : 'Expression Experiments',
				checked : true,
				name : 'searchEEs',
				width : 'auto'
			});
			var bmCheckBox = new Ext.form.Checkbox( {
				boxLabel : 'BioMaterials',
				checked : true,
				name : 'searchBMs',
				width : 'auto'
			});
			var fvCheckBox = new Ext.form.Checkbox( {
				boxLabel : 'Factor Values',
				checked : true,
				name : 'searchFVs',
				width : 'auto'
			});

			var fvvCheckBox = new Ext.form.Checkbox( {
				boxLabel : 'Uncharacterized factor Values',
				checked : true,
				tooltip : 'Factor values that lack proper characteristics',
				name : 'searchFVVs',
				width : 'auto'
			});

			var noCheckBox = new Ext.form.Checkbox( {
				boxLabel : 'No parent',
				checked : true,
				name : 'searchNos',
				width : 'auto'
			});

			var toolbar = browsergrid.getTopToolbar();

			toolbar.addField(charCombo);
			toolbar.addSpacer();
			toolbar.addField(searchButton);
			toolbar.addSeparator();
			toolbar.addField(saveButton);
			toolbar.addSeparator();
			toolbar.addField(deleteButton);
			toolbar.addSeparator();
			toolbar.addField(revertButton);
			toolbar.addSeparator();
			toolbar.addField(copyButton);
			toolbar.addSeparator();
			toolbar.addField(pasteButton);
			toolbar.addSeparator();
			toolbar.addField(pasteCategoryButton);
			toolbar.addFill();
			toolbar.add(toggleDetailsButton);

			/*
			 * toolbar.addSeparator(); toolbar.addField( testButton );
			 */

			var secondToolbar = new Ext.Toolbar(toolbar.getEl().createChild());
			secondToolbar.addSpacer();
			secondToolbar.addText("Find characteristics from");
			secondToolbar.addSpacer();
			secondToolbar.addField(eeCheckBox);
			secondToolbar.addSpacer();
			secondToolbar.addField(bmCheckBox);
			secondToolbar.addSpacer();
			secondToolbar.addField(fvCheckBox);
			secondToolbar.addSpacer();
			secondToolbar.addField(noCheckBox);
			secondToolbar.addSpacer();
			secondToolbar.addField(fvvCheckBox);

		});
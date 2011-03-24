Ext.namespace('Gemma');

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {

	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var pstore = new Gemma.BibRefPagingStore({
				autoLoad : {
					params : {
						start : 0,
						limit : 20
					}
				}
			});

	var bibRefGrid = new Ext.grid.GridPanel({
		renderTo : 'bibRefGrid',
		width : 1000,
		loadMask : true,
		autoHeight : true,
		store : pstore,
		bbar : new Ext.PagingToolbar({
					store : pstore, // grid and PagingToolbar using same store
					displayInfo : true,
					pageSize : 20
				}),
		colModel : new Ext.grid.ColumnModel({
			defaultSortable : true,
			columns : [{
				header : "Details",
				dataIndex : 'id',
				renderer : function(value, metaData, record) {
					return '<a href="/Gemma/bibRef/bibRefView.html?accession=' + record.get('pubAccession') +
							'"><img ext:qtip="View details in Gemma" src="/Gemma/images/icons/magnifier.png" /></a>';
				},
				width : 30,
				sortable : false
			}, {
				header : "Authors",
				dataIndex : 'authorList',
				width : 200
			}, {
				header : "Title",
				dataIndex : 'title',
				width : 300
			}, {
				header : "Publication",
				dataIndex : 'publication'
			}, {
				header : "Date",
				dataIndex : 'publicationDate',
				width : 50,
				renderer : Ext.util.Format.dateRenderer("Y")
			}, {
				header : "Pages",
				dataIndex : 'pages',
				width : 80,
				sortable : false
			}, {
				header : "Experiments",
				dataIndex : 'experiments',
				renderer : function(value) {
					var result = "";
					for (var i = 0; i < value.length; i++) {
						result = result + '&nbsp<a target="_blank" ext:qtip="View details of ' + value[i].shortName +
								' (' + value[i].name +
								')" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=' + value[i].id +
								'">' + value[i].shortName + '</a>';
					}
					return result;
				}

			}, {
				header : "PubMed",
				dataIndex : 'pubAccession',
				width : 60,
				renderer : function(value) {
					return '<a target="_blank" href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&list_uids=' +
							value +
							'&query_hl=3&dopt=Abstract"><img ext:qtip="View at NCBI PubMed"  src="/Gemma/images/pubmed.gif" width="47" height="15" /></a>';
				},
				sortable : false
			}]
		})

	}

	);

});

Gemma.BibRefPagingStore = Ext.extend(Ext.data.Store, {
			constructor : function(config) {
				Gemma.BibRefPagingStore.superclass.constructor.call(this, config);
			},
			remoteSort : true,
			proxy : new Ext.data.DWRProxy({
						apiActionToHandlerMap : {
							read : {
								dwrFunction : BibliographicReferenceController.browse,
								getDwrArgsFunction : function(request) {
									var params = request.params;
									return [params];
								}
							}
						}
					}),

			reader : new Ext.data.JsonReader({
						root : 'records', // required.
						successProperty : 'success', // same as default.
						messageProperty : 'message', // optional
						totalProperty : 'totalRecords', // default is 'total'; optional unless paging.
						idProperty : "id", // same as default
						fields : [{
									name : "id",
									type : "int"
								}, {
									name : "volume"
								}, {
									name : "title"
								}, {
									name : "publicationDate",
									type : 'date'
								}, {
									name : "publication"
								}, {
									name : "pubAccession"
								}, {
									name : "pages"
								}, {
									name : "citation"
								}, {
									name : "authorList"
								}, {
									name : "abstractText"
								}, {
									name : "experiments"
								}]
					}),

			writer : new Ext.data.JsonWriter({
						writeAllFields : true
					})

		});

function doUpdate(id) {
	var callParams = [];
	callParams.push(id);

	var delegate = updateDone.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
				callback : delegate,
				errorHandler : errorHandler
			});

	BibliographicReferenceController.update.apply(this, callParams);
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;Please wait..."
			});

};

function updateDone(data) {
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/ok.png'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;Updated"
			});
};

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/warning.png'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;There was an error: " + data
			});
};
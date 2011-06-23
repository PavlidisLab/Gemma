Ext.namespace('Gemma');

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function(){

    Ext.QuickTips.init();
    Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
    
    var title = new Ext.form.TextArea({
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Title',
        readOnly: true,
        width: 850,
        height: 40
    });
    
    var abstractBibli = new Ext.form.TextArea({
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Abstract',
        readOnly: true,
        width: 850,
        grow: true
    });
    
    var authors = new Ext.form.TextArea({
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Authors',
        readOnly: true,
        width: 850,
        height: 50
    });
    
    var publication = new Ext.form.TextField({
        enableKeyEvents: true,
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Publication',
        width: 850,
        readOnly: true
    });
    
    var date = new Ext.form.TextField({
        enableKeyEvents: true,
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Date',
        width: 850,
        readOnly: true
    });
    
    var pages = new Ext.form.TextField({
        enableKeyEvents: true,
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Pages',
        width: 850,
        readOnly: true
    });
    
    var experiments = new Ext.form.TextField({
        enableKeyEvents: true,
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Experiments',
        width: 850,
        readOnly: true
    
    });
    
    var citation = new Ext.form.TextField({
        enableKeyEvents: true,
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Citation',
        width: 850,
        readOnly: true
    });
    
    var pubmed = new Ext.form.TextField({
        enableKeyEvents: true,
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Pubmed',
        width: 850,
        readOnly: true
    });
    
    var mesh = new Ext.form.TextArea({
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Mesh',
        readOnly: true,
        width: 850,
        grow: true
    });
    
    var chemicals = new Ext.form.TextArea({
        disabledClass: 'disabled-plain',
        fieldClass: 'x-bare-field',
        fieldLabel: 'Chemicals',
        readOnly: true,
        width: 850,
        grow: true
    });
    
    var bibRefDet = new Ext.FormPanel({
        renderTo: 'bibRefDet',
        title: 'Bibliographic Reference Details',
        width: 1000,
        padding: 20,
        items: [title, abstractBibli, authors, publication, date, pages, citation, experiments, pubmed, mesh, chemicals]
    });
    
    // filter button
    var btnFilter = new Ext.CycleButton({
        showText: true,
        prependText: 'Filter by ',
        items: [{
            text: 'Authors',
            id: 'authorList',
            iconCls: 'view-text',
            checked: true
        }, {
            text: 'Title',
            id: 'title',
            iconCls: 'view-text'
        }, {
            text: 'PudMed ID',
            id: 'pubAccession',
            iconCls: 'view-text'
        }, {
            text: 'Mesh Terms',
            id: 'meshTerms',
            iconCls: 'view-text'
        
        }]
    });
    
    // textField that accept a filter from the user
    var searchInGridField = new Ext.form.TextField({
        enableKeyEvents: true,
        emptyText: 'Filter',
        listeners: {
            'keyup': function(){
                var txtValue = searchInGridField.getValue();
                pstore.clearFilter();
                
                if (txtValue.length > 1) {
                    pstore.filter(btnFilter.getActiveItem().id, txtValue, true, false);
                }
            }
        }
    });
    
    var filterToolbar = new Ext.Toolbar({
        items: [btnFilter, searchInGridField]
    });
    
    var pstore = new Gemma.BibRefPagingStore({
        autoLoad: {
            params: {
                start: 0,
                limit: 20
            }
        }
    });
    
    var bibRefGrid = new Ext.grid.GridPanel({
        renderTo: 'bibRefGrid',
        width: 1000,
        loadMask: true,
        autoHeight: true,
        store: pstore,
        
        sm: new Ext.grid.RowSelectionModel({
            singleSelect: true,
            listeners: {
            
                // when a row is selected trigger an action: populate details about the selected row
                rowselect: function(sm, index, record){
                
                    title.setValue(record.get('title'));
                    abstractBibli.setValue(record.get('abstractText'));
                    authors.setValue(record.get('authorList'));
                    publication.setValue(record.get('publication'));
                    date.setValue(record.get('publicationDate').format('F j, Y'));
                    title.setValue(record.get('title'));
                    citation.setValue(record.get('citation'));
                    
                    var allExperiments = '';
                    
                    for (var i = 0; i <
                    record.get('experiments').length; i++) {
                        allExperiments += record.get('experiments')[i].shortName +
                        " : ";
                        allExperiments += record.get('experiments')[i].name +
                        "\n";
                    }
                    experiments.setValue(allExperiments);
                    
                    var allMeshTerms = "";
                    
                    for (var i = 0; i <
                    record.get('meshTerms').length; i++) {
                    
                        allMeshTerms += record.get('meshTerms')[i] +
                        "\n";
                    }
                    pubmed.setValue(record.get('pubAccession'));
                    mesh.setValue(allMeshTerms);
                    
                    var allChemicalsTerms = "";
                    
                    for (var i = 0; i <
                    record.get('chemicalsTerms').length; i++) {
                        allChemicalsTerms += record.get('chemicalsTerms')[i] +
                        "\n";
                    }
                    chemicals.setValue(allChemicalsTerms);
                }
            }
        }),
        
        bbar: new Ext.PagingToolbar({
            store: pstore, // grid and PagingToolbar using same
            // store
            displayInfo: true,
            pageSize: 20
        }),
        colModel: new Ext.grid.ColumnModel({
            defaultSortable: true,
            columns: [{
                header: "Authors",
                dataIndex: 'authorList',
                width: 215
            }, {
                header: "Title",
                dataIndex: 'title',
                width: 350
            }, {
                header: "Publication",
                dataIndex: 'publication',
                width: 135
            }, {
                header: "Date",
                dataIndex: 'publicationDate',
                width: 70,
                renderer: Ext.util.Format.dateRenderer("Y")
            }, {
                header: "Pages",
                dataIndex: 'pages',
                width: 80,
                sortable: false
            }, {
                header: "Experiments",
                dataIndex: 'experiments',
                width: 80,
                renderer: function(value){
                    var result = "";
                    for (var i = 0; i < value.length; i++) {
                        result = result +
                        '&nbsp<a target="_blank" ext:qtip="View details of ' +
                        value[i].shortName +
                        ' (' +
                        value[i].name +
                        ')" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=' +
                        value[i].id +
                        '">' +
                        value[i].shortName +
                        '</a>';
                    }
                    return result;
                }
                
            }, {
                header: "PubMed",
                dataIndex: 'pubAccession',
                width: 70,
                renderer: function(value){
                    return '<a target="_blank" href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&list_uids=' +
                    value +
                    '&query_hl=3&dopt=Abstract"><img ext:qtip="View at NCBI PubMed"  src="/Gemma/images/pubmed.gif" width="47" height="15" /></a>';
                },
                sortable: false
            }]
        })
    
    });
    
	// when the grid loads select the first row by default
    pstore.on('load', function(){
        bibRefGrid.getSelectionModel().selectFirstRow();
    });
    
    var bibRefPanel = new Ext.Panel({
        width: 1000,
        renderTo: 'bibRefPanel',
        items: [filterToolbar, bibRefGrid]
    });
});

Gemma.BibRefPagingStore = Ext.extend(Ext.data.Store, {
    constructor: function(config){
        Gemma.BibRefPagingStore.superclass.constructor.call(this, config);
    },
    remoteSort: true,
    proxy: new Ext.data.DWRProxy({
        apiActionToHandlerMap: {
            read: {
                dwrFunction: BibliographicReferenceController.browse,
                getDwrArgsFunction: function(request){
                    var params = request.params;
                    return [params];
                }
            }
        }
    }),
    
    reader: new Ext.data.JsonReader({
        root: 'records', // required.
        successProperty: 'success', // same as default.
        messageProperty: 'message', // optional
        totalProperty: 'totalRecords', // default is 'total'; optional unless
        // paging.
        idProperty: "id", // same as default
        fields: [{
            name: "id",
            type: "int"
        }, {
            name: "volume"
        }, {
            name: "title"
        }, {
            name: "publicationDate",
            type: 'date'
        }, {
            name: "publication"
        }, {
            name: "pubAccession"
        }, {
            name: "pages"
        }, {
            name: "citation"
        }, {
            name: "authorList"
        }, {
            name: "abstractText"
        }, {
            name: "experiments"
        }, {
            name: "meshTerms"
        }, {
            name: "chemicalsTerms"
        }]
    }),
    
    writer: new Ext.data.JsonWriter({
        writeAllFields: true
    })

});

function doUpdate(id){
    var callParams = [];
    callParams.push(id);
    
    var delegate = updateDone.createDelegate(this, [], true);
    var errorHandler = handleFailure.createDelegate(this, [], true);
    
    callParams.push({
        callback: delegate,
        errorHandler: errorHandler
    });
    
    BibliographicReferenceController.update.apply(this, callParams);
    Ext.DomHelper.overwrite("messages", {
        tag: 'img',
        src: '/Gemma/images/default/tree/loading.gif'
    });
    Ext.DomHelper.append("messages", {
        tag: 'span',
        html: "&nbsp;Please wait..."
    });
    
};

function updateDone(data){
    Ext.DomHelper.overwrite("messages", {
        tag: 'img',
        src: '/Gemma/images/icons/ok.png'
    });
    Ext.DomHelper.append("messages", {
        tag: 'span',
        html: "&nbsp;Updated"
    });
};

function handleFailure(data, e){
    Ext.DomHelper.overwrite("messages", {
        tag: 'img',
        src: '/Gemma/images/icons/warning.png'
    });
    Ext.DomHelper.append("messages", {
        tag: 'span',
        html: "&nbsp;There was an error: " + data
    });
};

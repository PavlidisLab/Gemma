Ext.namespace('Gemma');

/**
 *
 */
Gemma.GenePhenotypes = Ext.extend(Gemma.GemmaGridPanel, {

    record: Ext.data.Record.create([{
        name: "databaseId"
    }, {
        name: "phenotypes"
    }, {
        name: "description"
    }, {
        name: "evidenceCode"
    }, {
        name: "primaryPublicationValueObject"
    }]),
    
    initComponent: function(){
    
        Ext.apply(this, {
            columns: [{
                header: "Phenotypes",
                dataIndex: "phenotypes",
                
                renderer: function(value, metaData, record, rowIndex, colIndex, store){
                    var display = '';
                    
                    var i;
                    for (i = 0; i < value.length; i++) {
                    
                        var uri = value[i].valueUri;
                        
                        if (uri!=null && uri.indexOf('#') !=
                        -1) {
                        
                            var link = "<a href=\"http://www.ebi.ac.uk/ontology-lookup/?termId=" +
                            uri.substring(uri.indexOf("#") +
                            1) +
                            "\">";
                            link = link.replace("_", ":");
                            
                            display += link +
                            value[i].value +
                            "</a><br \>";
                            
                        }
                        else {
                        
                            display += value[i].value +
                            "<br \>";
                        }
                        
                    }
                    return display;
                }
            }, {
                header: "Evidence Code",
                dataIndex: "evidenceCode"
            }, {
                header: "Type",
                
                renderer: function(){
                
                    return "Experimental Evidence";
                }
            }],
            
            store: new Ext.data.Store({
                proxy: new Ext.data.DWRProxy(GeneController.loadGeneEvidences),
                reader: new Ext.data.ListRangeReader({
                    id: "databaseId"
                }, this.record),
                remoteSort: false
            })
        });
        
        Gemma.GenePhenotypes.superclass.initComponent.call(this);
        
        this.getStore().load({
            params: [this.geneid]
        });
 
        
         this.getStore().on("load", function(store, records, options) {
        
         //console.log(records[0]);
        
         })
        

        
    }
    
});
Ext.reg('genephenotypes', Gemma.GenePhenotypes);

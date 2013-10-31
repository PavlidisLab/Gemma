Ext.namespace('Gemma.BibliographicReference');

Gemma.BibliographicReference.Browser = Ext.extend(Ext.Panel, {
   // would use vbox, but then user can't resize panels
   layout : 'border',
   initComponent : function() {

      Gemma.BibliographicReference.Browser.superclass.initComponent.call(this);

      var grid = new Gemma.BibliographicReference.SearchResultGrid({ region : 'center',
         title : 'Bibliographic References' });

      var details = new Gemma.BibliographicReference.DetailsPanel({ region : 'south', split : true, title : '', // We
      // don't
      // need
      // title.
      collapsible : true, collapsed : true });

      this.add(grid);
      this.add(details);
      this.doLayout();

      grid.on('bibRefSelected', function(record) {
         details.expand();
         details.updateFields(record);
      }, this);

      grid.on('runPubmedSearch', function() {
         details.clear();
      }, this);

      grid.on('runKeywordSearch', function() {
         details.clear();
      }, this);

      this.on('render', function() {
         details.setHeight(Ext.getBody().getViewSize().height / 2);
      }, this);
   }

});
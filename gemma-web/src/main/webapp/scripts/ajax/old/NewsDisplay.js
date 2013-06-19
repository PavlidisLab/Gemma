Ext.namespace("Gemma");

Gemma.NewsDisplay = Ext.extend(Ext.Panel, {

      autoHeight : true,
      baseCls : 'x-plain-panel',
      initComponent : function() {

         /*
          * I don't know why, but if you just initialize items outside of initcomponent it fails.
          */
         Ext.apply(this, {
               items : [

               new Ext.DataView({

                     autoHeight : true,
                     emptyText : 'No news',
                     loadingText : 'Loading news ...',
                     itemSelector : 'news',

                     store : new Ext.data.Store({
                           proxy : new Ext.data.DWRProxy(FeedReader.getLatestNews),
                           reader : new Ext.data.JsonReader({
                                 fields : [{
                                       name : "title"
                                    }, {
                                       name : "date",
                                       type : "date",
                                       convert : function(v, rec) {
                                          return Ext.util.Format.date(v, "M d y");
                                       }
                                    }, {
                                       name : "body"
                                    }, {
                                       name : "teaser"
                                    }]
                              }),
                           autoLoad : true
                        }),

                     tpl : new Ext.XTemplate(' <tpl for="."><div class="news"><div class="roundedcornr_box_962327">'
                        + '<div class="roundedcornr_top_962327"> <div></div>	</div> <div class="roundedcornr_content_962327">'
                        + '<h3>{title}</h3>{body}<div style="font-size:smaller">Posted: {date}</div><div class="roundedcornr_bottom_962327"> <div></div> </div></div></tpl>')

                  })]
            });
         Gemma.NewsDisplay.superclass.initComponent.call(this);

      }

   });

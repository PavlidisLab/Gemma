<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Widget tests</title>
<style type="text/css">
    .widget {
        padding: 10px;
        margin: 5px;
        background-color: #DDDDDD;
    }
</style>
</head>

<div class="padded">

    <h2>EE Page</h2>
    <div class="widget" id="eepage"></div>

    <h1>ComboBoxes</h1>
    <h2>DatasetGroup combo</h2>
    <div class="widget" id="eesetcombo"></div>

    <h2>DatasetGroupComboPanel</h2>
    <div class="widget" id="eesetpanel"></div>
    <h2>TaxonCombo</h2>
    <div class="widget" id="taxoncombo"></div>


    <h2>FactorValueCombo</h2>
    <div class="widget" id="factorValueCombo"></div>


    <h2>Dataset search field</h2>
    <div class="widget" id="datasetsearchfield"></div>
    <div id="dsresults"></div>


    <h2>CharacteristicCombo</h2>
    <div class="widget" id="charCombo"></div>

    <h2>Gene combo</h2>
    <div class="widget" id="genecombo"></div>

    <h2>Gene Group Combo</h2>
    <div class="widget" id="genegroupcombo"></div>


    <h2>MGEDCombo</h2>
    <div class="widget" id="mgedcombo"></div>
    <h2>ArrayDesignCombo</h2>
    <div class="widget" id="adCombo"></div>

    <h1>Panels</h1>

    <h2>GeneChooserPanel</h2>
    <div class="widget" id="genepicker"></div>


    <h2>DatasetGroupGridPanel with DatasetGroupEditToolbar</h2>
    <div class="widget" id="datasetGroupGrid"></div>
    <h2>ExpressionExperiment Grid</h2>
    <div class="widget" id="eegrid"></div>


    <h2>AuditTrailGrid</h2>
    <div class="widget" id="atGrid"></div>

    <h2>FilesUpload</h2>
    <div class="widget" id="fileUpload"></div>


    <h2>Visualization</h2>
    <div class="widget" id="visualization"></div>

    <h2>ProgressWidget</h2>
    <div class="widget" id="progressWidget"></div>


    <h2>AnalysisResultsSearch</h2>
    <div id="analysis-results-search-form" align="center"></div>
    <br>
    <div id="analysis-results-search-form-messages"></div>
    <div id="analysis-results-search-form-results"></div>

    <h2>Widget</h2>
    <div class="widget" id=""></div>

    <h2>Font-awesome test</h2>
    <p>
        <i class="fa fa-camera-retro fa-lg"></i>fa-camera-retro
    </p>
</div>

<script>
Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

Ext.onReady( function() {
   Ext.QuickTips.init();

   new Gemma.DatasetGroupCombo( {
      renderTo : 'eesetcombo'
   } );

   new Gemma.ExpressionExperimentPage( {
      renderTo : 'eepage',
      eeId : '2763'
   } );

   new Gemma.DatasetGroupComboPanel( {
      renderTo : 'eesetpanel'
   } );

   let k = new Gemma.DatasetSearchField( {
      renderTo : 'datasetsearchfield',
      taxon : {
         id : 1
      },
      initQuery : "liver"
   } );

   k.on( 'aftersearch', function( f, results ) {
      Ext.DomHelper.overwrite( 'dsresults', results.length + " found." );
   } );

   new Gemma.GeneCombo( {
      renderTo : 'genecombo'
   } );

   new Gemma.GeneGroupCombo( {
      renderTo : 'genegroupcombo'
   } );

   new Gemma.TaxonCombo( {
      renderTo : 'taxoncombo'
   } );

   new Gemma.GeneGrid( {
      renderTo : 'genepicker',
      height : 200
   } );

   new Gemma.DatasetGroupGridPanel( {
      renderTo : 'datasetGroupGrid',
      width : 750,
      tbar : new Gemma.DatasetGroupEditToolbar()
   } );

   new Gemma.ExpressionExperimentGrid( {
      renderTo : 'eegrid',
      width : 400,
      height : 200,
      eeids : [ 1, 2, 3, 4, 5, 6 ],
      rowExpander : true
   } );

   new Gemma.ArrayDesignCombo( {
      renderTo : 'adCombo'
   } );

   new Gemma.AuditTrailGrid( {
      renderTo : 'atGrid',
      auditable : {
         classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperiment",
         id : 1
      },
      height : 200,
      width : 520
   } );

   new Gemma.FactorValueCombo( {
      efId : 1,
      renderTo : 'factorValueCombo'
   } );

   new Gemma.FileUploadForm( {
      title : 'Upload your data file',
      renderTo : 'fileUpload'
   } );

   new Gemma.CharacteristicCombo( {
      renderTo : 'charCombo',
      listeners : {
         afterrender : function( d ) {
            d.focus();
            d.setValue( "urinary" );
            d.getStore().load( {
               params : [ 'urinary' ],
               callback : function( rec, op, success ) {
                  d.focus();
                  d.expand();
                  d.select( 1 );
               }
            } )

         },
      }
   } );
   /*

    var v = new Gemma.ProgressWidget({renderTo : 'progressWidget', width : 400});
    v.allMessages = "Here are some messages for you, widget test.";


    var testdata = Gemma.testVisData();
    var viswin = new Gemma.VisualizationWithThumbsWindow ({
    renderTo : 'visualization',	admin : false,
    id : 'viswin',
    title : "This is a test of the visualization widget",
    tpl : Gemma.getProfileThumbnailTemplate(false, false),
    store : new Gemma.VisualizationStore({
    proxy : new Ext.data.MemoryProxy(testdata)
    })
    }) ;
    viswin.show({});
    */
} );
</script>

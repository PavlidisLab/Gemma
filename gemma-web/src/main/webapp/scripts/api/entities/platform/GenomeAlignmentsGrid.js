Ext.namespace( 'Gemma' );

Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

Gemma.UCSC_ICON = Gemma.CONTEXT_PATH + "/images/logo/ucsc-logo.png";
Gemma.NCBI_ICON = Gemma.CONTEXT_PATH + "/images/logo/ncbi-symbol.svg";
const UCSC_TRACKS = 'https://genome.ucsc.edu/cgi-bin/hgTracks';
/**
 * 
 * @class Gemma.GenomeAlignmentsGrid; based on old ProbeDetailsGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.GenomeAlignmentsGrid = Ext.extend( Ext.grid.GridPanel, {

   loadMask : {
      msg : "Loading details ..."
   },

   autoExpandColumn : 'alignment',
   autoScroll : true,
   stateful : false,

   record : Ext.data.Record.create( [ {
      name : "identity",
      type : "float"
   }, {
      name : "score",
      type : "float"
   }, {
      name : "blatResult"
   }, {
      name : "compositeSequence"
   }, {
      name : "geneProductIdMap"
   }, {
      name : "geneProductIdGeneMap"
   } ] ),

   /**
    * @memberOf Gemma.GenomeAlignmentsGrid
    */
   numberformat : function( d ) {
      return Math.round( d * 100 ) / 100;
   },

   gpMapRender : function( data ) {
      var res = "";
      for ( var i in data) {
         if ( data[i].name ) {
            res = res + data[i].name + "<br />";
         }
      }
      return res;
   },

   geneMapRender : function( data ) {
      var res = "";

      for ( var i in data) {
         if ( data[i].id ) {
            res = res
               + "<a title='View gene details (opens new window)' target='_blank' href='" + Gemma.CONTEXT_PATH + "/gene/showGene.html?id="
               + data[i].id + "'>" + data[i].officialSymbol + "</a><br />";
         }
      }
      return res;
   },

   blatResRender : function( d, metadata, record, row, column, store ) {
      if ( !d.targetChromosomeName ) {
         return "";
      }

      var res = "chr" + d.targetChromosomeName + " (" + d.strand + ") " + d.targetStart + "-" + d.targetEnd;

      var organism = d.taxon;
      var database = this.getDb( organism );
      if ( database ) {
         var link = UCSC_TRACKS + "?org=" + encodeURIComponent( organism.commonName ) + "&pix=850"
            + "&db=" + encodeURIComponent( database )
            // We're using a custom track here, so the URL must be publicly accessible by UCSC servers, so Gemma.HOST_URL
            // is not suitable since it may refer to a local/development instance.
            + "&hgt.customText=" + encodeURIComponent( "https://gemma.msl.ubc.ca/blatTrack.html?id=" + d.id );
         res = res + "&nbsp;<a title='Genome browser view (opens in new window)' target='_blank' href='" + link
            + "'><img src='" + Gemma.UCSC_ICON + "' height='10' alt='UCSC Genome Browser logo' /></a>";
      }
      return res;
   },

   getDb : function( taxon ) {
      if ( taxon.externalDatabase ) {
         return taxon.externalDatabase.name;
      }
   },

   initComponent : function() {
      Ext.apply( this, {
         columns : [ {
            sortable : true,
            id : "alignment",
            header : "Alignment",
            dataIndex : "blatResult",
            renderer : this.blatResRender.createDelegate( this ),
            tooltip : "Alignments to the genome"
         }, {
            sortable : true,
            id : "score",
            header : "Score",
            width : 60,
            dataIndex : "score",
            renderer : this.numberformat.createDelegate( this ),
            tooltip : "BLAT score"
         }, {
            sortable : true,
            id : "identity",
            header : "Identity",
            width : 60,
            dataIndex : "identity",
            renderer : this.numberformat.createDelegate( this ),
            tooltip : "Sequence alignment identity"
         }, {
            sortable : true,
            id : 'genes',
            header : "Genes",
            dataIndex : "geneProductIdGeneMap",
            renderer : this.geneMapRender.createDelegate( this ),
            tooltip : "Genes at this genomic location"
         }, {
            sortable : true,
            id : 'transcripts',
            header : "Transcripts",
            dataIndex : "geneProductIdMap",
            renderer : this.gpMapRender.createDelegate( this ),
            tooltip : "Transcripts at this genomic location"
         } ],

         store : new Ext.data.Store( {
            proxy : new Ext.data.DWRProxy( CompositeSequenceController.getGeneMappingSummary ),
            reader : new Ext.data.ListRangeReader( {}, this.record ),
            remoteSort : false,
            sortInfo : {
               field : "score",
               direction : "DESC"
            }
         } )
      } );

      Gemma.GenomeAlignmentsGrid.superclass.initComponent.call( this );

   },

} );
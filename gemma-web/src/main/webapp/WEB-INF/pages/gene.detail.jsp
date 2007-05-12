<%@ include file="/common/taglibs.jsp"%>
<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<jsp:directive.page import="java.util.Collection" />
<jsp:directive.page import="ubic.gemma.model.genome.gene.*" />
<jsp:useBean id="gene" scope="request"
	class="ubic.gemma.model.genome.GeneImpl" />

<script
		src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
		type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>"
		type="text/javascript"></script>
	
	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
		
	<script type='text/javascript'
		src='/Gemma/dwr/interface/Gene2GOAssociationService.js'></script>
		
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

<script type="text/javascript"
	src="<c:url value="/scripts/scrolltable.js"/>"></script>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/styles/scrolltable.css'/>" />

<link href="<c:url value='/styles/ext-all.css'/>" media="screen"
		rel="stylesheet" type="text/css" />

<script type="text/javascript">

var GridUI = function() {
    var ds;
	var grid; //component
	var columnModel; // definition of the columns
	function initDataSource() {
		var recordType = Ext.data.Record.create([
    	  {name: "id", type: "int"},
		  {name: "value", type: "string"},
		  {name: "description", type: "string"},
		  ]);

		  ds = new Ext.data.Store({
		    proxy: new Ext.data.DWRProxy(Gene2GOAssociationService.findByGene ),
		    reader: new Ext.data.ListRangeReader( 
					{id:'id'}, recordType),
		    remoteSort: true
		  });
		
			ds.on("load", function () {
			});		
	}
	
	function getColumnModel() {
		if(!columnModel) {
			columnModel = new Ext.grid.ColumnModel(
				[
					{
						header: 'Value',
						width: 250,
						sortable: true,
						dataIndex: 'value'
					},
					{
						header: 'Description',
						width: 250,
						sortable: true,
						dataIndex: 'description'
					} 															
				]);
		}
		return columnModel;
	}	
	
	function buildGrid() {				
		grid = new Ext.grid.Grid(
			'go-grid',
			{
				ds: ds,
				cm: getColumnModel(),
				autoSizeColumns: true,
				selModel: new Ext.grid.RowSelectionModel({singleSelect:true})
			}
		);
		
		
		grid.render();
	}
			

	return {
		init : function() {
		    var geneid = dwr.util.getValue("gene");
            var g = { id : geneid };
			initDataSource();
			ds.load( { params : [ g ] });			
			buildGrid();
		},
		
		getStore: function() {
			return ds;
		}
	}
}();
Ext.onReady(GridUI.init, GridUI, true);	
	
</script>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<title><fmt:message key="gene.details" /></title>

<h2>
	<fmt:message key="gene.details" />
	<c:if test="${gene.officialSymbol != null}">
	for <jsp:getProperty name="gene" property="officialSymbol" />
	</c:if>
</h2>
<table width="100%" cellspacing="10">
	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.officialSymbol" /> </b>
		</td>
		<td>
			<%
			if ( gene.getOfficialSymbol() != null ) {
			%>
			<jsp:getProperty name="gene" property="officialSymbol" />
			<%
			                } else {
			                out.print( "No official symbol available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.officialName" /> </b>
		</td>
		<td>
			<%
			if ( gene.getOfficialName() != null ) {
			%>
			<jsp:getProperty name="gene" property="officialName" />
			<%
			                } else {
			                out.print( "No official name available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.ncbi" /> </b>
		</td>
		<td>

			<%
			if ( gene.getNcbiId() != null ) {
			%>
			<jsp:getProperty name="gene" property="ncbiId" />
			<a
				href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=<%out.print(gene.getNcbiId()); %>">
				(NCBI link)</a>
			<%
			                } else {
			                out.print( "No NCBI ID available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.description" /> </b>
		</td>
		<td>

			<%
			if ( gene.getDescription() != null ) {
			%>
			<div class="clob" style="height:20px;">
				<jsp:getProperty name="gene" property="description" />
			</div>
			<%
			                } else {
			                out.print( "Description unavailable" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.taxon" /> </b>
		</td>
		<td>
			<%
			                if ( gene.getTaxon() != null ) {
			                out.print( gene.getTaxon().getScientificName() );
			            } else {
			                out.print( "Taxon unavailable" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td align="right" valign="top">
			<b> <fmt:message key="gene.aliases" /> </b>
		</td>
		<td>
			<%
			                if ( gene.getAliases().size() > 0 ) {
			                Collection<GeneAlias> aliasObjects = gene.getAliases();
			                String[] aliases = new String[aliasObjects.size()];
			                int i = 0;
			                for ( GeneAlias a : aliasObjects ) {
			                    aliases[i] = a.getAlias();
			                    i++;
			                }
			                out.print( StringUtils.join( aliases, ", " ) );
			            } else {
			                out.print( "No aliases defined" );
			            }
			%>
		</td>
	</tr>
	<tr>
		<td align="right" valign="top">
			<b>Probes</b><a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Number of probes for this gene on expression platforms in Gemma'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</td>
		<td>
			<c:out
					value="${compositeSequenceCount}" />
			<a
				href="/Gemma/gene/showCompositeSequences.html?id=<%out.print(gene.getId());%>"><img src="<c:url value='/images/magnifier.png'/>" /></a>
		</td>
	</tr>
</table>




	<h3>
		<fmt:message key="gene.ontology" /> terms <a class="helpLink" href="?"
							onclick="showHelpTip(event, 'Only Gene Ontology terms that are directly attached to this gene are shown. Implicit associations (i.e., with parent terms in the GO hierarchy) are not listed.'); return false"><img
								src="/Gemma/images/help.png" /> </a>

	</h3>
	
	
 
<div id="go-grid"  class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:620px;"></div>
<input type = "hidden" name="gene" id="gene" value= "${gene.id}"/> 


<%-- 
<c:if test="${numOntologyEntries > 0 }">
<div id="tableContainer" class="tableContainer">
<display:table name="ontologyEntries" class="list" requestURI=""
	id="ontologyEntriesList" pagesize="100"
	decorator="ubic.gemma.web.taglib.displaytag.OntologyWrapper">
	<display:column property="value" sortable="true" maxWords="20" />
	<display:column property="description" sortable="true" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>
</div>
</c:if>
--%>


<%
if ( gene.getAccessions().size() > 0 ) {
%>
<h3>
	<fmt:message key="gene.accessions" />
</h3>
<%
}
%>

<display:table name="gene.accessions" class="list" requestURI=""
	id="accessionsList" pagesize="10"
	decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper">
	<display:column property="accession" sortable="true" maxWords="20" />
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

<%
if ( gene.getProducts().size() > 0 ) {
%>
<h3>
	<fmt:message key="gene.products" />
</h3>
<%
}
%>


<div id="tableContainer" class="tableContainer">
	<display:table name="gene.products" class="scrollTable" requestURI=""
		id="productsList" pagesize="1000"
		decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper"
		defaultsort="3">
		<display:column property="name" sortable="true" maxWords="20"
			headerClass="fixedHeader" />
		<display:column property="description" sortable="true" maxWords="20"
			headerClass="fixedHeader" />
		<display:column property="type" sortable="true" title="Type" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</div>
 

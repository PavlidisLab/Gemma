<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="compositeSequence" scope="request"
	class="ubic.gemma.model.expression.designElement.CompositeSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
<title><fmt:message key="compositeSequence.title" /> ${ compositeSequence.name}</title>
<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
<%-- deprecated. We should replace this with something tidier --%>
<script type="text/javascript" type="text/javascript">
   Ext.onReady( function() {
      Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
      Ext.QuickTips.init();

      var csid = Ext.get( "cs" ) ? Ext.get( "cs" ).getValue() : null;
      this.detailsGrid = new Gemma.GenomeAlignmentsGrid( {
         renderTo : "probe-details",
         height : 100,
         width : 620
      } );

      this.detailsGrid.getStore().load( {
         params : [ {
            id : csid
         } ]
      } );

   } );
</script>
</head>
<body style="padding: 20px; margin: 20px;">
	<h2 style="margin: 10px;">
		<fmt:message key="compositeSequence.title" />
		: ${ compositeSequence.name} on <a href="/Gemma/arrays/showArrayDesign.html?id=${ compositeSequence.arrayDesign.id }">
			${compositeSequence.arrayDesign.shortName} </a> [${ compositeSequence.arrayDesign.name}]

	</h2>

	<table style="margin: 20px;">
		<tr>
			<td valign="top"><b> <fmt:message key="compositeSequence.description" /> <a class="helpLink" href="#"
					onClick="showHelpTip(event, 'Description for the probe, usually provided by the manufacturer. It might not match the sequence annotation!'); return false"><img
						src="/Gemma/images/help.png" /> </a>
			</b></td>
			<td><c:choose>
					<c:when test="${not empty compositeSequence.description}">${compositeSequence.description}</c:when>
					<c:otherwise>No description available</c:otherwise>
				</c:choose></td>
		</tr>

		<tr>
			<td valign="top"><b> Taxon </b></td>
			<td><c:choose>
					<c:when test="${not empty compositeSequence.biologicalCharacteristic.taxon}">
						${compositeSequence.biologicalCharacteristic.taxon.commonName} - 	${compositeSequence.biologicalCharacteristic.taxon.scientificName}
						</c:when>
					<c:otherwise>
						[Taxon missing]
						</c:otherwise>
				</c:choose></td>
		</tr>
		<tr>
			<td valign="top"><b> Sequence Type </b> <a class="helpLink" href="#"
				onClick="showHelpTip(event, 'The type of this sequence as recorded in our system'); return false"><img
					src="/Gemma/images/help.png" /> </a></td>
			<td><c:choose>
					<c:when test="${not empty compositeSequence.biologicalCharacteristic}">
						<spring:bind path="compositeSequence.biologicalCharacteristic.type">
							<spring:transform value="${compositeSequence.biologicalCharacteristic.type}">
							</spring:transform>
						</spring:bind>
					</c:when>
					<c:otherwise>
									 [Not available] 
								</c:otherwise>
				</c:choose></td>
		</tr>
		<tr>
			<td valign="top"><b> Sequence name <a class="helpLink" href="#"
					onClick="return showHelpTip(event, 'Name of the sequence in our system.')"><img src="/Gemma/images/help.png" />
				</a>
			</b></td>
			<td><c:choose>
					<c:when test="${not empty compositeSequence.biologicalCharacteristic.name}">${ compositeSequence.biologicalCharacteristic.name}</c:when>
					<c:otherwise>No name availaable</c:otherwise>
				</c:choose></td>
		</tr>
		<tr>
			<td valign="top"><b> Sequence description </b><a class="helpLink" href="#"
				onClick="return showHelpTip(event, 'Description of the sequence in our system.')"><img
					src="/Gemma/images/help.png" /> </a></td>
			<td><c:choose>
					<c:when test="${not empty compositeSequence.biologicalCharacteristic.description}">
						${ compositeSequence.biologicalCharacteristic.description}
						</c:when>
					<c:otherwise>No description availaable</c:otherwise>
				</c:choose></td>
		</tr>
		<tr>
			<td valign="top"><b> Sequence accession <a class="helpLink" href="#"
					onClick="return showHelpTip(event, 'External accession for this sequence, if known')"><img
						src="/Gemma/images/help.png" /> </a>
			</b></td>

		</tr>

		<tr>
			<td valign="top"><b> Sequence length </b></td>
			<td><c:choose>
					<c:when test="${not empty compositeSequence.biologicalCharacteristic.sequence}">
						${fn:length(compositeSequence.biologicalCharacteristic.sequence)}
						</c:when>
					<c:otherwise>No sequence available</c:otherwise>
				</c:choose></td>
		</tr>
	</table>


	<h3 style="padding: 5px;">Alignment information</h3>
	<div style="padding: 10px;" id="probe-details"></div>
	<input type="hidden" name="cs" id="cs" value="${compositeSequence.id}" />
</body>

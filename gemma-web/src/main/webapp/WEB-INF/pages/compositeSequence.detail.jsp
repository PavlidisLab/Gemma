<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="compositeSequence" scope="request" class="ubic.gemma.model.expression.designElement.CompositeSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
	<title><fmt:message key="compositeSequence.title" />
	</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/probe.grid.js' />

	<script type="text/javascript" type="text/javascript">
		Ext.onReady(Gemma.ProbeBrowser.app.initOneDetail, Gemma.ProbeBrowser.app);
	</script>
</head>
<body>

	<table id="csTableList">
		<tr>
			<td>
				<h2>
					<fmt:message key="compositeSequence.title" />
				</h2>
				<table width="100%">
					<tr>
						<td valign="top">
							<b> <fmt:message key="compositeSequence.name" /> </b>
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Identifier for the probe, provided by the manufacturer or the data submitter.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
						</td>
						<td>
							<%
							if ( compositeSequence.getName() != null ) {
							%>
							<jsp:getProperty name="compositeSequence" property="name" />
							<%
							                } else {
							                out.print( "No name available" );
							            }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> <fmt:message key="compositeSequence.description" /> <a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Description for the probe, usually provided by the manufacturer. It might not match the sequence annotation!'); return false"><img
										src="/Gemma/images/help.png" /> </a> </b>
						</td>
						<td>
							<%
							if ( compositeSequence.getDescription() != null ) {
							%>
							<jsp:getProperty name="compositeSequence" property="description" />
							<%
							                } else {
							                out.print( "No description available" );
							            }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> Array Design </b><a class="helpLink" href="?"
								onclick="showHelpTip(event, 'The array design this probe belongs to.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
						</td>
						<td>
							<%
							if ( ( compositeSequence.getArrayDesign().getName() != null ) &&  ( compositeSequence.getArrayDesign().getId() != null ) ){
							%>
							<a href="/Gemma/arrays/showArrayDesign.html?id=${ compositeSequence.arrayDesign.id }" >  ${ compositeSequence.arrayDesign.name} </a>
							<%
							                } else {
							                out.print( "Array Design unavailable." );
							            }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> Taxon </b>
						</td>
						<td>
							<%
							                    if ( ( compositeSequence.getBiologicalCharacteristic() != null )
							                    && ( compositeSequence.getBiologicalCharacteristic().getTaxon() != null ) ) {
							%>
							${compositeSequence.biologicalCharacteristic.taxon.scientificName}
							<%
							                } else {
							                out.print( "No taxon information available" );
							            }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> Sequence Type </b>
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'The type of this sequence as recorded in our system'); return false"><img
									src="/Gemma/images/help.png" /> </a>
						</td>
						<td>

							<c:choose>
								<c:when test="${compositeSequence.biologicalCharacteristic != null }">
									<spring:bind path="compositeSequence.biologicalCharacteristic.type">
										<spring:transform value="${compositeSequence.biologicalCharacteristic.type}">
										</spring:transform>
									</spring:bind>
								</c:when>
								<c:otherwise>
									<%="[Not available]"%>
								</c:otherwise>
							</c:choose>


						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> Sequence name <a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Name of the sequence in our system.'); return false"><img
										src="/Gemma/images/help.png" /> </a> </b>
						</td>
						<td>
							<%
							                    if ( ( compositeSequence.getBiologicalCharacteristic() != null )
							                    && ( compositeSequence.getBiologicalCharacteristic().getName() != null ) ) {
							%>
							<a title="View details in Gemma"
								href="<c:url value='/genome/bioSequence/showBioSequence.html?id=${compositeSequence.biologicalCharacteristic.id }'/>">${compositeSequence.biologicalCharacteristic.name
								}</a>
							<%
							                } else {
							                out.print( "No sequence name available" );
							            }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> Sequence description </b><a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Description of the sequence in our system.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
						</td>
						<td>
							<%
							                    if ( compositeSequence.getBiologicalCharacteristic() != null
							                    && compositeSequence.getBiologicalCharacteristic().getDescription() != null ) {
							%>
							${compositeSequence.biologicalCharacteristic.description }
							<%
							                } else {
							                out.print( "No description available" );
							            }
							%>
						</td>
					</tr>
					<tr>
					<tr>
						<td valign="top">
							<b> Sequence accession <a class="helpLink" href="?"
								onclick="showHelpTip(event, 'External accession for this sequence, if known'); return false"><img
										src="/Gemma/images/help.png" /> </a> </b>
						</td>
						<td>
							<%
							                    if ( ( compositeSequence.getBiologicalCharacteristic() != null )
							                    && ( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null )
							                    && compositeSequence.getBiologicalCharacteristic().getTaxon().getExternalDatabase() != null
							                    && compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null ) {

							                String organism = compositeSequence.getBiologicalCharacteristic().getTaxon().getCommonName();
							                String database = compositeSequence.getBiologicalCharacteristic().getTaxon().getExternalDatabase()
							                        .getName();
							                String position = compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry()
							                        .getAccession();

							                String link = position
							                        + " <a target=\"_blank\" title=\"View in UCSC Genome Browser\" href='http://genome.ucsc.edu/cgi-bin/hgTracks?org="
							                        + organism + "&db=" + database + "&position=+" + position
							                        + "&pix=620'><img src=\"/Gemma/images/logo/ucsc.gif\" /></a>";

							                out.print( link );

							            } else {
							                out.print( "No accession available" );
							            }
							%>
						</td>
					</tr>
					</tr>
					<tr>
						<td valign="top">
							<b> Sequence length </b>
						</td>
						<td>
							<%
							                    if ( ( compositeSequence.getBiologicalCharacteristic() != null )
							                    && ( compositeSequence.getBiologicalCharacteristic().getSequence() != null ) ) {
							                out.print( compositeSequence.getBiologicalCharacteristic().getSequence().length() );
							            } else {
							                out.print( "No sequence available" );
							            }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<b> Sequence </b>
						</td>
						<td>
							<%
							                    if ( ( compositeSequence.getBiologicalCharacteristic() != null )
							                    && ( compositeSequence.getBiologicalCharacteristic().getSequence() != null ) ) {
							                String sequence = compositeSequence.getBiologicalCharacteristic().getSequence();
							                String formattedSequence = org.apache.commons.lang.WordUtils.wrap( sequence, 80, "<br />", true );
							%>
							<div class="clob">
								<%
								out.print( formattedSequence );
								%>
							</div>
							<%
							                } else {
							                out.print( "No sequence available" );
							            }
							%>
						</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>

	<div id="probe-details"
		style="margin: 0 0 10px 0; padding: 10px; border: 1px solid #EEEEEE; overflow: hidden; width: 610px; height: 150px;"></div>

	<input type="hidden" name="cs" id="cs" value="${compositeSequence.id}" />



</body>

<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="compositeSequence" scope="request"
	class="ubic.gemma.model.expression.designElement.CompositeSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<title><fmt:message key="compositeSequence.title" />
</title>
<script type="text/javascript"
	src="<c:url value="/scripts/scrolltable.js"/>"></script>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/styles/scrolltable.css'/>" />



<aa:zone name="csTable">

	<table id="csTableList" class="searchTable">
		<tr>
			<td>
				<h2>
					<fmt:message key="compositeSequence.title" />
					Details (click on a composite sequence link to update this area)

				</h2>



				<a
					href="/Gemma/compositeSequence/showCompositeSequence.html?id=<jsp:getProperty name="compositeSequence" property="id" />">(bookmarkable
					link)</a>

				<table width="100%">
					<tr>
						<td valign="top">
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Identifier for the probe, provided by the manufacturer or the data submitter.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> <fmt:message key="compositeSequence.name" /> </b>
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
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Description for the probe, usually provided by the manufacturer. It might not match the sequence annotation!'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> <fmt:message key="compositeSequence.description" /> </b>
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
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'The taxon that this sequence belongs to.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Taxon </b>
						</td>
						<td>
							<%
							if ( compositeSequence.getBiologicalCharacteristic().getTaxon() != null ) {
							%>
							${
							compositeSequence.biologicalCharacteristic.taxon.scientificName}
							<%
							                    } else {
							                    out.print( "No taxon information available" );
							                }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'The type of this sequence in our system'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Sequence Type </b>
						</td>
						<td>
							<%
							if ( compositeSequence.getBiologicalCharacteristic().getType() != null ) {
			                    String type = null;
			                    if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "EST" ) ) {
			                        type = "EST";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "mRNA" ) ) {
			                        type = "mRNA";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "WHOLE_CHROMOSOME" ) ) {
			                        type = "Whole Chromosome";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "DNA" ) ) {
			                        type = "DNA";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "AFFY_COLLAPSED" ) ) {
			                        type = "Collapsed Affymetrix Probe";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "OLIGO" ) ) {
			                        type = "Oligo";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "AFFY_TARGET" ) ) {
			                        type = "Affymetrix Target";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "AFFY_PROBE" ) ) {
			                        type = "Affymetrix Probe";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "REFSEQ" ) ) {
			                        type = "RefSeq";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "BAC" ) ) {
			                        type = "BAC";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "WHOLE_GENOME" ) ) {
			                        type = "Whole Genome";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "OTHER" ) ) {
			                        type = "Other";
			                    } else if ( compositeSequence.getBiologicalCharacteristic().getType().getValue().equalsIgnoreCase( "ORF" ) ) {
			                        type = "ORF";
			                    } 
								out.print( type );
							} else {
								out.print( "No sequence type information available" );
							}
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Name of the sequence in our system.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Sequence name </b>
						</td>
						<td>
							<%
							if ( compositeSequence.getBiologicalCharacteristic().getName() != null ) {
							%>
							${compositeSequence.biologicalCharacteristic.name }
							<%
							                    } else {
							                    out.print( "No name available" );
							                }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'External accession for this sequence, if known'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Sequence accession </b>
						</td>
						<td>
							<%
							                    if ( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null ) {
							                    String organism = compositeSequence.getBiologicalCharacteristic().getTaxon().getCommonName();
							                    String database = "hg18";
							                    if ( organism.equalsIgnoreCase( "Human" ) ) {
							                        database = "hg18";
							                    } else if ( organism.equalsIgnoreCase( "Rat" ) ) {
							                        database = "rn4";
							                    } else if ( organism.equalsIgnoreCase( "Mouse" ) ) {
							                        database = "mm8";
							                    }
							                    // build position if the biosequence has an accession
							                    // otherwise point to location
							                    String position = compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry()
							                            .getAccession();
							                    String link = position + " <a href='http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org="
							                            + organism + "&db=" + database + "&position=+" + position
							                            + "&pix=620'>(Search UCSC Genome Browser)</a>";

							                    out.print( link );

							                } else {
							                    out.print( "No accession available" );
							                }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
						<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Number of bases in the sequence'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Sequence length </b>
						</td>
						<td>
							<%
							                    if ( compositeSequence.getBiologicalCharacteristic().getSequence() != null ) {
							                    out.print( compositeSequence.getBiologicalCharacteristic().getSequence().length() );
							                } else {
							                    out.print( "No sequence available" );
							                }
							%>
						</td>
					</tr>
					<tr>
						<td valign="top">
						<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Gemma\'s representation of the sequence on the array. For Affymetrix note that we \'collapse\' the sequences for the multiple probes into a single sequence that is used for alignments.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Sequence </b>
						</td>
						<td>
							<%
							                    if ( compositeSequence.getBiologicalCharacteristic().getSequence() != null ) {
							                    String sequence = compositeSequence.getBiologicalCharacteristic().getSequence();
							                    String formattedSequence = "";
							                    int nextIndex = 0;
							                    int lineLength = 60;
							                    for ( int i = 0; i < sequence.length() - lineLength; i += lineLength ) {
							                        formattedSequence += sequence.substring( i, i + lineLength );
							                        formattedSequence += "<br />";
							                        nextIndex = i + lineLength;
							                    }
							                    if ( ( sequence.length() % lineLength ) != 0 ) {
							                        formattedSequence += sequence.substring( nextIndex, sequence.length() );
							                        formattedSequence += "<br />";
							                    }
							%>
							<div class="clob" style="height:30px;">
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
		<tr>
			<td>
			<a class="helpLink" href="?"
						onclick="showWideHelpTip(event, 'Details of genomic alignments for this sequence. A sequence can have multiple alignments. The columns are:<ul><li>Alignment: the genomic location of the alignment. A link to view the alignment in the UCSC browser is provided.</li>				<li>S: The alignment score, primarily reflecting the fraction of the sequence that is aligned (0-1.0, higher is better)</li><li>I: Percent sequence identity in the aligned region</li>	<li>Gene Products: A list of the transcripts that overlap with the aligned region. A link out to NCBI is provided</li><li>Genes: The corresponding gene for each transcript. Links are provided to NCBI and Gemma</li>	</ul>'); return false"><img
							src="/Gemma/images/help.png" /> </a>
				<div id="tableContainer" class="tableContainer">		
					<script type="text/javascript">
 			initBodyTag();
 		</script>
					<div>
						&nbsp;
					</div>
					<br />
					<display:table name="blatResults" requestURI="" id="blatResult"
						style="width:100%;" pagesize="2000"
						decorator="ubic.gemma.web.taglib.displaytag.expression.designElement.CompositeSequenceWrapper"
						class="scrollTable" defaultsort="2" defaultorder="descending">
						<display:column property="blatResult" title="Alignment"
							headerClass="fixedHeader" />
						<display:column property="blatScore" title="S"
							headerClass="fixedHeader" />
						<display:column property="blatIdentity" title="I"
							headerClass="fixedHeader" />
						<display:column property="geneProducts" title="GeneProducts"
							headerClass="fixedHeader" />
						<display:column property="genes" title="Genes"
							headerClass="fixedHeader" />
						<display:setProperty name="basic.empty.showtable" value="true" />
					</display:table>
				</div>
			</td>
		</tr>
	</table>
</aa:zone>

</body>
</html>

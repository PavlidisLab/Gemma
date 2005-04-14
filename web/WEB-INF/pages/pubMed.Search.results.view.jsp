<%@ page session="false" %>

<%@ page import="java.util.*" %>
<%@ page import="edu.columbia.gemma.common.description.BibliographicReference" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
	<HEAD>
	</HEAD>
	<BODY>
		<FORM name="newSearchForm" action="pubMedSearch.htm">
			<INPUT type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>">
			<INPUT type="hidden" name="_eventId" value="newSearch">
		</FORM>
		<DIV align="left">
			<P>
				<TABLE width="100%">
					<TR>
						<TD>
							<DIV align="left">Search Results</DIV>
						</TD>
					</TR>
					<TR>
						<TD><HR></TD>
					</TR>
					<TR>
						<TD>
							<TABLE BORDER="1">
								<TR>
									<TD><B>Title</B></TD>
									<TD><B>Publication</B></TD>
									<TD><B>Author List</B></TD>
									<TD><B>Abstract</B></TD>

								
								</TR>
							<%
								List results=(List)request.getAttribute("bibliographicReferences");
								for (int i=0; i<results.size(); i++) {
									BibliographicReference bibliographicReference=(BibliographicReference)results.get(i);
							%>
								<TR>
									<TD>
										<A href="pubMedSearch.htm?_flowExecutionId=<%=request.getAttribute("flowExecutionId") %>&_eventId=select&id=<%=bibliographicReference.getTitle() %>">
										<%=bibliographicReference.getTitle() %>
									    </A>
									</TD>
									<TD><%=bibliographicReference.getPublication() %></TD>
									<TD><%=bibliographicReference.getAuthorList() %></TD>
									<TD><%=bibliographicReference.getAbstractText() %></TD>
								</TR>
							<%
								}
							%>
							</TABLE>
						</TD>
					</TR>
					<TR>
						<TD><HR></TD>
					</TR>
					<TR>
						<TD>
							<DIV align="right">
								<INPUT type="button" onclick="javascript:document.newSearchForm.submit()" value="New Search">
							</DIV>
						</TD>
					</TR>
				</TABLE>	
			</P>
		</DIV>
	</BODY>
</HTML>

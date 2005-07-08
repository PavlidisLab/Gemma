<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment" %>
<%@ page import="edu.columbia.gemma.common.auditAndSecurity.Person" %>
<%@ page import="edu.columbia.gemma.common.description.OntologyEntry" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExperimentalDesign" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExperimentalFactor" %>
<%@ page import="edu.columbia.gemma.expression.experiment.FactorValue" %>
<html>
<head>
	<title>Experiment Detail</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#ffffff">

<content tag="heading">Experiment Detail</content>

<script language="javascript">
	function doRemove(id){
		document.fRemove.userID.value = id;
		document.fRemove.submit();
	}
</script>


<%

ExpressionExperiment ee = null;
Map m = (Map)request.getAttribute("model");

if( m!=null) {
	ee=(ExpressionExperiment) m.get("experiments");
	
	if(ee==null){
		out.print("<B>ERROR: No Experiment with that ID was found.</B><P>&nbsp</P>");
	}
	else{
		%>
		<form name="fRemove" id="fRemove" method="POST" action="ExperimentDetail.html">
			<input type="hidden" name="action" id="action" value="removeParticipant">
			<input type="hidden" name="experimentID" id="experimentID" value="<%=ee.getId()%>">
			<input type="hidden" name="userID" id="userID">
		</form>	
		
		
		<%
		String desc = ee.getDescription();
		if( desc==null ){
			desc="";
		}
		String err = (String)m.get("error");
		if( err != null ){
			out.print("<B>ERROR: " + err + "</B><P>&nbsp</P>");
		}
		%>
		<form name="dForm" id="dForm" method="POST" action="ExperimentDetail.html">
		<input type="hidden" name="action" id="action" value="update">
		<input type="hidden" name="experimentID" id="experimentID" value="<%=request.getParameter("experimentID")%>">
		<table width="100%" cellpadding="1" cellspacing="2">
		<tr>
			<td colspan="2" nowrap="true" bgcolor="#eeeeee"><B>Experiment Details</B></td>
		</tr>
	
		<tr>
			<td bgcolor="white">Name:</td> 
			<td bgcolor="white">
				<input size="68" type="text" name="eName" id="eName" value="<%=ee.getName()%>">
			</td>
		</tr>
		<tr>
			<td bgcolor="white">Provider URL:</td> 
			<td bgcolor="white"><%
			String pu = "";
			if( ee.getProvider()!=null)
				pu = ee.getProvider().getURI();
			%>
			<input size="68" type="text" name="eProviderURL" id="eProviderURL" value="<%=pu%>">
			</td>
		</tr>	
		<tr>
			<td bgcolor="white">PUBMED ID:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td> 
			<td bgcolor="white"><input type="text" name="primaryPubmed" id="primaryPubmed" value="<%
			if( ee.getPrimaryPublication() != null)
				out.print( ee.getPrimaryPublication().getPubAccession().getAccession());
			out.print("\">");
			if( ee.getPrimaryPublication() != null){%>
				&nbsp;<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=<%=ee.getPrimaryPublication().getPubAccession().getAccession()%>">View Article</a>
			<%
			}
			%>
			
			</td>
		</tr>		
		<tr>
			<td valign="top" bgcolor="white">Description:</td> 
			<td bgcolor="white"> <textarea rows="5" cols="50" name="eDesc" id="eDesc"><%=desc%></textarea></td>
		</tr>	
		<tr>
			<td></td>
			<td bgcolor="white"><input type="submit" value="Update Details"></td>
		</tr>
		</form>
		
		<tr>
			<td colspan="2" bgcolor="#eeeeee">
				<B>Investigators</B>
			</td>
		</tr>
		<tr>
			<form name="dFormU" id="dFormU" method="POST" action="ExperimentDetail.html">
			<input type="hidden" name="action" id="action" value="setPI">
			<input type="hidden" name="experimentID" id="experimentID" value="<%=ee.getId()%>">
			<td valign="top">Principle Investigator:</td>
			<td>
				<%=ee.getOwner().getFullName()%><br>Change by Username: <input type="text" name="username" id="username"> <input type="submit" value="update">
			</td>
		</tr>
		</form>
			<%
			for(Iterator iter=ee.getInvestigators().iterator(); iter.hasNext(); ){
				Person p = (Person) iter.next();
				%>
				<tr>
					<td><%=p.getFullName()%></td>
					<td><a href="javascript:doRemove(<%=p.getId()%>)">remove</a></td>
				</tr>
			<%
			}
			%>
		<tr>
			<form name="dFormA" id="dFormA" method="POST" action="ExperimentDetail.html">
			<input type="hidden" name="action" id="action" value="addParticipant">
			<input type="hidden" name="experimentID" id="experimentID" value="<%=ee.getId()%>">
			<td align="right"></td>
			<td>
				Add by Username: <input type="text" name="username" id="username"> <input type="submit" value="add">
			</td>
		</tr>
		</form>
		
		<tr>
			<td colspan="2" bgcolor="#eeeeee">
				<B>Experimental Designs</B>
			</td>
			<%
			if(ee.getExperimentalDesigns().size()==0)
				out.print("<tr><td colspan='2'>No Experimental Designs for this experiment.</td></tr>");
			else{
				for(Iterator iter=ee.getExperimentalDesigns().iterator(); iter.hasNext(); ){
					ExperimentalDesign ed = (ExperimentalDesign) iter.next();
					String normalization = ed.getNormalizationDescription();
					String qualitycontrol = ed.getQualityControlDescription();
					String replicates = ed.getReplicateDescription();
					if(normalization==null)
						normalization = "Unknown";
					if(qualitycontrol==null)
						qualitycontrol = "Unknown";
					if(replicates==null)
						replicates = "Unknown";
						
					%>
					<tr>
						<td>Normalization</td>
						<td><%=normalization%></td>
					</tr>	
					<tr>
						<td>Quality Control</td>
						<td><%=qualitycontrol%></td>
					</tr>	
					<tr>
						<td>Replicates</td>
						<td><%=replicates%></td>
					</tr>	
					<tr>
						<td valign='top'>Design Types:</td>
						<td>
							<%
							for(Iterator iterTypes=ed.getTypes().iterator(); iterTypes.hasNext(); ){
								OntologyEntry oe = (OntologyEntry) iterTypes.next();
								out.print(oe.getValue() + "<br>");	
							}
							%>
						</td>
					</tr>	
					<tr>
						<td valign='top'>Factors</td>
						<td>	
							<%
							String categoryName="";
							String factorList="";
							if( ed.getExperimentalFactors().size()==0 )
								out.print("No Factors for this experiment");
							else{
								for(Iterator iterFac=ed.getExperimentalFactors().iterator(); iterFac.hasNext(); ){
									categoryName="";
									factorList="";
									String factorValue="";
									String factorMeasurement="";
									ExperimentalFactor fac = (ExperimentalFactor) iterFac.next();
									OntologyEntry category = fac.getCategory();
									if( category==null)
										categoryName="Unknown";
									else
										categoryName=category.getValue();
									for(Iterator iterFacValues=fac.getFactorValues().iterator(); iterFacValues.hasNext(); ){
										FactorValue fv = (FactorValue) iterFacValues.next();	
										OntologyEntry fvValue = fv.getValue();
										if(fvValue==null)
											factorValue="Unknown Value";
										else
											factorValue = fvValue.getValue();
										if(fv.getMeasurement()==null)
											factorMeasurement="Unknown Measurement";
										else
											factorMeasurement = fv.getMeasurement().getValue();
										factorList = factorList + fv.getName() + " (" + factorValue + ") " + factorMeasurement + "<br>";
									}						
								}
							}
							%>
							<table>
								<tr><td valign='top'><%=categoryName%>:</td>
								<tr><td valign='top'><%=factorList%></td></tr>
							</table>
						</td>
					</tr>	
							
				<%
				}
			}
			%>
			
		</tr>	
				
		</table>
	<%	
	}
}
else
	out.print("No experiment with that ID was found.<BR>");
%>

</body>
</html>

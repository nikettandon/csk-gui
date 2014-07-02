package view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import controller.InputToView;
import controller.InputToView.Atom;
import controller.InputToView.AtomType;
import controller.InputToView.Autocomplete;
import controller.InputToView.ResultRow;
import controller.InputToView.SubmitForm;
import controller.InputToView.SubmitForm.buttonInfo;

/****************************************************************************
  Uses jquery to construct html page for a generic browser with vertical tabs.
  Requires an {@link InputToView} object which has all formatting and content.
  TODO better UI using: 
  http://java.dzone.com/articles/how-create-jquery-datatable
  http://www.jquery4u.com/tutorials/jquery-infinite-scrolling-demos/
  http://jsfiddle.net/rdworth/AgpaN/
 ****************************************************************************/
public class UI {

private String resultsSummary; // for disambiguation of x,a,y
private Map<String, Atom> clusterSetting = new HashMap<>();
// Retain ordering of the input.
private LinkedHashMap<String, List<ResultRow>> clusterContent =
  new LinkedHashMap<>();
private Atom headerAtom;
private SubmitForm form;
private Autocomplete autocomplete;
private List<Atom> relatedWords; 

public UI(InputToView in) {
  parseInput(in);
}

/*******************************************
 Builds the HTML code for the input bean. 
*******************************************/
public StringBuilder buildHTMLCode(){
  StringBuilder html = new StringBuilder();
  html.append(buildHeader());
  html.append(buildFixedMenu());
  html.append(buildForm());
  html.append(buildRelatedDiv());
  html.append(buildClusterContent());
  html.append(endHtml());
  return html;
}

private StringBuilder buildFixedMenu(){
  StringBuilder menu = new StringBuilder();
  menu.append("\n<div class=\"navbar navbar-inverse navbar-fixed-top\">");
  menu
    .append("\n<div class=\"navbar-inner\"> \n<div class=\"container-fluid\">\n");
  menu.append("<a class=\"brand\" href=\"#\">Comparative Commonsense</a>");
  menu.append("\n</div>");
  menu.append("\n</div>");
  menu.append("\n</div>");
  return menu;
}

/****************************************************************
 TODO Ignore the AtomIDs for now; tooltip on entire(x..,r..,y..).
 @param in List of atoms (incl. Header) & submit form details.
 ****************************************************************/
private void parseInput(InputToView in){
  this.form = in.form;
  if(in.autocomplete != null) autocomplete = in.autocomplete;

  for(Atom a: in.metaAtoms){
    if(a.clusterName == null || a.clusterName.length() == 0) continue;

    if(a.type.equals(AtomType.header))
      headerAtom = a;
    else if(a.type.equals(AtomType.cluster))
      clusterSetting.put(a.clusterName, a);
    else if(a.type.equals(AtomType.resultSummary)) resultsSummary = a.content;
  }
  for(ResultRow row: in.rows){
    addToCluster(row._clusterName, row);
  }
  this.relatedWords = in.relatedWords;
  
}

private void addToCluster(String key,ResultRow row){
  if(clusterContent.containsKey(key))
    clusterContent.get(key).add(row);
  else{
    List<ResultRow> valList = new ArrayList<>();
    valList.add(row);
    clusterContent.put(key, valList);
  }
}

/*******************************************
Constructs submit form given the input {@link SubmitForm}.
TODO: adjust placement of buttons on UI.
*******************************************/
private StringBuilder buildHeader(){
  StringBuilder headerCode = new StringBuilder();
  headerCode.append(cssFiles());
  headerCode.append(twoFrames());
  headerCode.append(jsFiles());
  // JS code for vertical tabs; tooltip.
  headerCode
    .append("<script type=\"text/javascript\"> window.onload=function(){$('#tabs').tabs()");
  headerCode
    .append(".addClass('ui-tabs-vertical ui-helper-clearfix');}</script>\n");
/*  headerCode
    .append("<script> $( document ).ready(function() { $(\".loading\").fadeOut();");
  headerCode
  	.append("$( document ).tooltip();$( \"#x\" ).tooltip({ position: { my: \"top-25%\", at: \"right center\" } });");
  headerCode
	.append(" $(\"#go, a.word\").click(function(){$(\".loading\").fadeIn();});");
  headerCode
  	.append("$(window).on('unload', function(){$(\".loading\").fadeOut();});});</script>\n");*/
  headerCode
  	.append("<script></script>\n");
  headerCode.append("</HEAD><BODY>\n");
  return headerCode;
}

private StringBuilder twoFrames(){
  StringBuilder headerCode = new StringBuilder();
  // 60px to make the container go all the way to the bottom of the topbar
  headerCode.append("\n<style> body {padding-top: 60px;} </style>\n");
  return headerCode;
}

private StringBuilder cssFiles(){
  StringBuilder css = new StringBuilder();
  css.append("<HTML><HEAD><meta http-equiv='Content-Type' content='text/html;")
    .append(
      " charset=utf-8'><meta name='robots' content='noindex,nofollow' />\n")
    .append("<TITLE>").append(headerAtom.content).append("</TITLE>\n");

  css
  // http://getbootstrap.com/2.3.2/ bootstrap.css
    .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/twitter.css\" />");

  css
  // http://code.jquery.com/ui/1.9.1/themes/base/jquery-ui.css
    .append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"css/jquery-ui.css\" />");
  css
  // http://www.mpi-inf.mpg.de/~ntandon/verticalstyle.css
    .append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"css/verticalstyle.css?v=1.8\" />");
  
  if(autocomplete != null)
    css
    // http://www.pengoworks.com/workshop/jquery/lib/jquery.autocomplete.css
      .append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"css/jquery.autocomplete.css\" />");
  return css;
}

private StringBuilder jsFiles(){
  StringBuilder js = new StringBuilder();

  /*  js
    // bootstrap.min.js <order matters. place bootstrap initially to avoid tooltip issues,
    // same name conflict in jquery and bootstrap.>
    .append("<script src=\"js/twitter.js\">");
    js.append("</script>\n");
  */

  js
  // http://code.jquery.com/jquery-1.8.2.js ; jquery-1.10.2.js
  .append("\n<script src=\"js/jquery-1.8.2.js\">");
  js.append("</script>\n");

  js
  // bootstrap.min.js <order matters. place bootstrap initially to avoid tooltip issues,
  // same name conflict in jquery and bootstrap.>
  .append("<script src=\"js/twitter.js\">");
  js.append("</script>\n");

  js
  // http://code.jquery.com/ui/1.9.1/jquery-ui.js ; jquery-ui-1.10.2.js
  .append("<script src=\"js/jquery-ui.js\">");
  js.append("</script>\n");
  
  js
  .append("<script src=\"js/genericbrowser.js\">");
  js.append("</script>\n");

  if(autocomplete != null){
    js
    // http://www.pengoworks.com/workshop/jquery/lib/jquery.autocomplete.js
    .append("<script src=\"js/jquery.autocomplete.js\">").append("</script>\n");
    /*.append("<script src=\"js/json.autocompletion.js\">").append("</script>\n");*/
  }

  return js;
}

/*******************************************
Constructs submit form given the input {@link SubmitForm}.
TODO: adjust placement of buttons on UI.
*******************************************/
private StringBuilder buildForm(){
  StringBuilder formCode = new StringBuilder();
  formCode.append(formDiv1());
  formCode.append(formDiv2());
  
  for(buttonInfo b: form.buttons){

    //@formatter:off
    formCode
      .append("\n<input ")
      .append(" id=\"").append(b.id).append("\"")
      .append(" type=\"").append(b.type).append("\"")
      .append(" name=\"").append(b.name).append("\"")
      .append(" value=\"").append(b.value).append("\"")
      .append(" title=\"").append(b.tooltip).append("\"")
      // On enter submit , autofocus for html5, js fallback..
      .append(b.type.equalsIgnoreCase("submit")? 
        " style=\"visibility:hidden\" ":" size=\"60\"  style=\"height:40px\" ")
      .append(">\n");
    //@formatter:on 
  }
  // Autocompletion code.
  // myautocomplete('x', 'jsp/autocomplete.jsp');
  if(autocomplete != null){

    formCode.append("<script>");
    /*formCode.append("myautocomplete('" + autocomplete.onWhichFieldID + "','"
    + autocomplete.usingWhichJSP + "'");*/

    formCode.append("$( document ).ready(function() {$(\"#");
    formCode.append(autocomplete.onWhichFieldID + "\")");
    formCode.append(".autocomplete(\"" + autocomplete.usingWhichJSP
      + "\" ,cacheLength=0);");
    formCode.append("});</script>\n");
    /*
        //TODO experimental lines added to retain focus on query input box.
        formCode
          .append("<script>\n if (!(\"autofocus\" in document.createElement(\"input\"))) "
            + "{document.getElementById(\"x\").focus();}</script>\n");*/
  }

  formCode.append(closeFormDiv());
  return formCode;
}

private Object formDiv2(){
  StringBuilder formDiv = new StringBuilder();
  formDiv.append("\n<form  method=\"").append(form.method).append(
    "\" action=\"").append(form.action).append("\">\n");
  formDiv.append("\n<fieldset>");
  formDiv.append("\n<div class=\"control-group\">");
  formDiv.append("\n<div class=\"controls\">");
  formDiv.append("\n<div class=\"input-append\">");
  return formDiv;
}

private StringBuilder closeFormDiv(){
  StringBuilder formCode = new StringBuilder();
  //formCode.append("</div></div></div></fieldset></form></div></div>");
  formCode.append("</div></div></div></fieldset></form></div>");
  return formCode;
}

private StringBuilder formDiv1(){
  StringBuilder formDiv = new StringBuilder();

  formDiv.append("\n<div class=\"container-fluid\">");
  formDiv.append("\n<div class=\"row-fluid\">");
  formDiv.append("\n<div class=\"span3\">");
  formDiv.append("\n<div class=\"well sidebar-nav\">");
  return formDiv;
}

/*******************************************
  Uses jquery to construct vertical tabs.
  @return pluggable html code for content.
 *******************************************/
private StringBuilder buildClusterContent(){
  StringBuilder htmlCode = new StringBuilder();

  // Vertical tab is contained in another div.
  htmlCode.append(buildClusterDiv());

  // Vertical tabs structure
  htmlCode.append("<div id=\"tabs\">\n<ul>");
  for(Entry<String, List<ResultRow>> e: clusterContent.entrySet())
    htmlCode.append("\n\t<li>").append("\t<a href=\"#" + e.getKey() + "\">")
      .append(e.getKey()).append("</a>\t").append("</li>");
  htmlCode.append("</ul>");

  // Vertical tabs content
  for(Entry<String, List<ResultRow>> e: clusterContent.entrySet()){
    htmlCode.append("\n<div id=\"").append(e.getKey()).append("\">");
    htmlCode.append(oneClusterContent(e.getValue()));
    htmlCode.append("</div>");
  }

  return htmlCode.append("\n</div>");
}

private StringBuilder buildClusterDiv(){
  // TODO Niket add results summary (disambiguation) with formatting?
  return new StringBuilder().append("<div class=\"span9\"> \n "
    + " <div class=\"hero-unit\"> \n <div id=\"visualization\">"
    + resultsSummary + "");
}

/***************************************************
 Construct Vertical tab's content.
 @param rows atoms used to construct the content.
 ***************************************************/
private StringBuilder oneClusterContent(List<ResultRow> rows){
  StringBuilder clusterHTML = new StringBuilder();
  // Each clusterContent is a table of x1 x2 x3 ...
  // TODO better table formatting.. (incl. tb header).. datatable plugin?
  clusterHTML.append("\n<TABLE align='center' border=\"0\" width=\"600px\" ")
    .append("style='border-width:2px;border-spacing:10px;").append(
      " border-color: black;'>\n<TBODY>\n");

  for(ResultRow a: rows)
    clusterHTML.append(htmlizeRow(a)).append("\n");

  clusterHTML.append("</TBODY></TABLE>");
  return clusterHTML;
}

/****************************************
 Decorate row, fill content etc. 
 ****************************************/
private StringBuilder htmlizeRow(ResultRow row){
  StringBuilder atomHTML = new StringBuilder();
  // TODO one atom per row (later match using ID).
  atomHTML.append("<tr>\n");
  for(Atom a: row._rowElems)
    atomHTML.append(htmlizeAtom(a)).append("\n");
  atomHTML.append("</tr>");
  return atomHTML;
}

/****************************************
Decorate atom, fill content etc. 
****************************************/
private StringBuilder htmlizeAtom(Atom a){
  StringBuilder atomHTML = new StringBuilder();
  atomHTML.append("<td style=\"padding-bottom:10px;\">");
  atomHTML.append("<a class=\"word\"").append(
    " style=\"color:" + a.decoration.c.name() + "\" ");
  // TODO more decoration reqd..
  atomHTML.append("title=\"").append(a.tooltip).append("\"");
  atomHTML.append(" href=\"?x=" + a.hrefQuery + "\"> ");
  atomHTML.append(a.content).append(" </a>\t");
  atomHTML.append("</td>\n");
  return atomHTML;
}

/****************************************
 * Construct div to show related words.
 */
private StringBuilder buildRelatedDiv(){
	StringBuilder htmlCode = new StringBuilder();
	htmlCode.append("<div class=\"well sidebar-nav\">");
	htmlCode.append("<div class=\"control-group\">");
	htmlCode.append("<div class=\"controls\">");
	htmlCode.append("<h4>Related Words</h4>\n<ul style=\"list-style-type: none;padding:0;margin: 0;\">\n");
	for (Atom r: relatedWords){
		htmlCode.append("\t<li style=\"padding-bottom:5px;\"><a class=\"word\" ");
		htmlCode.append("style=\"color:"+r.decoration.c.name()+";");
		htmlCode.append("font-weight:"+r.decoration.bf.name()+";\" ");
		htmlCode.append("href=\""+r.hrefQuery+"\">");
		htmlCode.append(r.content).append("</a></li>\n");
	}
	/*for(Entry<String, List<ResultRow>> e: clusterContent.entrySet()){
		for(ResultRow a: e.getValue()){
			for(Atom at: a._rowElems){
				htmlCode.append(at.toString()+"<br>");

			}
		}
	}*/
	htmlCode.append("</ul></div></div></div></div>");
	return htmlCode;
}
/****************************************
 HTML end markers. 
 ****************************************/
private StringBuilder endHtml(){
  StringBuilder htmlCode = new StringBuilder();
  htmlCode.append("</div>  </div> </div> </div> <hr> </div>");
  htmlCode.append("<div class=\"loading\">Loading&#8230;</div>");
  htmlCode.append("\n</BODY></HTML>\n");
  return htmlCode;
}

}
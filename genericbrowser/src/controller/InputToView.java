package controller;

import java.util.ArrayList;
import java.util.List;

/********************************************************** 
 Input to a generic IE browser.  
 @author ntandon
 *********************************************************/
public class InputToView {

public List<Atom> metaAtoms;
public List<ResultRow> rows;
public SubmitForm form;
public Autocomplete autocomplete;

/*********************************************************
 Assumes that the application has no autocomplete feature
 Internally initializes a list of UIAtoms (e.g. triples)
 Many atoms (incl. header) make up the input to UI.
 Submit form is for form based applications.
 *********************************************************/
public InputToView(SubmitForm form,List<Atom> metaAtoms,List<ResultRow> rows) {
  this.metaAtoms = new ArrayList<>();
  for(Atom a: metaAtoms)
    if(a != null) this.metaAtoms.add(a);

  this.rows = new ArrayList<>();
  for(ResultRow resultRow: rows)
    if(resultRow != null) this.rows.add(resultRow);

  this.form = form;
}

/*********************************************************
Internally initializes a list of UIAtoms (e.g. triples)
Many atoms (incl. header) make up the input to UI.
Submit form is for form based applications. Autocompletion 
e.g. when user types x:bike; autoSuggest y:bus,car 
*********************************************************/
public InputToView(SubmitForm form,List<Atom> atoms,List<ResultRow> rows,
  Autocomplete autoC) {
  this(form, atoms, rows);
  this.autocomplete = autoC;
}

public enum AtomType {
  header, content, cluster, resultSummary
}

/********************************************************** 
 Submit form type, and button details:
 <ul><li>method<li>action<li>buttons</ul>.
 *********************************************************/
public static class SubmitForm {
/** get or post */
public String method;
/** who is called when submit button is clicked */
public String action;
/** e.g. < input id=\"search\" type=\"text\" name=\"x\" value=\"\"> */
public List<buttonInfo> buttons;

public SubmitForm(String method,String action,List<buttonInfo> buttons) {
  super();
  this.method = method;
  this.action = action;
  this.buttons = buttons;
}

/************************************************************** 
 Submit form must be initialized before initializing buttonInfo
 <ul> <li>id <li>type <li>name <li>value</ul>
 @author ntandon
 **************************************************************/
public static class buttonInfo {
public String id;
public String type;
public String name;
public String value;
public String tooltip;

public buttonInfo(String id,String type,String name,String value,String tooltip) {
  super();
  this.id = id;
  this.type = type;
  this.name = name;
  this.value = value;
  this.tooltip = tooltip;
}
}

}

/***************************************************************** 
 Autocompletion e.g. when user types x:bike; autoSuggest y:bus,car 
 ****************************************************************/
public static class Autocomplete {
public String onWhichFieldID;
public String usingWhichJSP;

public Autocomplete(String onWhichFieldID,String usingWhichJSP) {
  super();
  this.onWhichFieldID = onWhichFieldID;
  this.usingWhichJSP = usingWhichJSP;
}
}

/********************************************************** 
 Layout of a single atom e.g. color, font, background 
 *********************************************************/
public static class Decoration {

public enum color {
  white, red, green, blue, black
}
public enum font {
  normal, italics
}
public enum boldface {
  bold, normal
}
public enum fontsize {
  small, normal, big
}
public enum background {
  white, black, yellow
}

public color c;
public font f;
public boldface bf;
public fontsize s;
public background bg;

/*************************************************************
 * Default values are used. Use other constructor to customize
 *************************************************************/
public Decoration() {
  this.c = color.black;
  this.f = font.normal;
  this.bf = boldface.normal;
  this.s = fontsize.normal;
  this.bg = background.white;
}

public Decoration(color c,font f,boldface bf,fontsize s,background bg) {
  this.c = c;
  this.f = f;
  this.bf = bf;
  this.s = s;
  this.bg = bg;
}
}

/**********************************************************
 The element to display on UI (e.g. a triple in IE browser)
 <ul> <li> AtomType type <li> int atomID,<li>String content,
 <li>String url,<li>String tooltip,<li>String clusterName,
 <li>Decoration decoration  </li>
 @author ntandon
 *********************************************************/
public static class Atom {

/** Content, header, cluster  */
public AtomType type;
/**  e.g. same atomID if x,r,y three atoms must be placed together */
public int atomID;
/**  e.g. x or r or y */
public String content;
/**  upon clicking the atom, this url is used. */
public String hrefQuery;
/**  e.g. frequency statistics of the atom */
public String tooltip;
/**  e.g. cluster by relation name (marriedTo) */
public String clusterName;
/**  e.g. make the atom bold or colored */
public Decoration decoration;

public Atom(AtomType type,int atomID,String content,String hrefQuery,
  String tooltip,String clusterName,Decoration decoration) {
  super();
  this.type = type;
  this.atomID = atomID;
  this.content = content;
  this.hrefQuery = hrefQuery;
  this.tooltip = tooltip;
  this.clusterName = clusterName;
  this.decoration = decoration;
}

// Used only for debugging.
@Override public String toString(){
  return "Atom [type=" + type + ", atomID=" + atomID + ", content=" + content
    + ", url=" + hrefQuery + ", tooltip=" + tooltip + ", clusterName="
    + clusterName + ", decoration=" + decoration + "]";
}

}

public static class ResultRow {

public List<Atom> _rowElems;
public String _clusterName;

public ResultRow(List<Atom> rowElems,String clusterName) {
  _rowElems = new ArrayList<>();

  for(Atom atom: rowElems)
    if(atom != null){
      _rowElems.add(atom);
      assert atom.type.equals(AtomType.content) : "InputToView rowElem (Atom)"
        + " should be of type content";
    }
  _clusterName = clusterName;
}

}

}
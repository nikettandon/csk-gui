package controller;

import java.util.ArrayList;
import java.util.List;

/********************************************************************
The response to {@link InputToView} e.g. Db lookup is required on this.
********************************************************************/
public class InputToModel {

/********************************************************************
 Initialized from {@link InputToView}'s  form values (type/initial val)
 Replaces with the (new) state of the values as entered by user.
 ********************************************************************/
public List<String> x;
public List<String> metadata;
public int maxDbResults;

public InputToModel(List<String> x,int maxDbResults) {
  this(x, maxDbResults, new ArrayList<String>());
}

public InputToModel(List<String> x,int maxDbResults,List<String> metadata) {
  super();
  this.x = new ArrayList<>();
  if(x != null) for(String oneX: x)
    this.x.add(oneX);

  this.metadata = new ArrayList<>();
  if(metadata != null) for(String oneMetadata: metadata)
    this.metadata.add(oneMetadata);

  this.maxDbResults = maxDbResults;
}

}
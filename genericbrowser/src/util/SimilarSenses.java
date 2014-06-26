package util;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class SimilarSenses {

/**
 * wna_181, {ripe#a#1, unripe#a#1}
 */
private AutoMap<String, Set<String>> cA,cN;

/**
 * ripe#a#1,wna_181
 */
private AutoMap<String, Set<String>> wA,wN;

private static SimilarSenses proxy;
private static SimilarSenses proxyMFS;

public static SimilarSenses getInstance(boolean restrictToMFS) throws Exception{
  if(restrictToMFS){
    if(proxyMFS == null) proxyMFS = new SimilarSenses(restrictToMFS);
    return proxyMFS;
  } else{
    if(proxy == null) proxy = new SimilarSenses(restrictToMFS);
    return proxy;
  }
}

public static void main(String[] args) throws Exception{
  SimilarSenses proxy = SimilarSenses.getInstance(false);
  System.out.println(proxy.getSimilar("tiger#n#1", false));
  DBConnector.closeConnections();

}

private SimilarSenses(boolean restrictToMFS) throws Exception {

  cA = new AutoMap<String, Set<String>>();
  cN = new AutoMap<String, Set<String>>();
  wA = new AutoMap<String, Set<String>>();
  wN = new AutoMap<String, Set<String>>();
  loadMaps(restrictToMFS);
}

public Set<String> getSimilar(String wordNo_,boolean isNoun){
  Set<String> similars = new HashSet<String>();
  if(wordNo_.contains("_")) wordNo_.replaceAll("_", " ");
  if(isNoun){
    if(!wN.containsKey(wordNo_)) return similars;
    for(String clusterID: Util.nullableIter(wN.get(wordNo_)))
      similars.addAll(cN.get(clusterID));
  } else{
    if(!wA.containsKey(wordNo_)) return similars;
    for(String clusterID: Util.nullableIter(wA.get(wordNo_)))
      similars.addAll(cA.get(clusterID));
  }
  return similars;
}

private void loadMaps(boolean restrictToMFS) throws Exception{
  noSenses(restrictToMFS, "__clusteredwna", false);
  noSenses(restrictToMFS, "__clusteredwnn", true);

  /*noSenses("./data/antonyms/expanded.adjectives.antonyms", mA, restrictToMFS);
  noSenses("./data/synonyms/expanded.adjectives.synonyms", mA, restrictToMFS);
  noSenses("./data/antonyms/expanded.nouns.antonyms", mN, restrictToMFS);
  noSenses("./data/synonyms/expanded.nouns.synonyms", mN, restrictToMFS);*/
}

/** 
 * fill map <good,bad> (and also return it)
 * @param filePath ./data/antonyms/expanded.antonyms
 * @param restrictToMFS 
 * @return 
 * @throws IOException
 */
private void noSenses(boolean restrictToMFS,String dbTb,boolean isNoun){
  try{
    String sql = "select subx,y,ywps from " + dbTb;
    ResultSet rs = DBConnector.q(sql);
    while (rs.next()){
      String subx = rs.getString(1);
      String ywps = rs.getString(3);
      if(restrictToMFS && !ywps.endsWith("#1")) continue;

      if(isNoun){
        cN.addSetValue(subx, ywps);
        wN.addSetValue(ywps, subx);
      } else{
        cA.addSetValue(subx, ywps);
        wA.addSetValue(ywps, subx);
      }
    }
  } catch (Exception e){
    e.printStackTrace();
    DBConnector.closeConnections();
    noSenses(restrictToMFS, dbTb, isNoun);
  }
  // System.out.println("  [done]");
}
}

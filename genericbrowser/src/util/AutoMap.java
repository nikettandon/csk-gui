package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @description this class is like C# AutoDictionary
 * @date Dec 8, 2011
 * @version 1
 * @author ntandon
 */
public class AutoMap<K, V> extends HashMap<K, V> {

private static final long serialVersionUID = 1L;

public AutoMap(int size) {
  super(size);
}

public AutoMap() {
  super();
}

public AutoMap(Map<K, V> existingMap) {
  super(existingMap);
}

public void addNumericValueInt(K patternKey,V scoreOrFreq){
  V value = scoreOrFreq;
  if(this.containsKey(patternKey)){
    Integer temp = (Integer) value + (Integer) this.get(patternKey);
    this.put(patternKey, (V) temp);
  } else this.put(patternKey, value);
}

public void addNumericValue(K patternKey,V scoreOrFreq){
  V value = scoreOrFreq;
  if(scoreOrFreq instanceof Number && this.containsKey(patternKey)){
    Double temp =
      ((Number) scoreOrFreq).doubleValue()
        + ((Number) this.get(patternKey)).doubleValue();
    value = (V) temp;
  }
  this.put(patternKey, value);
}

/** 
 * Updates frequency of value's key in AutoDictionary (String, AutoDictionary (String, Double)) <BR>
 * red,blue,yellow p1:20, p2:30, p3:5
 * @param key : e.g. red,blue,yellow
 * @param value : e.g. p3:3 or p4:10
 *  
 */
public static void addKeyKeyNumericValue(String key,
  AutoMap<String, AutoMap<String, Double>> map,String patternString,
  double freqToAdd){
  AutoMap<String, Double> patternFreq = new AutoMap<String, Double>();
  if(map.containsKey(key)) patternFreq = map.get(key);
  patternFreq.addNumericValue(patternString, freqToAdd); // p3,100
  map.put(key, patternFreq);
}

public void addArrayValueNoRepeat(K key,Object value){
  V newList = (V) new ArrayList();
  if(this.containsKey(key)) newList = this.get(key);
  if(!((Collection) newList).contains(value))
    ((Collection) newList).add(value);
  this.put(key, newList);
}

public void addArrayValue(K key,Object value){

  V newList = (V) new ArrayList();
  if(this.containsKey(key)) newList = this.get(key);
  ((Collection) newList).add(value);
  this.put(key, newList);
}

public void addSetValue(K key,Object value){
  V newList = (V) new HashSet();
  if(this.containsKey(key)){
    newList = this.get(key);
  }
  ((Collection) newList).add(value);
  this.put(key, newList);
}

public Number sumValues(){
  double sum = 0;
  for(V value: this.values())
    if(value instanceof Number) sum += (Double) value;
    else return -1;
  return sum;
}

/* (non-Javadoc)
 * 
 * @see java.util.AbstractMap#toString() */
@Override public String toString(){
  return super.toString();
}

public void put(String kv,String separator,int keyPosition){
  String[] parts = kv.split(separator);
  if(parts.length == 2)
    this.put((K) parts[keyPosition], (V) parts[parts.length - 1 - keyPosition]);
}

public TreeMap<K, V> sortByValue(){
  ValueComparator<K, V> sortByNumericVal = new ValueComparator<K, V>(this);
  TreeMap<K, V> sortedMap = new TreeMap<K, V>(sortByNumericVal);
  sortedMap.putAll(this);
  return sortedMap;
}

/**
 * @description this method returns a cloned keyset so no changes are
 *              reflected in the original keyset e.g. in case of retainAll
 *              operation
 * @return
 * @date Dec 14, 2011
 * @author ntandon
 */
public Set getClonedKeySet(){
  Set clonedSet = new HashSet<K>();
  for(K key: this.keySet())
    clonedSet.add(key);
  return clonedSet;
}

}

class ValueComparator<K, V> implements Comparator {

Map<K, V> base;

public ValueComparator(Map<K, V> base) {
  this.base = base;
}

@Override public int compare(Object a,Object b){
  // Imp Note: if you return 0 on compare, the map assumes this is
  // duplicate key.
  if(((Number) base.get(a)).doubleValue() <= ((Number) base.get(b))
    .doubleValue()) return 1;
  else return -1;
}
}

package util;
import java.io.FileReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javatools.database.Database;
import javatools.database.PostgresDatabase;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

public class DBConnector {
private static Database db;
private static CopyManager cm;

public static ResultSet q(String sql) throws SQLException{
  Statement st = DBConnector.getDB().getConnection().createStatement();
  st.setFetchSize(1000);
  return st.executeQuery(sql);
}

public static Database getDB(){
  if(db != null) return db;
  try{
    db =
      new PostgresDatabase("ntandon", "postgres@123", "www13",
        "postgres2.d5.mpi-inf.mpg.de", null);
    
    // Turn off autocommit mode
    db.getConnection().setAutoCommit(false);
  } catch (Exception e){
    try{
      e.printStackTrace();
      db =
        new PostgresDatabase("postgres", "postgres@123", "postgres", null, null);
    } catch (Exception e1){
      e1.printStackTrace();
    }
  }
  return db;
}

public static void closeConnections(){
  try{
    if(!db.getConnection().isClosed()) db.close();
    db = null;
    cm = null;
  } catch (Exception e){
    db = null;
    cm = null;
  }
}

/** cm.copyIn("COPY mytable FROM STDIN WITH DELIMITER '\t'", new FileReader(fileToLoadFrom.tsv)); */
public static void load(String tbName,String fileToCopyFrom,boolean clearTable,
  char delim) throws Exception{
  if(cm == null)
    cm = new CopyManager((BaseConnection) getDB().getConnection());
  if(clearTable) getDB().executeUpdate("DELETE FROM " + tbName);
  cm.copyIn("COPY " + tbName + " FROM STDIN WITH DELIMITER '" + delim + "'",
    new FileReader(fileToCopyFrom));
}
}

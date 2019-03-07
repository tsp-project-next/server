package server;

import java.sql.*;

public class Database {

    Connection conn = null;
    String username = null;
    String password = null;

    public boolean connectDB(){
        try{
            conn = DriverManager.getConnection("path",username,password);
        } catch (SQLException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void disconnectDB(){
        try{
            conn.close();
        } catch(SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("Vendor: " + e.getErrorCode());

        }
    }
}

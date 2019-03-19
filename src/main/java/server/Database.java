package server;

import java.sql.*;

public class Database {

    Connection conn = null;
    String username = "next";
    String password = "projectnext";

    public boolean connectDB(){
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/",username,password);
            System.out.println("Database Connected...");
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

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

    /**
     * addLobby
     * calls the addLobby procedure from the database
     * Parameters: code (the lobby code), uri (the playlist uri)
     * Return: 0 (if failed), 1 (if completed)
     */
    public int addLobby(String code, String uri) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int rowcount;

        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        try {
            stmt = conn.prepareStatement(
                "addLobby(?, ?)"
            );
            stmt.setString(code, uri);
            rowcount = stmt.executeUpdate();
            if (rowcount != 1) {
                System.err.println("Something went wrong.");
                conn.rollback();
            } else {
                conn.commit();
            } 
        } catch(SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }
    
    /**
     * removeLobby
     * calls the removeLobby procedure from the database
     * Parameters: code (the lobby code)
     * Return: 0 (if failed), 1 (if completed)
     */
    public int removeLobby(String code) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int rowcount;

        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        try {
            stmt = conn.prepareStatement(
                "removeLobby(?)"
            );
            stmt.setString(code);
            rowcount = stmt.executeUpdate();
            if (rowcount != 1) {
                System.err.println("Something went wrong.");
                conn.rollback();
            } else {
                conn.commit();
            } 
        } catch(SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }
}

package server;

import java.io.Console;
import java.sql.*;
import java.util.Scanner;

public class Database {

    private Connection conn = null;
    private String username = null;
    private String password = null;
    private boolean mask = false;
    private char[] maskedPassword = null;

    public boolean connectDB(){
        try{
            Scanner scanner = new Scanner(System.in);
            if(scanner == null){
                System.out.println("Scanner is null.");
                return false;
            }
            System.out.print("Do you want to mask input (y/n): ");
            String response = scanner.nextLine();
            if(response.charAt(0) == 'y' || response.charAt(0) == 'Y') {
                mask = true;
            }

            if(mask == true) {
                Console console = System.console();
                if(console == null){
                    System.out.println("Console is null. Run the program in terminal.");
                    return false;
                }
                username = console.readLine("Please enter your name:");
                maskedPassword = console.readPassword("Please enter your password:");
            } else {
                System.out.print("Please enter your username: ");
                username = scanner.nextLine();
                System.out.print("Please enter your password: ");
                password = scanner.nextLine();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            if(mask == true) {
                conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/", username, new String(maskedPassword));
                System.out.println("Database Connected...");
            } else {
                conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/", username, password);
                System.out.println("Database Connected...");
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
            //e.printStackTrace();
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
    public boolean addLobby(String code, String uri) {
        CallableStatement stmt = null;
        Connection conn = null;
        ResultSet rs = null;
        int rowcount;
        String query = "{ call addLobby(?, ?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, code);
            stmt.setString(2, uri);
            rs = stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * removeLobby
     * calls the removeLobby procedure from the database
     * Parameters: code (the lobby code)
     * Return: 0 (if failed), 1 (if completed)
     */
    public boolean removeLobby(String code) {
        CallableStatement stmt = null;
        Connection conn = null;
        ResultSet rs = null;
        int rowcount;
        String query = "{ call removeLobby(?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import client.Packet;

public class Server {

    private ServerSocket serverSocket;
    private Database database;

    private static boolean debugBuild = false;

    //client number serves no purpose other than to display how many users are connected
    private int clientNumber = 0;
    //a list of all currently taken user_ids
    private List<String> user_ids = Collections.synchronizedList(new ArrayList<>());
    //Hashmap<userid, lobbycode> This is for storing the lobby codes hosts are associated with
    private ConcurrentHashMap<String,String> hostMap = new ConcurrentHashMap<>();
    //Hashmap<user_id,client thread/object> the clients thread is stored by their user_id
    private ConcurrentHashMap<String,HandleClient> clientMap = new ConcurrentHashMap<>();
    //Hashmap<lobbycode, arraylist<userids>> each lobby code stores a list of connected users
    private ConcurrentHashMap<String,ArrayList<String>> lobbyMap = new ConcurrentHashMap<>();

    public static void main(String args[]) {

        new Server();
    }

    public Server() {
        try {
            database = new Database();
            boolean connected = database.connectDB();
            //if the database is not connected then shut down the server
            if(!connected) {
                System.out.println("Failed to connect to database.");
                System.out.println("Server stopping...");
                System.exit(0);
            }

            database.deleteTableRows("users");
            System.out.println("users table data cleared");
            database.deleteTableRows("lobby");
            System.out.println("lobby table data cleared");

            //Create server socket
            serverSocket = new ServerSocket(9000);
            System.out.println("Server started...");

            while(true) {
                //Listen for new connection request
                Socket socket = serverSocket.accept();

                InetAddress address = socket.getInetAddress();
                System.out.println("Client " + clientNumber + " connected with host name " + address.getHostName());

                //generate a unused userid
                String user_id;
                do {
                    user_id = Utilities.generateCode();
                } while (user_ids.contains(user_id));
                user_ids.add(user_id);

                HandleClient handleClient;
                new Thread(handleClient = new HandleClient(socket, user_id)).start();

                //add the socket to the client map
                clientMap.put(user_id, handleClient);

                //increment client number
                clientNumber++;
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //sends a packet to all clients within a lobby
    private void sendPacketToLobby(String lobbyCode, Packet packet) {
        try {
            for (String user_id : lobbyMap.get(lobbyCode)){
                //send packet to every member of the lobby
                clientMap.get(user_id).outputToClient.writeObject(packet);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //sends a packet to all clients within a lobby
    private void sendPacketToLobbyHost(String lobbyCode, Packet packet) {
        try {
            for (String user_id : lobbyMap.get(lobbyCode)){
                if(hostMap.keySet().contains(user_id) && hostMap.get(user_id).equals(lobbyCode)) {
                    clientMap.get(user_id).outputToClient.reset();
                    //send packet to the lobby host
                    clientMap.get(user_id).outputToClient.writeObject(packet);
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //sends a packet to all clients currently connected to the server
    public void sendPacketToAllClients(Packet packet) {
        try {
            for (String user_id : clientMap.keySet()){
                //send packet to every currently registered client
                clientMap.get(user_id).outputToClient.writeObject(packet);
                System.out.println("Object sent to user_id: " + user_id);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //inner class to handle each client on a separate thread
    class HandleClient implements Runnable {

        private ObjectInputStream inputFromClient;
        private ObjectOutputStream outputToClient;
        private String user_id;
        private boolean isHost;

        //The connected socket
        private Socket socket;

        public HandleClient(Socket socket, String user_id) {
            this.socket = socket;
            this.user_id = user_id;
        }

        public void setHost(boolean isHost) {
            this.isHost = isHost;
        }

        public boolean isHost() {
            return isHost;
        }

        @Override
        public void run() {
            try {
                System.out.println("New thread created for socket: " + socket.getInetAddress().getHostName());

                //Create output stream from socket
                outputToClient = new ObjectOutputStream(socket.getOutputStream());

                //Create input stream from socket
                inputFromClient = new ObjectInputStream(socket.getInputStream());

                while(true){
                    //Read from input
                    Packet packetReceived = (Packet) inputFromClient.readObject();

                    handleReceivedPacket(packetReceived);
                }
            } catch(ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch(SocketException ex) {
                System.out.println("Connection reset/closed for client: " + socket.getInetAddress().getHostName());
            } catch(EOFException ex) {
                // System.out.println("We're catching this in the final block....");
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                removeUser();
            }
        }

        //Use this method to switch through and handle different received packets
        public void handleReceivedPacket(Packet packet) {
            try {
                switch(packet.getPacketType()) {
                    //packet type 0 = lobby creation
                    case 0:
                        String lobby_id;

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("We saw debug packet type 0");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 0);
                            returnPacket.setPlaylistURI(packet.getPlaylistURI());
                            returnPacket.setLobby("aaaa");
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        //find a lobby id not currently in use
                        do {
                            lobby_id = Utilities.generateCode();
                        } while (lobbyMap.keySet().contains(lobby_id));
                        lobbyMap.put(lobby_id, new ArrayList<>());
                        lobbyMap.get(lobby_id).add(user_id);
                        setHost(true);

                        boolean lobbyCreate = database.addLobby(lobby_id, packet.getPlaylistURI());
                        //if the lobby was create in the database successfully
                        if(lobbyCreate) {
                            //store the userid and the lobby code they are hosting
                            hostMap.put(user_id, lobby_id);
                            //add the user to the database
                            database.addUser(user_id, lobby_id, true);
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 0);
                            returnPacket.setPlaylistURI(packet.getPlaylistURI());
                            returnPacket.setLobby(lobby_id);
                            outputToClient.writeObject(returnPacket);
                        } else {
                            System.out.println("Failed to create lobby with id: " + lobby_id);
                        }
                        break;
                    //packet type 1 = lobby join
                    case 1:

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("Debug packet type 1");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 1);
                            returnPacket.setPlaylistURI("this:is:a:playlist:uri");
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        if(lobbyMap.keySet().contains(packet.getLobby())) {
                            lobbyMap.get(packet.getLobby()).add(user_id);

                            //need to edit the user to be in a lobby
                            database.addUser(user_id, packet.getLobby(), false);
                            String uri = database.getURI(packet.getLobby());
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 1);
                            returnPacket.setPlaylistURI(uri);
                            outputToClient.writeObject(returnPacket);

                            Packet returnPacketUserIds = new Packet(packet.getPacketIdentifier(), 5);
                            returnPacketUserIds.setUserIds(lobbyMap.get(packet.getLobby()));
                            returnPacketUserIds.setLobby(packet.getLobby());
                            sendPacketToLobbyHost(packet.getLobby(), returnPacketUserIds);
                        } else {
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 1);
                            outputToClient.writeObject(returnPacket);
                        }
                        break;
                    //packet type 2 = song update
                    case 2:

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("Debug packet type 2");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 2);
                            returnPacket.setSongURI(packet.getSongURI());
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        if(lobbyMap.keySet().contains(packet.getLobby())) {
                            boolean isInBlacklist = false;

                            String[] blacklist = database.getBlacklist(packet.getLobby());

                            for(int i = 0; i < blacklist.length; i++) {

                                if(blacklist[i].equals(packet.getSongURI())) {

                                    isInBlacklist = true;
                                    break;
                                }

                            }

                            if(!isInBlacklist) {
                                Packet returnPacket = new Packet(packet.getPacketIdentifier(), 2);
                                returnPacket.setLobby(packet.getLobby());
                                sendPacketToLobby(packet.getLobby(), returnPacket);
                            } else {
                                Packet sendBlackListPromptHost = new Packet(packet.getPacketIdentifier(), 7);
                                outputToClient.writeObject(sendBlackListPromptHost);
                            }
                        }
                        break;
                    //packet type 3 = user song update. send to host
                    case 3:

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("Debug packet type 3");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 3);
                            returnPacket.setSongURI(packet.getSongURI());
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        boolean isInBlacklist = false;

                        String[] blacklist = database.getBlacklist(packet.getLobby());

                        for(int i = 0; i < blacklist.length; i++) {

                            if(blacklist[i].equals(packet.getSongURI())) {

                                isInBlacklist = true;
                                break;
                            }

                        }
                        if(!isInBlacklist) {
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 3);
                            returnPacket.setSongURI(packet.getSongURI());
                            returnPacket.setLobby(packet.getLobby());
                            sendPacketToLobbyHost(packet.getLobby(), returnPacket);
                            System.out.println("Packet sent to lobby host of code: " + packet.getLobby());
                        } else {
                            Packet sendBlackListPromptUser = new Packet(packet.getPacketIdentifier(), 7);
                            outputToClient.writeObject(sendBlackListPromptUser);
                        }
                        break;
                    //packet type 4 = user disconnect to main landing page
                    case 4:

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("Debug packet type 4");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 4);
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        if(isHost()) {
                            Packet sendUsersToLandingPage = new Packet(packet.getPacketIdentifier(), 6);
                            sendUsersToLandingPage.setLobby(packet.getLobby());
                            sendPacketToLobby(packet.getLobby(), sendUsersToLandingPage);
                            removeUserLimited();
                        } else {
                            lobbyMap.get(packet.getLobby()).remove(user_id);
                            removeUserLimited();
                            //send update to host about a user returning to landing page
                            Packet returnPacketUserIds = new Packet(packet.getPacketIdentifier(), 5);
                            returnPacketUserIds.setUserIds(lobbyMap.get(packet.getLobby()));
                            returnPacketUserIds.setLobby(packet.getLobby());
                            sendPacketToLobbyHost(packet.getLobby(), returnPacketUserIds);
                        }
                        break;
                    //packet type 5 = update userid list
                    case 5:
                        if (debugBuild) {
                            System.out.println("Debug packet type 5");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 4);
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        break;

                    //packet type 6 = send users to landing page if host leaves
                    case 6:

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("Debug packet type 6");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 4);
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        if(isHost()) {
                            Packet sendUsersToLanding = new Packet(packet.getPacketIdentifier(), 6);
                            sendUsersToLanding.setLobby(packet.getLobby());
                            sendPacketToLobby(packet.getLobby(), sendUsersToLanding);
                        } else {
                            lobbyMap.get(packet.getLobby()).remove(user_id);
                            removeUser();
                            //send update to host about a user returning to landing page
                            Packet returnPacketUserIds = new Packet(packet.getPacketIdentifier(), 5);
                            returnPacketUserIds.setUserIds(lobbyMap.get(packet.getLobby()));
                            returnPacketUserIds.setLobby(packet.getLobby());
                            sendPacketToLobbyHost(packet.getLobby(), returnPacketUserIds);
                        }
                        break;

                    //packet type 7 = song is in black list
                    case 7:

                        if (debugBuild) {
                            System.out.println("Debug packet type 7");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 4);
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        break;

                    //packet type 8 = add to black list
                    case 8:

                        // DEBUG PYTHON TESTING

                        if (debugBuild) {
                            System.out.println("Debug packet type 8");
                            Packet returnPacket = new Packet(packet.getPacketIdentifier(), 4);
                            returnPacket.setLobby(packet.getLobby());
                            outputToClient.flush();
                            outputToClient.writeObject(returnPacket);
                            return;
                        }

                        // END DEBUG PYTHON TESTING

                        database.addBlacklist(packet.getBlackListURI(), packet.getLobby());
                        Packet added = new Packet(packet.getPacketIdentifier(), 8);
                        added.setLobby(packet.getLobby());
                        outputToClient.writeObject(added);
                        break;

                    default:
                        System.out.println("Packet Type Mismatch...");
                        break;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        //a method used to remove all instances of a specific user from memory and the database
        public void removeUser() {
            if(isHost == true) {
                //this if statement is order specific and needs to be handled carefully
                //send a lobby close packet to all clients

                //remove the lobby from the lobbymap
                lobbyMap.remove(hostMap.get(user_id));
                //if they are a host remove them from the hostmap
                hostMap.remove(user_id);
            }

            //make the id value available
            user_ids.remove(user_id);
            //remove user from clientMap
            clientMap.remove(user_id);
            //remove user from database
            database.removeUser(user_id);
            System.out.println("user data for user id: " + user_id + " removed.");
        }

        //a method used to remove all instances of a specific user from the database but keep their client connection
        public void removeUserLimited() {
            if(isHost == true) {
                //this if statement is order specific and needs to be handled carefully
                //send a lobby close packet to all clients

                //remove the lobby from the lobbymap
                lobbyMap.remove(hostMap.get(user_id));
                //if they are a host remove them from the hostmap
                hostMap.remove(user_id);
            }

            //remove user from database
            database.removeUser(user_id);
            System.out.println("user data for user id: " + user_id + " removed.");
            setHost(false);
        }
    }
}

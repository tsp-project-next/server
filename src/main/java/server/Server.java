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

    //client number serves no purpose other than to display how many users are connected
    private int clientNumber = 0;
    //a list of all currently taken user_ids
    private List<String> user_ids = Collections.synchronizedList(new ArrayList<>());

    //a list of all currently taken lobby_ids
    private List<String> lobby_ids = Collections.synchronizedList(new ArrayList<>());
    //Hashmap<user_id,client thread/object> the clients thread is stored by their user_id
    private ConcurrentHashMap<String,HandleClient> clientMap = new ConcurrentHashMap<>();

    public static void main(String args[]) {
        new Server();
    }

    public Server() {
        try {
            database = new Database();
            boolean connected = database.connectDB();
            //if the database is not connected then shut down the server
            if(connected == false) {
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

                String user_id;
                do {
                    user_id = Utilities.generateCode();
                } while (user_ids.contains(user_id));
                user_ids.add(user_id);
                database.addUser(user_id, null, false);

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

    //sends a packet to all clients currently connected to the server
    public void sendPacketToAllClients(String packetIdentifier, int packetType, String playlistURI, String songURI, String lobby) {
        try {
            //Create a packet to send
            Packet packet = new Packet(packetIdentifier, packetType, playlistURI, songURI, lobby);
            for (String user_id : clientMap.keySet()){
                //send packet to every currently registered client
                clientMap.get(user_id).outputToClient.writeObject(packet);
                System.out.println("Object sent to user_id: " + user_id);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //Use this method to switch through and handle different received packets
    public void handleReceivedPacket(Packet packet, ObjectOutputStream outputToClient, String user_id) {
        try {
            switch(packet.getPacketType()) {
                //packet type 0 = lobby creation
                case 0:
                    String lobby_id = null;
                    do {
                        lobby_id = Utilities.generateCode();
                    } while (lobby_ids.contains(lobby_id));
                    lobby_ids.add(lobby_id);
                    boolean lobbyCreate = database.addLobby(lobby_id, packet.getPlaylistURI());
                    if(lobbyCreate) {
                        //need to edit the user to be a host
                        database.editUser(user_id, lobby_id, true);
                        Packet returnPacket = new Packet(packet.getPacketIdentifier(), 0, packet.getPlaylistURI(), null, lobby_id);
                        outputToClient.writeObject(returnPacket);
                    } else {
                        System.out.println("Failed to create lobby with id: " + lobby_id);
                    }
                    break;
                //packet type 1 = lobby join
                case 1:
                    if(lobby_ids.contains(packet.getLobby())) {
                        //need to edit the user to be in a lobby
                        database.editUser(user_id, packet.getLobby(), false);
                        String uri = database.getURI(packet.getLobby());
                        Packet returnPacket = new Packet(packet.getPacketIdentifier(), 1, uri, null, null);
                        outputToClient.writeObject(returnPacket);
                    } else {
                        Packet returnPacket = new Packet(packet.getPacketIdentifier(), 1, null, null, null);
                        outputToClient.writeObject(returnPacket);
                    }
                    break;
                default:
                    System.out.println("Packet Type Mismatch...");
                    break;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class HandleClient implements Runnable {

        private ObjectInputStream inputFromClient;
        private ObjectOutputStream outputToClient;
        private String user_id;

        //The connected socket
        private Socket socket;

        public HandleClient(Socket socket, String user_id) {
            this.socket = socket;
            this.user_id = user_id;
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

                    handleReceivedPacket(packetReceived, outputToClient, user_id);
                }
            } catch(ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch(SocketException ex) {
                System.out.println("Connection reset/closed for client: " + socket.getInetAddress().getHostName());
                //remove a user from the database and list
            } catch(EOFException ex) {
                // System.out.println("We're catching this in the final block....");
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                clientMap.remove(user_id);
                database.removeUser(user_id);
            }
        }
    }
}

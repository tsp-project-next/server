package client;

import java.io.Serializable;
import java.util.ArrayList;

public class Packet implements Serializable {

    private String packetIdentifier = null;

    private int packetType = -1;

    private String playlistURI = null;

    private String songURI = null;

    private String lobby  = null;

    private ArrayList<String> userids = null;

    public Packet(String packetIdentifier, int packetType) {
        this.packetIdentifier = packetIdentifier;
        this.packetType = packetType;
    }

    public void setUserIds(ArrayList<String> userids) {
        this.userids = userids;
    }

    public ArrayList<String> getUserIds() {
        return userids;
    }

    public void setLobby(String str) {
        lobby = str;
    }

    public String getLobby() {
        return lobby;
    }

    public void setSongURI(String str) {
        songURI = str;
    }

    public String getSongURI() {
        return songURI;
    }

    public void setPlaylistURI(String str) {
        playlistURI = str;
    }

    public String getPlaylistURI() {
        return playlistURI;
    }

    public String getPacketIdentifier() {
        return packetIdentifier;
    }

    public int getPacketType() {
        return packetType;
    }

}

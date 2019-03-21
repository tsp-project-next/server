package client;

import java.io.Serializable;

public class Packet implements Serializable {

    private String packetIdentifier;

    private int packetType;

    private String playlistURI;

    private String songURI;

    private String lobby;

    private int flag;

    public Packet(String packetIdentifier, int packetType) {
        this.packetIdentifier = packetIdentifier;
        this.packetType = packetType;
    }

    public void setFlag(int flag) {
        this.flag = flag;
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

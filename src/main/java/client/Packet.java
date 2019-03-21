package client;

import java.io.Serializable;

public class Packet implements Serializable {

    private String packetIdentifier = null;

    private int packetType = -1;

    private String playlistURI = null;

    private String songURI = null;

    private String lobby  = null;

    public Packet(String packetIdentifier, int packetType, String playlistURI, String songURI, String lobby) {
        this.packetIdentifier = packetIdentifier;
        this.packetType = packetType;
        this.playlistURI = playlistURI;
        this.songURI = songURI;
        this.lobby = lobby;
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

package client;

import java.io.Serializable;

public class Packet implements Serializable {

    private String data, clientIdentifier;

    public Packet(String data, String cId) {
        this.data = data;
        this.clientIdentifier = cId;
    }

    public String getData() {
        return data;
    }

}

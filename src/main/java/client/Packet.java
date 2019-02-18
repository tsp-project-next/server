package client;

import java.io.Serializable;

public class Packet implements Serializable {

    // This class must be under the "client" package.
    // Its specification must match the clients exact same packet classpath
    // or you will get a ClassNotFoundException

    private String data;

    public Packet(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

}
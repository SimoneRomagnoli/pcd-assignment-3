package part2.rmi;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class Host implements Serializable {

    private String address;
    private int id;

    public Host(int id) {
        try {
            this.address = Inet4Address.getLocalHost().getHostAddress();
            this.id = id;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Host() {
        try {
            this.address = Inet4Address.getLocalHost().getHostAddress();
            this.id = 0;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public String getAddress() {
        return this.address;
    }

    public int getId() {
        return this.id;
    }

    public String toString() {
        return this.address + ":" + this.id;
    }

}

package part2.rmi.remote;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class RemoteHostImpl implements RemoteHost, Serializable {

    private String address;
    private int id;

    public RemoteHostImpl(int id) {
        try {
            this.address = Inet4Address.getLocalHost().getHostAddress();
            this.id = id;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public RemoteHostImpl() {
        try {
            this.address = Inet4Address.getLocalHost().getHostAddress();
            this.id = 0;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAddress() {
        return this.address;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.address + ":" + this.id;
    }

}

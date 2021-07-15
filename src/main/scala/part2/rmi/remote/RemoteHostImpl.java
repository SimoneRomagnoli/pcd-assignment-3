package part2.rmi.remote;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class RemoteHostImpl implements RemoteHost, Serializable {
    private String address;
    private int id;
    private boolean reachable;

    public RemoteHostImpl(int id) {
        try {
            this.address = Inet4Address.getLocalHost().getHostAddress();
            this.id = id;
            this.reachable = true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAddress()  {
        return this.address;
    }

    @Override
    public int getId()  {
        return this.id;
    }

    @Override
    public String toString() {
        return this.address + ":" + this.id;
    }

    @Override
    public boolean isReachable()  {
        return this.reachable;
    }

    @Override
    public void setUnreachable(){
        this.reachable = false;
    }

}

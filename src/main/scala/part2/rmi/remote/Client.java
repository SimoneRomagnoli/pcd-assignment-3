package part2.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Client extends Remote {

    void update() throws RemoteException;

}

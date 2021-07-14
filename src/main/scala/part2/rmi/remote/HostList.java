package part2.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HostList extends Remote {

    List<RemoteHost> getHostList() throws RemoteException;

    void join(RemoteHostImpl host) throws RemoteException;
}

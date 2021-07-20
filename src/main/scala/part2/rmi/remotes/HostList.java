package part2.rmi.remotes;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HostList extends Remote {

    List<RemoteHost> getHostList() throws RemoteException;

    void join(RemoteHost host) throws RemoteException;
}

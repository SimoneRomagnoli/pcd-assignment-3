package part2.rmi.remotes;

import java.rmi.RemoteException;
import java.util.List;

public interface HostList extends RemoteObject {

    List<RemoteHost> getHostList() throws RemoteException;

    void join(RemoteHost host) throws RemoteException;

    void remoteUpdate(List<RemoteHost> remoteHosts) throws RemoteException;
}

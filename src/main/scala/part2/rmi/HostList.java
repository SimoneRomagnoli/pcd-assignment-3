package part2.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HostList extends Remote {

    List<Host> getHostList() throws RemoteException;

    void join(Host host) throws RemoteException;
}

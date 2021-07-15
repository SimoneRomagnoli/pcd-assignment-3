package part2.rmi.remotes;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteHost extends Remote {

    String getAddress() ;

    int getId();

    String toString();

    boolean isReachable();

    void setUnreachable();
}

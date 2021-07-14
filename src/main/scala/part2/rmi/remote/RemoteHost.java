package part2.rmi.remote;

import java.rmi.Remote;

public interface RemoteHost extends Remote {

    String getAddress();

    int getId();

    String toString();
}

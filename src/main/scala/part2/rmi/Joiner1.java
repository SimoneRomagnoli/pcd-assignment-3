package part2.rmi;


import part2.rmi.controller.Controller;
import part2.rmi.puzzle.PuzzleBoard;
import part2.rmi.remotes.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Joiner1 {

    public static void main(String[] args) {
        try {
            //GET REMOTE REGISTRY
            Registry registry = LocateRegistry.getRegistry(Starter.REGISTRY_PORT);

            //WATCH HOST LIST ON FIRST HOST
            HostList remoteHostList = (HostList)registry.lookup("hostlist");
            final int id = remoteHostList.getHostList().size()+1;

            //CREATE LOCAL REGISTRY
            final int port = Starter.REGISTRY_PORT + id - 1;
            System.out.println("Starting with port "+port);
            LocateRegistry.createRegistry(port);

            //CREATE OWN HOST LIST
            HostList remoteHostListStub = (HostList) UnicastRemoteObject.exportObject(remoteHostList, 0);
            LocateRegistry.getRegistry(port).rebind("hostlist", remoteHostListStub);

            //CREATE OWN MODEL
            BoardStatus remoteBoard = (BoardStatus) LocateRegistry.getRegistry(Starter.REGISTRY_PORT).lookup("boardStatus");
            BoardStatus remoteBoardStub = (BoardStatus) UnicastRemoteObject.exportObject(remoteBoard, 0);
            LocateRegistry.getRegistry(port).rebind("boardStatus", remoteBoardStub);

            BoardStatus myBoard = (BoardStatus)LocateRegistry.getRegistry(port).lookup("boardStatus");

            Controller controller = new Controller(id, myBoard, remoteHostList);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

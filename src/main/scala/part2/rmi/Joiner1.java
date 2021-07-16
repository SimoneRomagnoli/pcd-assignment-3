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
            LocateRegistry.createRegistry(Starter.REGISTRY_PORT + id);

            //CREATE OWN HOST LIST
            HostList localHostList = new HostListImpl(remoteHostList);
            HostList localHostListStub = (HostList) UnicastRemoteObject.exportObject(localHostList, 0);
            LocateRegistry.getRegistry(Starter.REGISTRY_PORT+id).rebind("hostlist", localHostListStub);

            //CREATE OWN MODEL
            BoardStatus remoteBoard = (BoardStatus) LocateRegistry.getRegistry(Starter.REGISTRY_PORT).lookup("boardStatus");
            BoardStatus localBoard = new BoardStatusImpl(remoteBoard.getTiles(), id);
            BoardStatus localBoardStub = (BoardStatus) UnicastRemoteObject.exportObject(localBoard, 0);
            LocateRegistry.getRegistry(Starter.REGISTRY_PORT+id).rebind("boardStatus", localBoardStub);

            Controller controller = new Controller(id, localBoard, localHostList);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

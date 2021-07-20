package part2.rmi.controller;

import part2.rmi.Starter;
import part2.rmi.puzzle.PuzzleBoard;
import part2.rmi.puzzle.SerializableTile;
import part2.rmi.remotes.*;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import java.rmi.RemoteException;


public class Controller {

    private final RemoteObserver observer;

    private final PuzzleBoard puzzleBoard;

    private BoardStatus boardStatus;
    private HostList hostList;

    private int id;
    private List<RemoteHost> remoteHosts;
    private List<RemoteObserver> observers;

    public Controller(int id, BoardStatus boardStatus, HostList hl) {
        this.id = id;
        this.boardStatus = boardStatus;
        this.hostList = hl;

        this.puzzleBoard = new PuzzleBoard(this, id == 1);
        this.puzzleBoard.setVisible(true);
        this.observer = new RemoteObserverImpl(this);

        try {
            UnicastRemoteObject.exportObject(observer, 0);
            this.boardStatus.addRemoteObserver(this.observer);

            RemoteHost localhost = new RemoteHostImpl(id);
            this.hostList.join(localhost);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void select(SerializableTile tile) throws RemoteException, NotBoundException {
        try {
            this.boardStatus.select(tile, this.id);
        } catch (RemoteException starterFailed) {
            BoardStatus board = new BoardStatusImpl(this.puzzleBoard.getSerializableTiles());
            BoardStatus boardStub = (BoardStatus) UnicastRemoteObject.exportObject(board, 0);
            this.boardStatus = board;
            this.boardStatus.addRemoteObserver(this.observer);
            Registry registry = LocateRegistry.getRegistry(Starter.REGISTRY_PORT + this.id - 1);
            registry.rebind("boardStatus", boardStub);
            for(RemoteObserver o:observers) {
                o.newObjectOwner(Starter.REGISTRY_PORT + this.id - 1);
            }
            //Propagator propagator = new Propagator(this.id, this.remoteHosts);
            //propagator.propagate();
            select(tile);
        }
    }

    public void update() {
        try {
            this.puzzleBoard.updateBoard();
            this.updateHostList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateHostList() throws RemoteException {
        this.remoteHosts = new ArrayList<>(this.hostList.getHostList());
    }

    public void loadInitialBoard(List<SerializableTile> tiles) {
        try {
            this.boardStatus.loadCurrentTiles(tiles);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<SerializableTile> getSerializableTiles() throws RemoteException {
        return this.boardStatus.getTiles();
    }

    public void updateRemoteObservers(List<RemoteObserver> observers) {
        this.observers = new ArrayList<>(observers);
    }

    public void updateRemoteObjects(int remotePort) {
        try {
            Registry remoteRegistry = LocateRegistry.getRegistry(remotePort);
            final int localPort = Starter.REGISTRY_PORT + this.id - 1;
            Registry localRegistry = LocateRegistry.getRegistry(localPort);

            HostList remoteHostList = (HostList) remoteRegistry.lookup("hostlist");
            HostList remoteHostListStub = (HostList) UnicastRemoteObject.exportObject(remoteHostList, 0);
            localRegistry.rebind("hostlist", remoteHostListStub);

            BoardStatus remoteBoard = (BoardStatus) remoteRegistry.lookup("boardStatus");
            BoardStatus remoteBoardStub = (BoardStatus) UnicastRemoteObject.exportObject(remoteBoard, 0);
            LocateRegistry.getRegistry(localPort).rebind("boardStatus", remoteBoardStub);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

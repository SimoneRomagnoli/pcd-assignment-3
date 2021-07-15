package part2.rmi.remotes;

import part2.rmi.controller.Controller;

import java.io.Serializable;

public class LocalObserver implements  Serializable {

    private final Controller controller;

    public LocalObserver(Controller controller) {
        this.controller = controller;
    }

    public void update() {
        this.controller.update();
    }
}

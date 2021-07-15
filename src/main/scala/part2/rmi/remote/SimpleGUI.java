package part2.rmi.remote;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SimpleGUI extends JFrame {

    private JPanel panel;
    private int id;
    private JButton btn;
    private Controller controller;

    public SimpleGUI(int id, Controller controller) {
        this.id = id;
        this.controller = controller;
        setTitle("JFrame "+id);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.panel = new JPanel();
        this.panel.setBorder(BorderFactory.createLineBorder(Color.gray));
        this.panel.setLayout(new FlowLayout());
        getContentPane().add(this.panel, BorderLayout.CENTER);

        try {
            this.btn = new JButton(String.valueOf(controller.getValue()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        this.btn.addActionListener(al -> {
            try {
                this.controller.inc();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //btn.setText(String.valueOf(controller.getValue()));
        });

        this.panel.add(this.btn);
        setVisible(true);
        pack();
    }

    private void drawButton() {
        try {
            System.out.println("In drawing button i got counter "+this.controller.getValue());
            btn.setText(String.valueOf(this.controller.getValue()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateVal() {
        SwingUtilities.invokeLater(this::drawButton);
    }
}

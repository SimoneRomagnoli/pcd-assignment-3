package part2.rmi.remote;

import javax.swing.*;
import java.awt.*;

public class ExampleBoard extends JFrame {

    private int number = 0;
    private JPanel board;
    private JLabel label;

    public ExampleBoard() {
        setTitle("example");
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        board = new JPanel();
        board.setBorder(BorderFactory.createLineBorder(Color.gray));
        board.setLayout(new GridLayout(3, 5, 0, 0));
        getContentPane().add(board, BorderLayout.CENTER);

        label = new JLabel(String.valueOf(number));
        label.setBounds(30, 30, 30, 30);
        board.add(label);
        pack();
    }

    public void inc() {
        board.removeAll();
        this.number++;
        label.setText(String.valueOf(number));
        label.setBounds(300, 300, 300, 300);
        board.add(label);
        pack();
    }

}

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import randomreverser.RandomReverser;
import randomreverser.util.Rand;

public class DropperCracker extends JFrame {

    private final int SIZE = 16;

    private DropperCracker() {

        JPanel p1 = new JPanel();
        p1.setLayout(new GridLayout(SIZE, SIZE,0,0));

        boolean[][] checkBoxes = new boolean[SIZE][SIZE];
        ArrayList<JCheckBox> cbs = new ArrayList<>(SIZE*SIZE);

        for (int i = 0; i < 16*16; i++) {
            JCheckBox checkbox = new JCheckBox();
            checkbox.addActionListener((ActionEvent event) -> {
                for (int row = 0; row < SIZE; row++) {
                    for (int col = 0; col < SIZE; col++) {
                        checkBoxes[row][col] = cbs.get(row*SIZE+col).isSelected();
                    }
                }
            });
            cbs.add(checkbox);
            p1.add(checkbox);
        }

        JLabel s = new JLabel("Seed: ");
        JButton fire = new JButton("Crack");
        JButton reset = new JButton("Reset");
        JButton copy = new JButton("Copy");

        copy.addActionListener((ActionEvent e) -> {
            try {
                String seed = s.getText().split(" ")[1];
                Long.parseLong(seed);
                StringSelection stringSelection = new StringSelection(s.getText().split(" ")[1]);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException ee) {
                System.err.println(ee.getMessage());
            }
        });
        copy.setEnabled(false);

        fire.addActionListener((ActionEvent e) -> {
            ArrayList<Integer> indices = getSuccessfulIndices(checkBoxes);
            if (indices.size() < 14) {
                s.setText("Not Enough Info to Crack");
                return;
            }
            RandomReverser device = new RandomReverser();
            int last = 0;
            int count = 0;
            for (int j: indices) {
                if (count < 24) {
                    device.consumeNextFloatCalls(j - last - 1);
                    device.addNextIntCall(8, 0, 0);
                }
                last = j;
                count++;
            }
            AtomicInteger k = new AtomicInteger(0);
            device.findAllValidSeeds().forEach(m -> {
                Rand r = Rand.ofInternalSeed(m);
                r.advance(2048);
                s.setText("Seed: " + r.getSeed());
                copy.setEnabled(true);
                k.incrementAndGet();
            });
            if (k.get() == 0) {
                s.setText("Could Not Find Seeds");
                copy.setEnabled(false);
            } else if (k.get() > 1) {
                s.setText("No Unique Seed Identified");
                copy.setEnabled(false);
            }
        });

        reset.addActionListener((ActionEvent e) -> {
            for (int row = 0; row < SIZE; row++)
                for (int col = 0; col < SIZE; col++) {
                    checkBoxes[row][col] = false;
                    cbs.get(row*SIZE+col).setSelected(false);
                    s.setText("Seed: ");
                    copy.setEnabled(false);
                }
        });

        setLayout(new FlowLayout());

        add(p1);
        add(reset);
        add(fire);
        add(copy);
        add(s);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Dropper Cracker");
        setSize(400,425);
        setVisible(true);
    }

    private ArrayList<Integer> getSuccessfulIndices(boolean[][] checkboxes) {
        ArrayList<Integer> callIndices = new ArrayList<>();
        int count = 0;
        for (int i = SIZE - 1; i >= 0; i--) {
            if ((i & 1) == 0) {
                for (int j = 0; j < SIZE; j++) {
                    count += 8;
                    if (checkboxes[i][j]) {
                        callIndices.add(count);
                    }
                }
            } else {
                for (int j = SIZE - 1; j >= 0; j--) {
                    count += 8;
                    if (checkboxes[i][j]) {
                        callIndices.add(count);
                    }
                }
            }
        }
        return callIndices;
    }

    public static void main(String args[])
    {
        new DropperCracker();
    }
}
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
    private final int ITEMS_PER_DROPPER = 8;
    private final int WINDOW_WIDTH = 400;
    private final int WINDOW_HEIGHT = 425;

    private DropperCracker() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(SIZE, SIZE));

        boolean[][] litLamps = new boolean[SIZE][SIZE];
        ArrayList<JCheckBox> checkboxes = new ArrayList<>(SIZE*SIZE);

        for (int cbox = 0; cbox < SIZE*SIZE; cbox++) {
            JCheckBox checkbox = new JCheckBox();
            checkbox.addActionListener((ActionEvent event) -> {
                for (int row = 0; row < SIZE; row++) {
                    for (int col = 0; col < SIZE; col++) {
                        litLamps[row][col] = checkboxes.get(row*SIZE+col).isSelected();
                    }
                }
            });
            checkboxes.add(checkbox);
            panel.add(checkbox);
        }

        JLabel output = new JLabel("Seed: ");
        JButton crack = new JButton("Crack");
        JButton reset = new JButton("Reset");
        JButton copy = new JButton("Copy");

        copy.setEnabled(false);
        copy.addActionListener((ActionEvent event) -> {
            try {
                String seed = output.getText().split(" ")[1];
                Long.parseLong(seed);
                StringSelection stringSelection = new StringSelection(output.getText().split(" ")[1]);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                System.err.println("Tried to copy but couldn't find a seed!");
                System.err.println(e.getMessage());
            }
        });


        crack.addActionListener((ActionEvent event) -> {
            ArrayList<Integer> indices = getSuccessfulIndices(litLamps);

            if (indices.size() < 14) {
                output.setText("Not Enough Info to Crack");
                return;
            }

            RandomReverser device = new RandomReverser();
            int lastSuccessfulIndex = 0;
            int count = 0;
            for (int callIndex: indices) {
                if (count < 24) {
                    device.consumeNextFloatCalls(callIndex - lastSuccessfulIndex - 1);
                    device.addNextIntCall(ITEMS_PER_DROPPER, 0, 0);
                }
                lastSuccessfulIndex = callIndex;
                count++;
            }

            AtomicInteger seedsFound = new AtomicInteger(0);

            device.findAllValidSeeds().forEach(m -> {
                Rand randAdvancer = Rand.ofInternalSeed(m);
                randAdvancer.advance(SIZE*SIZE*ITEMS_PER_DROPPER);
                output.setText("Seed: " + randAdvancer.getSeed());
                copy.setEnabled(true);
                seedsFound.incrementAndGet();
            });

            if (seedsFound.get() == 0) {
                output.setText("Could Not Find Seeds");
                copy.setEnabled(false);
            } else if (seedsFound.get() > 1) {
                output.setText("No Unique Seed Identified");
                copy.setEnabled(false);
            }
        });

        reset.addActionListener((ActionEvent event) -> {
            for (int row = 0; row < SIZE; row++)
                for (int col = 0; col < SIZE; col++) {
                    litLamps[row][col] = false;
                    checkboxes.get(row*SIZE+col).setSelected(false);
                    output.setText("Seed: ");
                    copy.setEnabled(false);
                }
        });

        setLayout(new FlowLayout());

        add(panel);
        add(reset);
        add(crack);
        add(copy);
        add(output);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Dropper Cracker");
        setSize(WINDOW_WIDTH,WINDOW_HEIGHT);
        setVisible(true);
    }

    private ArrayList<Integer> getSuccessfulIndices(boolean[][] checkboxes) {
        ArrayList<Integer> callIndices = new ArrayList<>();
        int count = 0;

        // snake upwards starting from the lower right
        // example on 3x3:
        //
        //  9 8 7
        //  4 5 6
        //  3 2 1
        for (int i = SIZE - 1; i >= 0; i--) {
            //the snake alternates iterating left to right and right to left. Last row always right to left.
            if ((i % 2) == (SIZE % 2)) {
                //this row goes from left to right
                for (int j = 0; j < SIZE; j++) {
                    count += ITEMS_PER_DROPPER;
                    if (checkboxes[i][j]) {
                        callIndices.add(count);
                    }
                }
            } else {
                //this row goes from right to left
                for (int j = SIZE - 1; j >= 0; j--) {
                    count += ITEMS_PER_DROPPER;
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
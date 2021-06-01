import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.util.concurrent.atomic.AtomicLong;

import randomreverser.RandomReverser;
import randomreverser.util.Rand;
import sun.jvm.hotspot.types.JBooleanField;

public class DropperCracker extends JFrame {

    private final int SIZE = 16;
    private final int ITEMS_PER_DROPPER = 8;
    private final int WINDOW_WIDTH = 750;
    private final int WINDOW_HEIGHT = 450;
    private boolean seedSet = false;

    private DropperCracker() {

        JPanel lampPanel = new JPanel();
        lampPanel.setLayout(new GridLayout(SIZE, SIZE));

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
            lampPanel.add(checkbox);
        }

        JPanel dropperPanel = new JPanel();
        dropperPanel.setLayout(new GridLayout(3,3, 3, 3));

        JFormattedTextField[][] dropperSlots = new JFormattedTextField[3][3];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                NumberFormat format = NumberFormat.getInstance();
                NumberFormatter formatter = new NumberFormatter(format);
                formatter.setValueClass(Integer.class);
                formatter.setMinimum(0);
                formatter.setMaximum(64);
                formatter.setAllowsInvalid(true);
                JFormattedTextField textField = new JFormattedTextField(formatter);
                textField.setText("0");
                textField.setValue(0);
                textField.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "" + (3*row + col+1)));
                textField.setHorizontalAlignment(JTextField.CENTER);
                textField.setPreferredSize(new Dimension(110,110));
                textField.setFont(textField.getFont().deriveFont(30.0f));
                dropperSlots[row][col] = textField;
                dropperPanel.add(textField);
            }
        }

        JLabel output = new JLabel("Seed: ");
        JLabel firedItem = new JLabel("");

        JButton crack = new JButton("Crack");
        JButton reset = new JButton("Reset");
        JButton copy = new JButton("Copy Seed");
        JButton fire_dropper = new JButton("Fire Dropper");

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
            AtomicLong seed = new AtomicLong(0);

            device.findAllValidSeeds().forEach(m -> {
                Rand randAdvancer = Rand.ofInternalSeed(m);
                randAdvancer.advance(SIZE*SIZE*ITEMS_PER_DROPPER);
                output.setText("Seed: " + randAdvancer.getSeed());
                seed.set(randAdvancer.getSeed());
                copy.setEnabled(true);
                seedsFound.incrementAndGet();
            });

            if (seedsFound.get() == 0) {
                output.setText("Could Not Find Seeds");
                copy.setEnabled(false);
            } else if (seedsFound.get() > 1) {
                output.setText("No Unique Seed Identified");
                copy.setEnabled(false);
            } else {
                //dropper = new Dropper(new  int[9]);
                Dropper.setSeed(seed.get());
                seedSet = true;
                //DocGame docGame = new DocGame(seed.get());
                //StringBuilder builder = new StringBuilder("");
                //for (int i = 0; i < 20; i++) {
                //    builder.append(docGame.runGame());
                //    builder.append(" ");
                //}
                //firedItem.setText(builder.toString());
            }
        });

        reset.addActionListener((ActionEvent event) -> {
            for (int row = 0; row < SIZE; row++)
                for (int col = 0; col < SIZE; col++) {
                    litLamps[row][col] = false;
                    checkboxes.get(row*SIZE+col).setSelected(false);
                    output.setText("Seed: ");
                    copy.setEnabled(false);
                    firedItem.setText("");
                    seedSet = false;
                }
        });

        fire_dropper.addActionListener((ActionEvent event) -> {
            if (seedSet) {
                int[] contents = new int[9];
                for (int row = 0; row < 3; row++)
                    for (int col = 0; col < 3; col++) {
                        JTextField field = dropperSlots[row][col];
                        int slot = Integer.parseInt(field.getText());
                        contents[row * 3 +col] = slot;
                    }
                Dropper dropper = new Dropper(contents);
                int i = dropper.chooseNonEmptySlot();
                if (i != -1) {
                    JTextField modifiedSlot = dropperSlots[i / 3][i % 3];
                    modifiedSlot.setText((Integer.parseInt(modifiedSlot.getText()) - 1) + "");
                    firedItem.setText("Fired from slot " + (i + 1));
                } else {
                    firedItem.setText("Can't fire with no items!");
                }
            } else {
                firedItem.setText("Can't fire with no seed!");
            }
        });

        setLayout(new FlowLayout());

        lampPanel.setBorder(BorderFactory.createTitledBorder("Lamps"));
        dropperPanel.setBorder(BorderFactory.createTitledBorder("Dropper"));


        add(lampPanel);
        add(dropperPanel);
        add(output);
        add(reset);
        add(crack);
        add(copy);
        add(fire_dropper);
        add(firedItem);
        //add(dropperPanel);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Dropper Cracker");
        setAlwaysOnTop(true);
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

    public static void main(String[] args)
    {
        new DropperCracker();
    }
}
import java.util.Random;

public class Dropper {
    private final int[] contents;
    private static final Random random = new Random();

    Dropper(int[] contents) {
        this.contents = contents;
    }

    public static void setSeed(long seed) {
        random.setSeed(seed ^ 0x5deece66dL);
    }

    public int chooseNonEmptySlot() {
        int i = -1;
        int j = 1;

        for(int k = 0; k < this.contents.length; ++k) {
            if (contents[k] > 0 && random.nextInt(j++) == 0) {
                i = k;
            }
        }

        return i;
    }

    public int removeFromNonEmptySlot() {
        int i = chooseNonEmptySlot();
        contents[i]--;
        return i;
    }
}

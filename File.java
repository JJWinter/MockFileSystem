
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
public class File {

    String name;
    int size;
    ArrayList<Integer> blockList = new ArrayList<Integer>();
    int blockLocation;
    int byteLocation;

    public File(String name) {
        this.name = name;
        this.size = size;

    }

    public int size() {
        return size;
    }

    public void setBlock(int index) {
        blockLocation = index;
    }

    public void setIndex(int index) {
        byteLocation = index;
    }

    public int getBlock() {
        return blockLocation;
    }

    public int getIndex() {
        return byteLocation;
    }

    public int[] getBlocks() {
        int[] blocks = new int[blockList.size()];
        for (int i = 0; i < blockList.size(); i++) {
            blocks[i] = blockList.get(i);
        }
        return blocks;
    }
}

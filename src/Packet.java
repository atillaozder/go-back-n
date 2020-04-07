import java.io.Serializable;
import java.util.Arrays;

public class Packet implements Serializable {

    public enum PacketType {
        ACKED,
        SENT,
        CORRUPTED
    }

    private int sequenceNo;
    private byte[] data;
    private PacketType type;
    private boolean last;
    public Packet(int sequenceNo, PacketType type) {
        this.sequenceNo = sequenceNo;
        this.type = type;
    }

    public Packet(int sequenceNo, byte[] data, PacketType type, boolean last) {
        this.sequenceNo = sequenceNo;
        this.data = data;
        this.type = type;
        this.last = last;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    @Override
    public String toString() {
        return "Packet {" + "seqNo=" + sequenceNo + ", type=" + type + ", length=" + data.length +
                ", last=" + last + ", data=" + Arrays.toString(data) + "}";
    }
}

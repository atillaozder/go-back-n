import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class Receiver {

    // max byte length that one datagram packet contains
    // (packet to byte array increases the size of received byte array)
    private static final int MSS = 1186;

    public static void main(String args[]) throws Exception {

        String portStr = args[2];
        int port = Integer.parseInt(portStr);

        DatagramSocket serverSocket = new DatagramSocket(port);
        byte[] receivedData = new byte[MSS];

        String str = new Timestamp(new Date().getTime()) + ", Bound: " + serverSocket.isBound()
                + ", Port: " + serverSocket.getLocalPort();
        System.out.println(str);

        List<Packet> packetList = new ArrayList<>();
        int waitingForPacket = 0;

        BufferedWriter logWriter = new BufferedWriter(new FileWriter("receiver.log"));
        logWriter.write(str);
        logWriter.newLine();

        while (true) {
            DatagramPacket receive = new DatagramPacket(receivedData, receivedData.length);
            serverSocket.receive(receive);

            String debugMsg = new Timestamp(new Date().getTime()) + " ";

            // *********************** CHECKSUM CONTROL ********************************** //

            // Byte Array to packet object (Received Packet)
            ByteArrayInputStream b = new ByteArrayInputStream(receive.getData());
            ObjectInputStream o = new ObjectInputStream(b);
            Packet receivedPacket = (Packet) o.readObject();

            int offset = (MSS * receivedPacket.getSequenceNo()) / 8;
            debugMsg += "[recv data] " + receivedPacket.getSequenceNo() + " " + offset + " "
                    + receivedPacket.getData().length;

            // Control if the received packet is in order
            if (receivedPacket.getSequenceNo() == waitingForPacket) {
                debugMsg += " ACCEPTED";
                waitingForPacket++;
                packetList.add(receivedPacket);
            } else {
                debugMsg += " IGNORED";
            }

            System.out.println(debugMsg);
            logWriter.write(debugMsg);
            logWriter.newLine();

            // Packet object to byte array (ACKed Packet)
            Packet ackPacket = new Packet(receivedPacket.getSequenceNo(), Packet.PacketType.ACKED);
            ByteArrayOutputStream ackOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(ackOutputStream);
            objectOutputStream.writeObject(ackPacket);
            byte[] packetData = ackOutputStream.toByteArray();

            InetAddress IPAddress = receive.getAddress();
            int receivedPacketPort = receive.getPort();
            DatagramPacket ACK = new DatagramPacket(packetData, packetData.length, IPAddress, receivedPacketPort);
            serverSocket.send(ACK);

            if (receivedPacket.isLast()) {
                break;
            }
        }

        // Get the last packet that was send and total byte length required (sequence number * MSS)
        Packet lastPkt = packetList.get(packetList.size() - 1);
        String fileName = new String(lastPkt.getData());

        int totalLength = 0;
        for (Packet pkt : packetList) {
            totalLength += pkt.getData().length;
        }

        byte[] totalBytes = new byte[totalLength];
        // Merge all received packets bytes to one byte array and write it to local disk
        try (FileOutputStream stream = new FileOutputStream(System.getProperty("user.dir") + "/" + fileName)) {
            ByteBuffer target = ByteBuffer.wrap(totalBytes);
            for (Packet pkt : packetList) {
                if (!pkt.isLast())
                    target.put(pkt.getData());
            }

            stream.write(totalBytes);
        }

        String debugMsg = new Timestamp(new Date().getTime()) + " [completed]";
        System.out.println(debugMsg);
        logWriter.write(debugMsg);
        logWriter.newLine();

        logWriter.close();
    }
}
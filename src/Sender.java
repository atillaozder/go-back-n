
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.*;

class Sender {

    private static final int WINDOW_SIZE = 4; // max number of pkts that can be send in pipelining
    private static final int MSS = 1000; // max byte length that one packet contains

    private static List<Packet> packetList = new ArrayList<>(); // Packets that will be sent
    private static int lastPacketSent = 0;
    private static int waitingForACK = 0;
    private static BufferedWriter logWriter;

    public static void main(String args[]) {
        String address = args[2];
        String path = args[4];

        int IPIdx = address.indexOf(":");
        String IP = address.substring(0, IPIdx);
        String portStr = address.substring(IPIdx + 1, address.length());

        try {
            logWriter = new BufferedWriter(new FileWriter("sender.log"));

            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(IP);
            int port = Integer.parseInt(portStr);
            System.out.println("IP: " + IPAddress + ", Port: " + port + ", File Path: " + path);

            // Read file from user and convert to byte array
            File file = new File(path);
            byte[] bytes = new byte[(int) file.length()];

            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytes);

            int from = 0;
            int to = MSS;
            int seqNo = 0;

            // Prepare all packets dividing whole byte to smaller pieces according to MSS
            // Example - If there are 4029 byte readed from file and MSS = 1000
            // There will be 4 Packets (1000, 1000, 1000, 29) byte
            while (from < bytes.length) {
                byte[] data = Arrays.copyOfRange(bytes, from, to);
                Packet packet = new Packet(seqNo, data, Packet.PacketType.SENT, false);

                packetList.add(packet);
                seqNo++;
                from += MSS;
                if ((to + MSS) < bytes.length) {
                    // To get the whole part (1000)
                    to += MSS;
                } else {
                    // To get the part that is smaller than 1000 (29)
                    to += bytes.length - to;
                }
            }

            // Last packet contains information about file name and extension of file
            String name = file.getName();
            byte[] data = name.getBytes();
            Packet lastPacket = new Packet(seqNo, data, Packet.PacketType.SENT, true);
            packetList.add(lastPacket);

            ACKWaiter ackWaiter = new ACKWaiter(clientSocket, IPAddress, port);
            Thread ackerThread = new Thread(ackWaiter);
            ackerThread.start();

            // Initially send 4 (WINDOW_SIZE) packet and wait for response
            resentPackets(clientSocket, IPAddress, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ACKWaiter implements Runnable {

        private DatagramSocket clientSocket;
        private InetAddress IPAddress;
        private int port;

        ACKWaiter(DatagramSocket clientSocket, InetAddress IPAddress, int port) {
            this.clientSocket = clientSocket;
            this.IPAddress = IPAddress;
            this.port = port;
        }

        @Override
        public void run() {
            // If timeout occurs resent all packets in the current window
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (waitingForACK != lastPacketSent) {
                        lastPacketSent = waitingForACK;
                        resentPackets(clientSocket, IPAddress, port);
                    }
                }
            };

            // The task will be started after 5 secs and
            // for every 5 seconds the task will be continuously executed.....
            timer.scheduleAtFixedRate(timerTask, 5000, 5000);

            while (waitingForACK < packetList.size()) {
                try {
                    // ACK of sending packets
                    byte[] ackBytes = new byte[1500];
                    DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
                    clientSocket.receive(ack);

                    // Byte Array to packet object (ACKed Packet)
                    ByteArrayInputStream b = new ByteArrayInputStream(ack.getData());
                    ObjectInputStream o = new ObjectInputStream(b);
                    Packet ackPacket = (Packet) o.readObject();

                    int offset = (MSS * ackPacket.getSequenceNo()) / 8;
                    String debugMsg = new Timestamp(new Date().getTime()) + " [recv ack] "
                            + ackPacket.getSequenceNo() + " " + offset;
                    System.out.println(debugMsg);
                    logWriter.write(debugMsg);
                    logWriter.newLine();

                    // If ACK has type ACKED then it means packet is delivered one way or another
                    // If ACK has type CORRUPTED then it means checksum error resent all packets
                    if (ackPacket.getType() == Packet.PacketType.ACKED) {
                        if (ackPacket.getSequenceNo() == waitingForACK) waitingForACK++;
                        else lastPacketSent = waitingForACK;
                    } else {
                        lastPacketSent = waitingForACK;
                    }

                    if (lastPacketSent < packetList.size()) {
                        Packet packet = packetList.get(lastPacketSent);
                        sendPacket(packet, clientSocket, IPAddress, port);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            String comp = new Timestamp(new Date().getTime()) + " [completed]";
            System.out.println(comp);
            try {
                logWriter.write(comp);
                logWriter.newLine();
                logWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.exit(0);
        }
    }

    // Sends given packet to given address
    private static void sendPacket(Packet pkt, DatagramSocket clientSocket, InetAddress IPAddress, int port) {
        try {
            // Packet object to byte array
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(pkt);
            byte[] packetData = b.toByteArray();

            // Debug Message
            int offset = (MSS * pkt.getSequenceNo()) / 8;
            String debugMsg = new Timestamp(new Date().getTime()) + " [send data] "
                    + pkt.getSequenceNo() + " " + offset + " " + pkt.getData().length;
            System.out.println(debugMsg);
            logWriter.write(debugMsg);
            logWriter.newLine();

            // Send Packet over UDP
            DatagramPacket sendingPacket = new DatagramPacket(packetData, packetData.length, IPAddress, port);
            clientSocket.send(sendingPacket);
            lastPacketSent++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Resent all packets in the current window
    private static void resentPackets(DatagramSocket clientSocket, InetAddress IPAddress, int port) {
        int packetsFrom = lastPacketSent;
        int packetsTo = lastPacketSent + WINDOW_SIZE;

        if (packetsTo > packetList.size()) {
            packetsTo = packetList.size();
        }

        // Loop to send all pkts in current window
        for (int i = packetsFrom; i < packetsTo; i++) {
            Packet pkt = packetList.get(i);
            sendPacket(pkt, clientSocket, IPAddress, port);
        }
    }
}

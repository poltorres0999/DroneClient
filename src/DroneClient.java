import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class DroneClient {

    private String ip;
    private int telemetryPort;
    private int commandPort;

    private DatagramSocket commandSock;
    private DatagramSocket telemetrySock;
    private InetAddress address;

    private byte[] commandBuf;
    private byte[] telemetryBuf;

    private boolean connectionStarted = false;


    public void DroneClient(String ip, int telemetryPort, int commandPort) throws UnknownHostException, SocketException {

        this.ip = ip;
        this.telemetryPort = telemetryPort;
        this.commandPort = commandPort;
        this.commandBuf = new byte[40];
        this.telemetryBuf = new byte[40];
        this.address = InetAddress.getByName(ip);
        this.commandSock = new DatagramSocket(commandPort);
    }

    public void startConnection () throws IOException {

        byte[] response = new byte[40];

        if (!this.connectionStarted) {

            this.createPackage(300,1,0);

            DatagramPacket packet = new DatagramPacket(this.commandBuf, this.commandBuf.length);
            long startTime = System.nanoTime();

            while (!this.connectionStarted || System.nanoTime() - startTime > 5) {
                commandSock.receive(packet);
                response = packet.getData();
                if (packet != null) {

                    if (this.getCode(response) == 300) {
                        this.connectionStarted = true;
                    }
                }
            }
        }
    }

    public void setRc() throws IOException {

    }

    private DatagramPacket createPackage(int code, int size, int data) {

        int[] packageData = new int []{code, size, data};

        ByteBuffer byteBuffer = ByteBuffer.allocate(packageData.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(packageData);

        this.commandBuf = byteBuffer.array();

        DatagramPacket packet = new DatagramPacket(this.commandBuf, this.commandBuf.length, this.address, 4445);

        return packet;

    }

    private int getInt(byte[] arr, int off) {
        return arr[off]<<8 &0xFF00 | arr[off+1]&0xFF;
    }

    private int getCode(byte[] response) {
        return this.getInt(response, 0);
    }







}

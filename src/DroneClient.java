import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class DroneClient {


    private static final int START_CONNECTION = 300;
    private static final int END_CONNECTION = 301;
    private static final int ARM = 220;
    private static final int DISARM = 221;
    private static final int START_TELEMETRY = 120;
    private static final int END_TELEMETRY = 121;
    private static final int RAW_IMU = 102;
    private static final int SERVO = 103;
    private static final int MOTOR = 104;
    private static final int RC = 105;
    private static final int ATTITUDE = 108;
    private static final int ALTITUDE = 109;
    private static final int SET_RC = 200;

    private String ip;
    private int telemetryPort;
    private int commandPort;

    private DatagramSocket commandSock;
    private DatagramSocket telemetrySock;
    private InetAddress address;

    private byte[] commandBuf;
    private byte[] telemetryBuf;

    private boolean connectionStarted;
    private boolean telemetryActive;


    public void DroneClient(String ip, int telemetryPort, int commandPort) throws UnknownHostException, SocketException {

        this.ip = ip;
        this.telemetryPort = telemetryPort;
        this.commandPort = commandPort;
        this.commandBuf = new byte[40];
        this.telemetryBuf = new byte[40];
        this.address = InetAddress.getByName(ip);
        this.commandSock = new DatagramSocket(commandPort);
        this.connectionStarted = false;
        this.telemetryActive = false;
    }

    public void startConnection () {

        if (!this.connectionStarted) {

            byte[] response;

            short[] data = new short[]{0};

            DatagramPacket packet = this.createPackage((short) START_CONNECTION, (short)1, data);
            try {
                this.commandSock.send(packet);

                long startTime = System.nanoTime();

                while (!this.connectionStarted || System.nanoTime() - startTime >= 5) {
                    commandSock.receive(packet);
                    response = packet.getData();
                    if (packet != null) {

                        if (this.getCode(response) == START_CONNECTION) {
                            this.connectionStarted = true;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void EndConnection() {

        if (this.connectionStarted) {
            DatagramPacket packet = this.createPackage((short)END_CONNECTION, (short)0, new short[]{0});
            try {
                this.commandSock.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setRc(short roll, short pitch, short yaw, short throttle) {

        short[] data = new short[]{roll, pitch, yaw, throttle};

        DatagramPacket packet = this.createPackage((short)SET_RC, (short)8, data);
        try {
            this.commandSock.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startTelemetry () {

        if (!this.telemetryActive) {

            DatagramPacket packet = this.createPackage((short)START_TELEMETRY, (short)0, new short[]{0});
            try {

                byte[] response;

                this.commandSock.send(packet);

                long startTime = System.nanoTime();

                while (!this.telemetryActive || System.nanoTime() - startTime >= 5) {
                    commandSock.receive(packet);
                    response = packet.getData();
                    if (packet != null) {

                        if (this.getCode(response) == START_TELEMETRY) {
                            this.telemetryActive = true;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void stopTelemetry() {

        if (this.telemetryActive) {
            DatagramPacket packet = this.createPackage((short)END_TELEMETRY, (short)0, new short[]{0});
            try {
                this.commandSock.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private DatagramPacket createPackage(short code, short size, short[] data) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + size * 2);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(code);
        shortBuffer.put(size);
        shortBuffer.put(data);

        this.commandBuf = byteBuffer.array();

        DatagramPacket packet = new DatagramPacket(this.commandBuf, this.commandBuf.length,
                this.address, this.commandPort);

        return packet;
    }

    private void evaluateTelemetry(short code, short size, byte[] data) {



    }

    private short getShort(byte[] arr, int off) {
        return (short) (arr[off]<<8 &0xFF00 | arr[off+1]&0xFF);
    }

    private short getCode(byte[] response) {
        return this.getShort(response, 0);
    }

    private short getSize(byte[] response) {
        return this.getShort(response, 2);
    }


    private short[] getTelemtryValues(short size, byte[] data) {



    }



}

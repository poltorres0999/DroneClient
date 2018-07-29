import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.FutureTask;

public class DroneClient {

    private static final short START_CONNECTION = 300;
    private static final short CONNECTION_ACCEPTED = 301;
    private static final short END_CONNECTION = 302;
    private static final short ARM = 220;
    private static final short DISARM = 221;
    private static final short START_TELEMETRY = 120;
    private static final short TELEMETRY_ACCEPTED = 121;
    private static final short END_TELEMETRY = 122;
    private static final short RAW_IMU = 102;
    private static final short SERVO = 103;
    private static final short MOTOR = 104;
    private static final short RC = 105;
    private static final short ATTITUDE = 108;
    private static final short ALTITUDE = 109;
    private static final short SET_RC = 200;

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

    private TelemetryThread telemetryThread;


    public DroneClient(String ip, int commandPort, int telemetryPort) throws UnknownHostException, SocketException {

        this.ip = ip;
        this.telemetryPort = telemetryPort;
        this.commandPort = commandPort;
        this.commandBuf = new byte[40];
        this.telemetryBuf = new byte[40];
        this.address = InetAddress.getByName(ip);
        this.commandSock = new DatagramSocket();
        this.connectionStarted = false;
        this.telemetryActive = false;
    }

    public void startConnection () {

        if (!this.connectionStarted) {

            byte[] response;

            short[] data = new short[]{0};

            DatagramPacket packet = this.createPackage(START_CONNECTION, (short)2, data);
            DatagramPacket receivePacket = new DatagramPacket(this.commandBuf, this.commandBuf.length);
            try {
                this.commandSock.send(packet);

                System.out.println("Start connection command sent");

                long startTime = System.nanoTime();
                long currentTime = System.nanoTime();

                while (!this.connectionStarted) {
                    currentTime = System.nanoTime();
                    commandSock.receive(receivePacket);
                    response = packet.getData();
                    if (packet != null) {

                        System.out.println(new String(packet.getData(), 0, packet.getLength()));

                        if (this.getCode(response) == CONNECTION_ACCEPTED) {
                            this.connectionStarted = true;
                            System.out.println("Connection started!");
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void endConnection() {

        if (this.connectionStarted) {
            DatagramPacket packet = this.createPackage(END_CONNECTION, (short)2, new short[]{0});
            try {
                this.commandSock.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startTelemetry () {

        if (!this.telemetryActive) {

            DatagramPacket packet = this.createPackage(START_TELEMETRY, (short)2, new short[]{0});

            try {

                //telemetrySock = new DatagramSocket();
                telemetrySock=commandSock;

                byte[] response;

                this.telemetrySock.send(packet);
                System.out.println("Start telemetry command sent\n");

                while (!this.telemetryActive) {
                    telemetrySock.receive(packet);
                    response = packet.getData();
                    System.out.print("Waiting for telemetry response...\n");
                    if (response != null) {

                        if (this.getCode(response) == TELEMETRY_ACCEPTED) {
                            this.telemetryActive = true;
                            System.out.print("Telemetry started!\n");

                        }
                    }
                }

                if (this.telemetryActive) {

                    this.telemetryThread = new TelemetryThread();
                    this.telemetryThread.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void stopTelemetry() {

        if (this.telemetryActive) {
            DatagramPacket packet = this.createPackage(END_TELEMETRY, (short)2, new short[]{0});
            try {
                this.commandSock.send(packet);
                System.out.println("Stop telemetry command sent");
                this.telemetryActive = false;
                telemetrySock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void evaluateTelemetry(short code, short size, byte[] data) {

        if (code == RAW_IMU) {
            short[] rawImu = this.getTelemetryValues(size, data);

            System.out.println("-----RAW_IMU-----\n");
            System.out.format("ACC -> accx: %h, accy: %h, accy: %h\n", rawImu[0], rawImu[1], rawImu[2]);
            System.out.format("GYRO -> gyrx: %h, gyry: %h, gyrz: %h\n", rawImu[3], rawImu[4], rawImu[5]);
            System.out.format("MAG -> magx: %h, magy: %h, magz: %h\n", rawImu[6], rawImu[7], rawImu[8]);

        }

        if (code == SERVO) {
            short[] servo = this.getTelemetryValues(size, data);

            System.out.println("-----SERVO-----\n");
            System.out.format("SERVOS -> s1: %h, s2: %h, s3: %h, s4: %h\n", servo[0], servo[1], servo[2], servo[3]);

        }

        if (code == MOTOR) {
            short[] motor = this.getTelemetryValues(size, data);
            System.out.println("-----MOTOR-----\n");
            System.out.format("MOTORS -> m1: %h, m2: %h, m3: %h, m4: %h\n", motor[0], motor[1], motor[2], motor[3]);
        }

        if (code == RC) {
            short[] rc = this.getTelemetryValues(size, data);
            System.out.println("-----RC-----\n");
            System.out.format("RC -> roll: %h, pitch: %h, yaw: %h, throttle: %h\n", rc[0], rc[1], rc[2], rc[3]);
        }

        if (code == ATTITUDE) {
            short[] attitude = this.getTelemetryValues(size, data);
            System.out.println("-----ATTITUDE-----\n");
            System.out.format("ALTITUDE -> angx: %h, angy: %h, heading: %h\n", attitude[0], attitude[1], attitude[2]);
        }

        if (code == ALTITUDE) {
            short[] altitude = this.getTelemetryValues(size, data);
            System.out.println("-----ALTITUDE-----\n");
            System.out.format("ALTITUDE -> estalt: %h, vario: %h\n", altitude[0], altitude[1]);
        }

    }

    public void setRc(short roll, short pitch, short yaw, short throttle) {

        short[] data = new short[]{roll, pitch, yaw, throttle};

        DatagramPacket packet = this.createPackage(SET_RC, (short)8, data);
        try {
            this.commandSock.send(packet);
            System.out.println("Set_rc command sent\n");
            System.out.format("Values -> roll: %h, pitch: %h, yaw: %h, throttle: %h", roll, pitch , yaw, throttle);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ARM () {

        DatagramPacket packet = this.createPackage(ARM, (short)2, new short[]{0});
        try {
            this.commandSock.send(packet);
            System.out.println("ARM command sent\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void DISARM () {

        DatagramPacket packet = this.createPackage(DISARM, (short)2, new short[]{0});
        try {
            this.commandSock.send(packet);
            System.out.println("DISARM command sent\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramPacket createPackage(short code, short size, short[] data) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + size * 2);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(code);
        shortBuffer.put(size);
        shortBuffer.put(data);

        System.out.println("Package created, values: " + byteBuffer.toString());


        this.commandBuf = byteBuffer.array();
        System.out.println(String.valueOf(this.getCode(this.commandBuf)));

        DatagramPacket packet = new DatagramPacket(this.commandBuf, this.commandBuf.length,
                this.address, this.commandPort);

        return packet;
    }

    private short[] getTelemetryValues(short size, byte[] data) {

        int j = 0;
        short[] values = new short[size];
        for (int i = 0; i < size; i+=2) {
            values[j] = this.getShort(data,i);
            j++;
        }

        return values;

    }

    private short getCode(byte[] response) {
        return this.getShort(response, 0);
    }

    private short getSize(byte[] response) {
        return this.getShort(response, 2);
    }

    private short getShort(byte[] arr, int off) {
        return (short) (arr[off]<<8 &0xFF00 | arr[off+1]&0xFF);
    }

    private class TelemetryThread extends Thread {

        @Override
        public void run() {

            System.out.println("RUN THREAD");

            DatagramPacket receivePacket = new DatagramPacket(telemetryBuf, telemetryBuf.length);

            while (telemetryActive && !telemetrySock.isClosed()) {

                try {

                    telemetrySock.receive(receivePacket);

                    byte[] telemetryResponse = receivePacket.getData();

                    if (telemetryResponse != null) {

                        short code = getCode(telemetryResponse);
                        System.out.println("CODE:" + String.valueOf(code));
                        short size = getSize(telemetryResponse);
                        byte[] data = Arrays.copyOfRange(telemetryResponse, 3, size + 3);

                        evaluateTelemetry(code, size, data);
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                }

            }

        }

    }
}

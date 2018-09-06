import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

public class DroneClientServer implements AutoCloseable {

    private final DatagramSocket datagramSocket;
    private final InetSocketAddress inetSocketAddress;
    private final Thread telemetryThread = new Thread(this::onTelemetryThreadRun);
    private boolean telemetryStarted;

    private Consumer<DroneSegment> onTelemetryCallback;

    public DroneClientServer(final InetSocketAddress inetSocketAddress){
        try {
            this.datagramSocket=new DatagramSocket();
            this.datagramSocket.setSoTimeout(1000);
            this.telemetryStarted = false;
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        this.inetSocketAddress=inetSocketAddress;
    }

    public void start() throws IOException {
        this.datagramSocket.connect(inetSocketAddress.getAddress(),inetSocketAddress.getPort());
        this.startConnection();
    }

    // Defines the behaviour to be executed when the telemetry is active.

    public void setOnTelemetryCallback(final Consumer<DroneSegment> onTelemetryCallback) {
        this.onTelemetryCallback = onTelemetryCallback;
    }

    @Override
    public void close() throws IOException {
        this.telemetryThread.stop();
        this.stopTelemetry();
        this.stopConnection();
        this.datagramSocket.close();
    }

    public void sendArm() throws IOException {
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)220);
        this.datagramSocket.send(datagramPacket);
    }

    public void sendDiasArm() throws IOException {
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)221);
        this.datagramSocket.send(datagramPacket);
    }

    public void sendSetRC(short roll, short pitch, short yaw, short throttle) throws IOException {
        final short[] payload = new short[]{roll, pitch, yaw, throttle};
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)200,(short)8,payload);

        this.datagramSocket.send(datagramPacket);
        this.datagramSocket.receive(datagramPacket);

    }

    // Initializes the communication with the MultiWii Server.

    private void startConnection() throws IOException {
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)300);

        synchronized (this.datagramSocket) {
            this.datagramSocket.send(datagramPacket);
            this.datagramSocket.receive(datagramPacket);
        }

        if (new DroneSegment(datagramPacket.getData()).code != 301) {
            throw new RuntimeException("Error: received package is not a accept connection package!");
        }

    }

    // Starts the telemetry thread.

    public void startTelemetry() throws IOException {
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)120);

        if (!this.telemetryStarted) {
            this.datagramSocket.send(datagramPacket);
            this.datagramSocket.receive(datagramPacket);

            if (new DroneSegment(datagramPacket.getData()).code == 121) {
                this.telemetryStarted = true;
                this.telemetryThread.start();
            }
        }
    }

    // Stops the telemetry thread.

    public void stopTelemetry() throws IOException{
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)122);
        this.datagramSocket.send(datagramPacket);
    }

    // Method that defines the behaviour of the telemetry thread.

    private void onTelemetryThreadRun() {
        while (true){
            final DatagramPacket datagramPacket=new DatagramPacket(new byte[40],40);
            try {
                this.datagramSocket.receive(datagramPacket);
                if(this.onTelemetryCallback!=null){
                    final DroneSegment droneSegment=new DroneSegment(datagramPacket.getData());
                    new Thread(() -> onTelemetryCallback.accept(droneSegment)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopConnection() throws IOException {
        final DatagramPacket datagramPacket=this.generateDatagramPacket((short)302);
        this.datagramSocket.send(datagramPacket);
    }

    // The two methods above are used to convert the information of a communication package to a datagram package.
    // The first one is related to the commands that have no data information to be send. The second one is used when
    // data information is needed.

    private DatagramPacket generateDatagramPacket(final short code) {
        return this.generateDatagramPacket(code, (short) 2, new short[]{(short)0});
    }

    private DatagramPacket generateDatagramPacket(final short code, final short size, final short[] payload){
        final ByteBuffer byteBuffer=ByteBuffer.allocate(4 + size);
        final ShortBuffer shortBuffer=byteBuffer.asShortBuffer();
        shortBuffer.put(code);
        shortBuffer.put(size);
        shortBuffer.put(payload);
        final byte[] buffer=byteBuffer.array();
        return new DatagramPacket(buffer,buffer.length);
    }

    public static class DroneSegment {

        public final short code;

        public final short size;

        public final short[] payload;

        /*Represents the structure of a communication package between the MultiWii client and sever. Requires the code
        * of the command to be send, the size in bytes of the data, and the data. Is used to easily pack and unpack
        * the communication packages. */

        public DroneSegment(final short code, final short size, final short[] payload){
            this.code=code;
            this.size=size;
            this.payload=payload;
        }

        public DroneSegment(final byte[] bytes){
            this.code=(short) (bytes[0]<<8 &0xFF00 | bytes[1]&0xFF);
            this.size=(short) (bytes[2]<<8 &0xFF00 | bytes[2+1]&0xFF);
            this.payload=new short[(bytes.length-4)/2];
            for(int i=0;(i*2)<bytes.length-4;i++){
                this.payload[i]=(short) (bytes[(i*2)+4]<<8 &0xFF00 | bytes[(i*2)+4+1]&0xFF);
            }
        }

        public byte[] toBytes(){
            final byte[] result=new byte[2+2+this.payload.length*2];
            //struct to result
            return result;
        }

        public static class RawImu {

            public short accx;
            public short accy;
            public short accz;
            public short gyrx;
            public short gyry;
            public short gyrz;
            public short magx;
            public short magy;
            public short magz;

            public RawImu (short[] payload) {
                this.accx = payload[0];
                this.accy = payload[1];
                this.accz = payload[2];
                this.gyrx = payload[3];
                this.gyry = payload[4];
                this.gyrz = payload[5];
                this.magx = payload[6];
                this.magy = payload[7];
                this.magz = payload[8];
            }
        }

        public static class Altitude {
            public short estalt;
            public short vario;

            public Altitude(short[] payload) {
                this.estalt = payload[0];
                this.vario = payload[1];
            }
        }

        public static class Attitude {
            public short angx;
            public short angy;
            public short heading;

            public Attitude(short[] payload) {
                this.angx = payload[0];
                this.angy = payload[1];
                this.heading = payload[2];
            }
        }

        public static class Rc {
            public short roll;
            public short pitch;
            public short yaw;
            public short throttle;

            public Rc(short[] payload) {
                this.roll = payload[0];
                this.pitch = payload[1];
                this.yaw = payload[2];
                this.throttle = payload[3];
            }
        }

        public static class Motor {
            public short m1;
            public short m2;
            public short m3;
            public short m4;

            public Motor(short[] payload) {
                this.m1 = payload[0];
                this.m2 = payload[1];
                this.m3 = payload[2];
                this.m4 = payload[3];
            }
        }

        public static class Servo {
            public short s1;
            public short s2;
            public short s3;
            public short s4;

            public Servo(short[] payload) {
                this.s1 = payload[0];
                this.s2 = payload[1];
                this.s3 = payload[2];
                this.s4 = payload[3];
            }
        }
    }

}

import sun.plugin.dom.exception.InvalidStateException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ExecuteClient {

    private static final int command_port = 4445;
    private static final int telemetry_port = 4446;
    private static final String ip = "192.168.0.162";

    public static void main (String[] args) throws SocketException, UnknownHostException, InterruptedException {
        try(DroneClientServer droneClientServer=new DroneClientServer(new InetSocketAddress(ip,4445))){
            droneClientServer.setOnTelemetryCallback((droneSegment)->{
                switch (droneSegment.code){
                    case 102:
                        printRawImu(new DroneClientServer.DroneSegment.RawImu(droneSegment.payload));
                        break;
                    case 108:
                        printAttitude(new DroneClientServer.DroneSegment.Attitude(droneSegment.payload));
                        break;
                    case 105:
                        printRc(new DroneClientServer.DroneSegment.Rc(droneSegment.payload));
                        break;
                    case 109:
                        printAltitude(new DroneClientServer.DroneSegment.Altitude(droneSegment.payload));
                        break;
                    default:
                        throw new InvalidStateException("Im fucked");
                }

                System.out.println("CODE: "+droneSegment.code);
                System.out.println("Size: "+ droneSegment.size);
                System.out.println("Payload: "+Arrays.toString(droneSegment.payload));
            });
            droneClientServer.start();
            //dronClientServer.send Arm();
            droneClientServer.startTelemetry();
            Thread.sleep((long)1000000000);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void printAttitude(DroneClientServer.DroneSegment.Attitude attitude) {
        System.out.println("-----ATTITUDE-----\n");
        System.out.format("ALTITUDE -> angx: %h, angy: %h, heading: %h\n", attitude.angx, attitude.angy, attitude.heading);
    }

    private static void printAltitude(DroneClientServer.DroneSegment.Altitude altitude) {

        System.out.println("-----ALTITUDE-----\n");
        System.out.format("ALTITUDE -> estalt: %h, vario: %h\n", altitude.estalt, altitude.vario);

    }

    private static void printRc(DroneClientServer.DroneSegment.Rc rc) {

        System.out.println("-----RC-----\n");
        System.out.format("RC -> roll: %h, pitch: %h, yaw: %h, throttle: %h\n", rc.roll, rc.pitch, rc.pitch, rc.yaw);
    }

    private static void printRawImu(DroneClientServer.DroneSegment.RawImu rawImu) {

        System.out.println("-----RAW_IMU-----\n");
        System.out.format("ACC -> accx: %h, accy: %h, accy: %h\n", rawImu.accx, rawImu.accy, rawImu.accz);
        System.out.format("GYRO -> gyrx: %h, gyry: %h, gyrz: %h\n", rawImu.gyrx, rawImu.gyry, rawImu.gyrz);
        System.out.format("MAG -> magx: %h, magy: %h, magz: %h\n", rawImu.magx, rawImu.magy, rawImu.magz);

    }

}

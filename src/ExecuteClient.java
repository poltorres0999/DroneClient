import java.net.SocketException;
import java.net.UnknownHostException;

public class ExecuteClient {

    private static final int command_port = 4445;
    private static final int telemetry_port = 4446;
    private static final String ip = "192.168.0.162";


    public static void main (String[] args) throws SocketException, UnknownHostException, InterruptedException {


        short roll = 1500;
        short pitch = 1500;
        short yaw = 1500;
        short throttle = 1500;

        DroneClient droneClient = new DroneClient(ip, command_port, telemetry_port);

        droneClient.startConnection();
        droneClient.ARM();
        //Waits until drone is armed to continue sending commands
        Thread.sleep(5000);

        droneClient.startTelemetry();

        long startTime = System.nanoTime();

        while (System.nanoTime() - startTime >= 10) {


            droneClient.setRc(roll, pitch, yaw, throttle);

            roll += 2;
            pitch += 2;
            yaw += 2;
            throttle +=2;

            Thread.sleep(200);
        }

        droneClient.DISARM();
        droneClient.stopTelemetry();
        droneClient.endConnection();

    }

}

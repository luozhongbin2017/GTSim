/*
 * TrafficSimulationDesktopApp.java
 */

package trafficsimulationdesktop;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class TrafficSimulationDesktopApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new TrafficSimulationDesktopView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of TrafficSimulationDesktopApp
     */
    public static TrafficSimulationDesktopApp getApplication() {
        return Application.getInstance(TrafficSimulationDesktopApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
//
//        TrafficSimulationDesktopView.KR0 = Integer.parseInt(args[0]);
//        TrafficSimulationDesktopView.KR1 = Integer.parseInt(args[1]);
//        TrafficSimulationDesktopView.KR2 = Integer.parseInt(args[2]);
//        TrafficSimulationDesktopView.KR3 = Integer.parseInt(args[3]);
//        TrafficSimulationDesktopView.KR4 = Integer.parseInt(args[4]);
//        TrafficSimulationDesktopView.KR5 = Integer.parseInt(args[5]);
//        TrafficSimulationDesktopView.KR6 = Integer.parseInt(args[6]);
//        TrafficSimulationDesktopView.KR7 = Integer.parseInt(args[7]);
//        TrafficSimulationDesktopView.KR8 = Integer.parseInt(args[8]);
//        TrafficSimulationDesktopView.KR9 = Integer.parseInt(args[9]);
//        TrafficSimulationDesktopView.KR10 = Integer.parseInt(args[10]);

//        TrafficSimulationDesktopView.flushUpOcc0 = Integer.parseInt(args[0]);
//        TrafficSimulationDesktopView.flushUpOcc1 = Integer.parseInt(args[1]);
//        TrafficSimulationDesktopView.flushUpOcc2 = Integer.parseInt(args[2]);
//        TrafficSimulationDesktopView.flushUpOcc3 = Integer.parseInt(args[3]);
//        TrafficSimulationDesktopView.flushUpOcc4 = Integer.parseInt(args[4]);
//        TrafficSimulationDesktopView.flushUpOcc5 = Integer.parseInt(args[5]);
//        TrafficSimulationDesktopView.flushUpOcc6 = Integer.parseInt(args[6]);
//        TrafficSimulationDesktopView.flushUpOcc7 = Integer.parseInt(args[7]);
//        TrafficSimulationDesktopView.flushUpOcc8 = Integer.parseInt(args[8]);
//        TrafficSimulationDesktopView.flushUpOcc9 = Integer.parseInt(args[9]);
//        TrafficSimulationDesktopView.flushUpOcc10 = Integer.parseInt(args[10]);
//
//        TrafficSimulationDesktopView.flushDownOcc0 = Integer.parseInt(args[22]);
//        TrafficSimulationDesktopView.flushDownOcc1 = Integer.parseInt(args[23]);
//        TrafficSimulationDesktopView.flushDownOcc2 = Integer.parseInt(args[24]);
//        TrafficSimulationDesktopView.flushDownOcc3 = Integer.parseInt(args[25]);
//        TrafficSimulationDesktopView.flushDownOcc4 = Integer.parseInt(args[26]);
//        TrafficSimulationDesktopView.flushDownOcc5 = Integer.parseInt(args[27]);
//        TrafficSimulationDesktopView.flushDownOcc6 = Integer.parseInt(args[28]);
//        TrafficSimulationDesktopView.flushDownOcc7 = Integer.parseInt(args[29]);
//        TrafficSimulationDesktopView.flushDownOcc8 = Integer.parseInt(args[30]);
//        TrafficSimulationDesktopView.flushDownOcc9 = Integer.parseInt(args[31]);
//        TrafficSimulationDesktopView.flushDownOcc10 = Integer.parseInt(args[32]);

        launch(TrafficSimulationDesktopApp.class, args);
    }
}

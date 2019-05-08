/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package trafficsimulationdesktop;

/**
 *
 * @author bhargavarama
 */
public class Alinea {

    // Constructor
    public Alinea() {}
  
   /*
    * Send Alinea Metering Rate
    */
    public static double AlineaMR(double currMR, double optDen, double currDen, int kr){
        return ((int)(currMR+kr*(optDen-currDen)));
    }    
}

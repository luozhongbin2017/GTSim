package trafficsimulationdesktop;
/*
 * Math library for the similation
 */

public class myMath {
    
    // Constructor
    public myMath() {
    }
  
   /*
    * Max - int
    */
    public static int max(int ... arr){
        int m = arr[0];
        for (int a : arr){
            if (a > m) m = a;
        }
        return m;
    }
    
   /*
    * Min - Int
    */
    public static int min(int ... arr){
        int m = arr[0];
        for (int a : arr){
            if (a < m) m = a;
        }
        return m;
    }
    
   /*
    * Max - Double
    */
    public static double max(double ... arr){
        double m = arr[0];
        for (double a : arr){
            if (a > m) m = a;
        }
        return m;
    }
    
   /*
    * Min - Double
    */
    public static double min(double ... arr){
        double m = arr[0];
        for (double a : arr){
            if (a < m) m = a;
        }
        return m;
    }
    
   /*
    * Middle of three double values
    */
    public static double middle (double a, double b, double c){
        double v = b;        
        if (b<a) {
            v=a;
        } else if(b>c) {
            v=c;
        }
        return v;
    }
    
   /*
    * Calculate Veq
    */
    public static double Veq(double s, double w, double kj, double u){
        double k = 1 / s;
        double q = w * (kj-k);
        return middle(0,q/k,u);
    }
    
   /*
    * Demand
    */
    public static double demand(double k, double u, double ko){
        return u * min(k,ko);
    }
    
   /*
    * Supply
    */
    public static double supply(double k, double cap, double w, double kj){
        return max(min(cap, w * (kj - k)),0);
    }
}
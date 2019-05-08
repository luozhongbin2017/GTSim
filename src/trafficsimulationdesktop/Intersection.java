/*
 * The Intersection class represents a road Intersection or Start or Destination point
 */

package trafficsimulationdesktop;

/**
 * @author Bharat R
 */

public class Intersection implements java.io.Serializable{
   public static final int RAMP = 0;                                          //On Ramp with no lane addition for 1 lane entrance
   public static final int EXIT = 1;                                          //Off Ramp with no lane drop for 1 lane exit
   public static final int SIGNALIZED = 2;
   public static final int FOUR_WAY = 3;                                       // Unsignamized
   public static final int ORIGIN = 4;
   public static final int DESTINATION = 5;
   public static final int ORIGIN_AND_DEST = 6;
   public static final int TWO_WAY = 7;
   public static final int GENERIC = 8;
   public static final int RAMP2 = 9;   //On Ramp with Lane Addition for 1 lane entrance
   public static final int EXIT2 = 10;  //Off Ramp with LaneDrop for 1 lane exit
   public static final int EXIT3 = 11;  //Off Ramp with Two Lanes Drop for 2 lane exit   
   
   // Basic Properties
   double lat;
   double lon;
   String intersectionName;
   int type;
//   int IntId;

   // Newly added properties
   double gore;
   //double meterRate;
   double sigDist;
   double greenTime;
   double redTime;
   double offset;
   double cycleTime;                   //Calculated Field
   boolean hasStopSign;

  /*
   * Set default properties
   */
   private void setDefaultSpecProperties(){
       this.gore = 70/1000.0;
//       this.meterRate = 1500;
       this.sigDist = 100;
       this.greenTime = 30;
       this.redTime = 30; 
       this.offset = 5;
       this.cycleTime = this.greenTime + this.redTime + this.offset;
       this.hasStopSign = true;
   }

   /*
   * Constructor
   */
   public Intersection(double lat, double lon, String name, int type){
       this.lat = lat;
       this.lon = lon;
       this.intersectionName = name;
       this.type = type;
//       this.IntId = NodeId;
//       NodeId++;

       setDefaultSpecProperties();
   }

   /*
   * Constructor
   */
   public Intersection(double lat, double lon, String name){
       //this(lat, lon, name, Intersection.SIGNALIZED);
       this(lat, lon, name, Intersection.GENERIC);
   }

   /*
   * Set OnRamp properties
   */
   public void setOnRampProps(double gore, double meterRate, double sigDist){
       setDefaultSpecProperties();

       this.gore = gore;
//       this.meterRate = meterRate;
       this.sigDist = sigDist;
   }

   /*
   * Set OffRamp properties
   */
   public void setOffRampProps(double gore){
       setDefaultSpecProperties();

       this.gore = gore;
   }

   /*
   * Set Signalized properties
   */
   public void setSignalizedProps(double greenTime, double redTime, double offset){
       setDefaultSpecProperties();

       this.greenTime = greenTime;
       this.redTime = redTime;
       this.offset = offset;
       this.cycleTime = greenTime + redTime + offset;
   }

  /*
   * Set Unsignalized properties
   */
   public void setUnsignalizedProps(boolean hasStopSign){
       setDefaultSpecProperties();

       this.hasStopSign = hasStopSign;
   }

  /*
   * java.io.Serializable overridden function
   */
   private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException{
       out.writeDouble(lat);
       out.writeDouble(lon);
       out.writeObject(intersectionName);
       out.writeInt(type);

       //New Properties
       out.writeDouble(gore);
//       out.writeDouble(meterRate);
       out.writeDouble(sigDist);
       out.writeDouble(greenTime);
       out.writeDouble(redTime);
       out.writeDouble(offset);
       out.writeBoolean(hasStopSign);
   }

  /*
   * java.io.Serializable overridden function
   */
   private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException{
       this.lat = in.readDouble();
       this.lon = in.readDouble();
       this.intersectionName = (String)in.readObject();
       this.type = in.readInt();

       //New Properties
       this.gore = in.readDouble();
//       this.meterRate = in.readDouble();
       this.sigDist = in.readDouble();
       this.greenTime = in.readDouble();
       this.redTime = in.readDouble();
       this.offset = in.readDouble();
       this.hasStopSign = in.readBoolean();

       this.cycleTime = this.greenTime + this.redTime + this.offset;
   }

  /*
   * java.lang.Object overridden function
   */
   @Override
   public boolean equals(Object obj){
       Intersection intscn = (Intersection)obj;
       if((intscn.lat == this.lat) && (intscn.lon == this.lon))
           return true;
       else
           return false;
   }

   @Override
   public int hashCode() {
       int hash = 7;
       hash = 59 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
       hash = 59 * hash + (int) (Double.doubleToLongBits(this.lon) ^ (Double.doubleToLongBits(this.lon) >>> 32));
       return hash;
   }

   @Override
   public String toString(){
       return intersectionName;
   }
}


/*
 * Essentially a "Pair" of Waypoints
 */

package trafficsimulationdesktop;

/**
 * @author Bharat R
 */
public class WaypointConnection{
    WaypointIntersection wp1;
    WaypointIntersection wp2;
    
    // Constructor
    public WaypointConnection(WaypointIntersection wp1, WaypointIntersection wp2){
        this.wp1 = wp1;
        this.wp2 = wp2;
    }
    
    // java.lang.Object overridden functions
    @Override
    public boolean equals(Object obj){
        if((wp1.equals(((WaypointConnection)obj).wp1)) && (wp2.equals(((WaypointConnection)obj).wp2)))
            return true;
        else
            return false;
    }
    
    @Override
    public int hashCode(){
        int sum = (int)Math.abs(wp1.getPosition().getLatitude()*1000 ) + (int)Math.abs(wp1.getPosition().getLongitude()*1000) + (int)Math.abs(wp2.getPosition().getLatitude()*1000 ) + (int)Math.abs(wp2.getPosition().getLongitude()*1000) ;
        return sum;
    }
}
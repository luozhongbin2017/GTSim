/*
 * Wrapper over Waypoint
 */

package trafficsimulationdesktop;

import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.Waypoint;

/**
 *
 * @author Bharat R
 */
public class WaypointIntersection extends Waypoint{
    
    // Constructor
    public WaypointIntersection(GeoPosition geopos){
        super(geopos);
    }
    
    // Constructor
    public WaypointIntersection(double lat, double lon){
        super(lat, lon);
    }

    // java.lang.Object overridden functions
    @Override
    public boolean equals(Object obj){
        if((this.getPosition().getLatitude() == ((WaypointIntersection)obj).getPosition().getLatitude()) && (this.getPosition().getLongitude() == ((WaypointIntersection)obj).getPosition().getLongitude()))
            return true;
        else
            return false;
    }
    
    @Override
    public int hashCode(){
        int sum = (int)Math.abs(this.getPosition().getLatitude()*1000 ) + (int)Math.abs(this.getPosition().getLongitude()*1000);
        return sum;
    }
}

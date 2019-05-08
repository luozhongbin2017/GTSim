/*
 * JFileFilter.java
 *
 * Created on October 22, 2007, 5:04 PM
 *
 */

package trafficsimulationdesktop;

import javax.swing.filechooser.FileFilter;

/**
 * File filter
 */
public class JFileFilter extends FileFilter {
    
    String description;
    String extension;
    
    /** Creates a new instance of JFileFilter */
    public JFileFilter(String description, String extension) {
        this.description = description;
        this.extension = extension;
    }
    
    public boolean accept(java.io.File f) {
        return (f.isFile() && f.getName().toLowerCase().endsWith(extension));
    }
    
    public String getDescription() {
        return description;
    }
}

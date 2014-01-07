package polly.rx.core.orion;

import java.util.Collection;

import polly.rx.core.orion.model.Quadrant;
import polly.rx.core.orion.model.Sector;


public interface QuadrantProvider extends QuadrantListener {
    
    public Collection<String> getAllQuadrantNames();
    
    public Collection<? extends Sector> getEntryPortals();
    
    public Quadrant getQuadrant(Sector sector);
    
    public Quadrant getQuadrant(String name);
    
    public Collection<? extends Quadrant> getAllQuadrants();
}
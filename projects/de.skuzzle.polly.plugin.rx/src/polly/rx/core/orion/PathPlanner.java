package polly.rx.core.orion;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import polly.rx.core.orion.Graph.Heuristic;
import polly.rx.core.orion.Graph.LazyBuilder;
import polly.rx.core.orion.model.Quadrant;
import polly.rx.core.orion.model.Sector;
import polly.rx.core.orion.model.SectorType;
import polly.rx.core.orion.model.Wormhole;


public class PathPlanner {
    
    public static class EdgeData {
        private final boolean isWormhole;
        private final Wormhole wormhole;
        
        public EdgeData() {
            this(null);
        }
        
        public EdgeData( Wormhole wormhole) {
            this.isWormhole = wormhole != null;
            this.wormhole = wormhole;
        }
        
        public boolean isWormhole() {
            return this.isWormhole;
        }
        
        public Wormhole getWormhole() {
            return this.wormhole;
        }
    }
    
    
    
    private class UniverseBuilder implements LazyBuilder<Sector, EdgeData> {
        
        private final double COST_DIAGONAL = 1.0;
        private final double COST_NORMAL = 2.0;
        private final double COST_QUAD = 5.0;
        
        
        private final Set<Sector> done;
        
        
        public UniverseBuilder() {
            this.done = new HashSet<>();
        }
        
        

        @Override
        public void collectIncident(Graph<Sector, EdgeData> graph, Sector source) {
            if (this.done.add(source)) {
                // add wormhole edges
                final Collection<Wormhole> holes = holeProvider.getWormholes(
                        source, quadProvider);
                
                for (final Wormhole hole : holes) {
                    final Quadrant targetQuad = quadProvider.getQuadrant(
                            hole.getTarget());
                    
                    final EdgeData d = new EdgeData(hole);
                    this.addNeighbour(targetQuad, hole.getTarget().getX(), 
                            hole.getTarget().getY(), COST_QUAD, graph, source, d);
                }
                
                // add direct neighbours
                final int x = source.getX();
                final int y = source.getY();
                final Quadrant quad = quadProvider.getQuadrant(source);
                for (int i = -1; i < 2; ++i) {
                    for (int j = -1; j < 2; ++j) {
                        final EdgeData d = new EdgeData();
                        final boolean diagonal = Math.abs(i) == 1 && Math.abs(j) == 1;
                        final double costs = diagonal ? COST_DIAGONAL : COST_NORMAL;
                        this.addNeighbour(quad, x + i, y + j, costs, graph, source, d);
                    }
                }
            }
        }
        
        
        
        private void addNeighbour(Quadrant quad, int x, int y, double costs,
                Graph<Sector, EdgeData> graph, Sector source, EdgeData edgeData) {
            if (x < 0 || x > quad.getMaxX() || y < 0 || y > quad.getMaxY() || 
                    (x == source.getX() && y == source.getY())) {
                return;
            }
            final Sector neighbour = quad.getSector(x, y);
            if (neighbour.getType() != SectorType.NONE) {
                final Graph<Sector, EdgeData>.Node vSource = graph.getNode(source);
                final Graph<Sector, EdgeData>.Node vTarget = graph.getNode(neighbour, neighbour); 
                vSource.edgeTo(vTarget, costs, edgeData);
            }
        }
    }
    
    
    
    private class SectorHeuristic implements Heuristic<Sector> {
        
        @Override
        public double calculate(Sector v1, Sector v2) {
            if (v1.getQuadName().equals(v2.getQuadName())) {
                final double dx = v1.getX() - v2.getX();
                final double dy = v1.getY() - v2.getY();
                return Math.sqrt(dx * dx + dy * dy);
            }
            return Double.MAX_VALUE;
        }
    }
    
    
    private final QuadrantProvider quadProvider;
    private final WormholeProvider holeProvider;
    private final Graph<Sector, EdgeData> graph;
    private final Heuristic<Sector> heuristic;
    
    
    
    public PathPlanner(QuadrantProvider quadProvider, WormholeProvider holeProvider) {
        this.graph = new Graph<>();
        this.heuristic = new SectorHeuristic();
        this.quadProvider = quadProvider;
        this.holeProvider = holeProvider;
    }
    
    
    
    public Graph<Sector, EdgeData>.Path findShortestPath(Sector start, Sector target) {
        final LazyBuilder<Sector, EdgeData> builder = new UniverseBuilder();
        return this.graph.findShortestPath(start, target, builder, this.heuristic);
    }
}
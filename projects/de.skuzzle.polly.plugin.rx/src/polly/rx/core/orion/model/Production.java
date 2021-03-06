package polly.rx.core.orion.model;

import de.skuzzle.polly.tools.Equatable;
import polly.rx.entities.RxRessource;

public interface Production extends Equatable, Comparable<Production> {

    public abstract RxRessource getRess();

    public abstract double getRate();
}
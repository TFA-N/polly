package polly.rx.entities;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import polly.rx.MSG;

@Entity
@NamedQueries({
        @NamedQuery(name = FleetScanShip.BY_REVORIX_ID, query = "SELECT ship FROM FleetScanShip ship WHERE ship.rxId = ?1"),
        @NamedQuery(name = FleetScanShip.ALL_SHIPS, query = "SELECT ship FROM FleetScanShip ship"),
        @NamedQuery(name = FleetScanShip.BY_OWNER, query = "SELECT DISTINCT ship FROM FleetScanShip ship WHERE ship.owner = ?1"),
        @NamedQuery(name = FleetScanShip.SHIPS_BY_CLAN, query = "SELECT DISTINCT ship FROM FleetScanShip ship WHERE ship.ownerClan = ?1"),
        @NamedQuery(name = FleetScanShip.SHIPS_BY_LOCATION, query = "SELECT DISTINCT ship FROM FleetScanShip ship, IN(ship.history) h, IN(h.changes) c WHERE "
                + "ship.quadrant = ?1 OR " + "c LIKE ?2")

})
public class FleetScanShip {

    public final static String BY_REVORIX_ID = "BY_REVORIX_ID"; //$NON-NLS-1$
    public final static String ALL_SHIPS = "ALL_SHIPS"; //$NON-NLS-1$
    public final static String BY_OWNER = "BY_OWNER"; //$NON-NLS-1$
    public final static String SHIPS_BY_CLAN = "SHIPS_BY_CLAN"; //$NON-NLS-1$
    public final static String SHIPS_BY_LOCATION = "SHIPS_BY_LOCATION"; //$NON-NLS-1$

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private int id;

    private int rxId;

    private String name;

    private String nameWhenSpotted;

    private int techlevel;

    private String owner;

    private String ownerClan;

    private String quadrant;

    private int x;

    private int y;

    @OneToMany(cascade = CascadeType.ALL)
    private List<FleetScanHistoryEntry> history;

    @Transient
    private transient ShipType shipType;

    @Transient
    private transient int shipClass;
    
    @Transient
    private transient String simpleName;


    
    public FleetScanShip() {
        this(0, "", 0, "", "", "", 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }



    public FleetScanShip(int rxId, String name, int techlevel, String owner,
            String ownerClan, String quadrant, int x, int y) {
        super();
        this.rxId = rxId;
        this.name = name;
        this.nameWhenSpotted = name;
        this.techlevel = techlevel;
        this.owner = owner;
        this.ownerClan = ownerClan;
        this.history = new LinkedList<FleetScanHistoryEntry>();
        FleetScanHistoryEntry e = new FleetScanHistoryEntry();
        e.getChanges().add(MSG.bind(MSG.scanShipSpotFirstTime, quadrant, x, y));
        this.x = x;
        this.y = y;
        this.quadrant = quadrant;
        this.history.add(e);
        this.postLoad();
    }



    @PostLoad
    public void postLoad() {
        this.shipType = ShipHelper.getShipType(this.name);
        this.shipClass = ShipHelper.getShipClass(this.name);
        this.simpleName = ShipHelper.getSimpleName(this.name);
    }



    public int getId() {
        return this.id;
    }



    public ShipType getShipType() {
        if (this.shipType == null) {
            this.postLoad();
        }
        return this.shipType;
    }



    public int getShipClass() {
        return this.shipClass;
    }

    
    
    public String getSimpleName() {
        if (this.simpleName == null) {
            this.postLoad();
        }
        return this.simpleName;
    }


    public int getRxId() {
        return this.rxId;
    }



    public String getName() {
        return this.name;
    }



    public String getNameWhenSpotted() {
        return this.nameWhenSpotted;
    }



    public int getTechlevel() {
        return this.techlevel;
    }



    public String getOwner() {
        return this.owner;
    }



    public String getOwnerClan() {
        return this.ownerClan;
    }



    public String getQuadrant() {
        return this.quadrant;
    }



    public int getX() {
        return this.x;
    }



    public int getY() {
        return this.y;
    }



    public int getHistorySize() {
        return this.history.size();
    }



    public List<FleetScanHistoryEntry> getHistory() {
        return this.history;
    }



    public boolean changedOwner() {
        for (final FleetScanHistoryEntry entry : this.getHistory()) {
            for (final String change : entry.getChanges()) {
                // HACK: This is a hack because ship history is not stored atomically
                if (change.contains("Spotted with different owner") || change.contains(MSG.scanShipOwnerChangedIndicator)) { //$NON-NLS-1$
                    return true;
                }
            }
        }
        return false;
    }



    public void update(FleetScanShip other) {
        if (this.rxId != other.rxId) {
            throw new RuntimeException("cannot update ships with different rxId's"); //$NON-NLS-1$
        }
        FleetScanHistoryEntry historyEntry = new FleetScanHistoryEntry();
        historyEntry.getChanges().add(
                MSG.bind(MSG.scanShipSpotted, other.quadrant, other.x, other.y));

        if (!this.name.equals(other.name)) {
            historyEntry.getChanges().add(
                    MSG.bind(MSG.scanShipNameChanged, this.name, other.name));
            this.name = other.name;
        }

        if (!this.owner.equals(other.owner)) {
            historyEntry.getChanges().add(
                    MSG.bind(MSG.scanShipOwnerChanged, this.owner, other.owner));
            
            this.owner = other.owner;
        }

        if (!this.ownerClan.equals(other.ownerClan)) {
            historyEntry.getChanges().add(MSG.bind(
                    MSG.scanShipOwnerChangedClan, other.ownerClan, this.ownerClan));
            this.ownerClan = other.ownerClan;
        }
        if (!historyEntry.getChanges().isEmpty()) {
            this.history.add(historyEntry);
        }
    }
}
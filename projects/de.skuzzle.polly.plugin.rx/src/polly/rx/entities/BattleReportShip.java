package polly.rx.entities;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.Transient;


@Entity
public class BattleReportShip {
    
    @Id@GeneratedValue(strategy = GenerationType.TABLE)
    private int id;
    
    private int rxId;
    
    private String name;
    
    private String capi;
    
    private int attack;
    
    private int shields;
    
    private int pz;
    
    private int structure;
    
    private int minCrew;
    
    private int maxCrew;
    
    private int systems;
    
    private int capiXp;
    
    private int crewXp;
    
    private int awDamage;
    
    private int capiHp;
    
    private int hpDamage;
    
    private int shieldDamage;
    
    @Transient
    private transient int currentPz;
    
    private int pzDamage;
    
    private int structureDamage;
    
    private int systemsDamage;
    
    private int crewDamage;
    
    @Transient
    private transient BattleDrop[] repairCostOffset;

    @Transient
    private transient ShipType shipType;

    @Transient
    private transient int shipClass;
    
    @Transient
    private transient String simpleName;
    
    
    
    public BattleReportShip() {
        this(0, "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    

    public BattleReportShip(int rxId, String name, String capi, int attack,
        int shields, int pz, int structure, int minCrew, int maxCrew,
        int systems, int capiXp, int crewXp, int shieldDamage, int currentPz, int pzDamage,
        int structureDamage, int systemsDamage, int hp, int hpDamage, int awDamage, 
        int crewDamage) {
        
        super();
        this.rxId = rxId;
        this.name = name;
        this.capi = capi;
        this.attack = attack;
        this.shields = shields;
        this.currentPz = currentPz;
        this.pz = pz;
        this.structure = structure;
        this.minCrew = minCrew;
        this.maxCrew = maxCrew;
        this.systems = systems;
        this.capiXp = capiXp;
        this.crewXp = crewXp;
        this.shieldDamage = shieldDamage;
        this.pzDamage = pzDamage;
        this.structureDamage = structureDamage;
        this.systemsDamage = systemsDamage;
        this.capiHp = hp;
        this.hpDamage = hpDamage;
        this.awDamage = awDamage;
        this.crewDamage = crewDamage;
    }

    
    private final static double CRED_FACTOR_PZ = 0.063273;
    private final static double NRG_FACTOR_PZ = 0.060257;
    private final static double ORG_FACTOR_PZ = 0.099357;
    private final static double FE_FACTOR_PZ = 0.150401;
    private final static double LM_FACTOR_PZ = 0.084333;
    private final static double SM_FACTOR_PZ = 0.054408;
    //private final static double REPAIR_TIME_FACTOR_PZ = 1.0 / 18.0; // in pz / seconds
    
    
    
    @PostLoad
    void calcCostOffset() {
        this.repairCostOffset = new BattleDrop[7];
        this.repairCostOffset[0] = new BattleDrop(RxRessource.CR, (int)Math.round(CRED_FACTOR_PZ * this.pzDamage));
        this.repairCostOffset[1] = new BattleDrop(RxRessource.NRG, (int)Math.round(NRG_FACTOR_PZ * this.pzDamage));
        this.repairCostOffset[2] = new BattleDrop(RxRessource.ORG, (int)Math.round(ORG_FACTOR_PZ * this.pzDamage));
        this.repairCostOffset[3] = new BattleDrop(RxRessource.SYNT, 0);
        this.repairCostOffset[4] = new BattleDrop(RxRessource.FE, (int)Math.round(FE_FACTOR_PZ * this.pzDamage));
        this.repairCostOffset[5] = new BattleDrop(RxRessource.LM, (int)Math.round(LM_FACTOR_PZ * this.pzDamage));
        this.repairCostOffset[6] = new BattleDrop(RxRessource.SM, (int)Math.round(SM_FACTOR_PZ * this.pzDamage));
        this.shipType = ShipHelper.getShipType(this.name);
        this.shipClass = ShipHelper.getShipClass(this.name);
        this.simpleName = ShipHelper.getSimpleName(this.name);
    }
    
    
    
    public int getId() {
        return this.id;
    }
    
    
    
    public ShipType getShipType() {
        return this.shipType;
    }



    public int getShipClass() {
        return this.shipClass;
    }

    
    
    public String getSimpleName() {
        return this.simpleName;
    }
    
    
    
    public BattleDrop[] getRepairCostOffset() {
        return this.repairCostOffset;
    }
    
    
    
    public double getRepairTimeOffset(int dockLevel) {
        return this.calculateRepairTime(dockLevel, this.pzDamage);
    }
    
    
    public double getAbsoluteRepairTime(int dockLevel) {
        final int pzDamage = this.pz - this.currentPz;
        return this.calculateRepairTime(dockLevel, pzDamage);
    }

    
    private double calculateRepairTime(int dockLevel, int pzDamage) {
        final double pzPerSecond = 1.0/(1.0 - dockLevel / 200.0) * 200.0 / 60.0 / 60.0;
        return pzDamage / pzPerSecond;
    }
    
    
    public int getCurrentPz() {
        return this.currentPz;
    }
    
    
    
    public int getRxId() {
        return this.rxId;
    }

    
    
    public String getName() {
        return this.name;
    }

    
    
    public String getCapi() {
        return this.capi;
    }

    
    
    public int getAttack() {
        return this.attack;
    }

    
    
    public int getShields() {
        return this.shields;
    }

    
    
    public int getPz() {
        return this.pz;
    }

    
    
    public int getStructure() {
        return this.structure;
    }

    
    
    public int getMinCrew() {
        return this.minCrew;
    }

    
    
    public int getMaxCrew() {
        return this.maxCrew;
    }

    
    
    public int getSystems() {
        return this.systems;
    }
    
    
    
    public int getMaxWend() {
        return this.systems - this.shields - this.attack;
    }

    
    
    public int getCapiXp() {
        return this.capiXp;
    }

    
    
    public int getCrewXp() {
        return this.crewXp;
    }

    
    
    public int getAwDamage() {
        return this.awDamage;
    }

    
    
    public int getCapiHp() {
        return this.capiHp;
    }

    
    
    public int getHpDamage() {
        return this.hpDamage;
    }

    
    
    public int getShieldDamage() {
        return this.shieldDamage;
    }

    
    
    public int getPzDamage() {
        return this.pzDamage;
    }

    
    
    public int getStructureDamage() {
        return this.structureDamage;
    }

    
    
    public int getSystemsDamage() {
        return this.systemsDamage;
    }

    
    
    public int getCrewDamage() {
        return this.crewDamage;
    }
    
    
    
    public int calcMaxWend() {
        return this.systems - this.shields;
    }
    
    
    
    @Override
    public String toString() {
        return this.name;
    }



    public double calculateKw() {
        return Math.sqrt((this.pz + this.shields) * this.attack);
    }
}

package polly.rx;

import de.skuzzle.polly.sdk.resources.Constants;
import de.skuzzle.polly.sdk.resources.Resources;

public class MSG extends Constants {

    private final static String FAMILY = "polly.rx.Translation"; //$NON-NLS-1$

    // AddTrainCommand
    public static String addTrainHelp;
    public static String addTrainSig0Desc;
    public static String addTrainSig0User;
    public static String addTrainSig0Bill;
    public static String addTrainSig1Desc;
    public static String addTrainSig1User;
    public static String addTrainSig1Details;
    public static String addTrainSig2Desc;
    public static String addTrainSig2User;
    public static String addTrainSig2Bill;
    public static String addTrainSig2Weight;
    public static String addTrainSig3Desc;
    public static String addTrainSig3User;
    public static String addTrainSuccess;
    public static String addTrainFail;
    public static String addTrainRemind;

    // CLoseTrainCommand
    public static String closeTrainHelp;
    public static String closeTrainSig0Desc;
    public static String closeTrainSig0User;
    public static String closeTrainSig1Desc;
    public static String closeTrainSig1Id;
    public static String closeTrainSuccessAll;
    public static String closeTrainSuccessSingle;

    // CrackerCommand
    public static String crackerHelp;
    public static String crackerSig0Desc;
    public static String crackerSig1Desc;
    public static String crackerSig1User;
    public static String crackerUnknownUser;
    public static String crackerSuccess;

    // DeliverTrainCommand
    public static String deliverHelp;
    public static String deliverSig0Desc;
    public static String deliverSig0User;
    public static String deliverSig1Desc;
    public static String deliverSig1User;
    public static String deliverSig1Receiver;

    // IPCommand
    public static String ipHelp;
    public static String ipSig0Desc;
    public static String ipSig0Venad;
    public static String ipInvalidAnswer;
    public static String ipNoIp;
    public static String ipResultWithClan;
    public static String ipResult;

    // MyTrainsCommand
    public static String myTrainsHelp;
    public static String myTrainsSig0Desc;
    public static String myTrainsSig0Trainer;
    public static String myTrainsSig1Desc;
    public static String myTrainsSig1Trainer;
    public static String myTrainsSig1Details;

    // MyVenadCommand
    public static String myVenadHelp;
    public static String myVenadSig0Desc;
    public static String myVenadSig0Name;
    public static String myVenadSuccess;

    // RankCommand
    public static String rankHelp;
    public static String rankSig0Desc;
    public static String rankSig0Name;
    public static String rankNoVenad;
    public static String rankSuccess;

    // RessCommand
    public static String ressHelp;
    public static String ressSigDesc;
    public static String ressSigExpression;

    // VenadCommand
    public static String venadHelp;
    public static String venadSig0Desc;
    public static String venadSig0User;
    public static String venadUnknownUser;
    public static String venadSuccess;

    // AZEntryManager
    public static String azEntryCantDeleteOther;

    // FleetDbManager
    public static String fleetDbReportExists;
    public static String fleetDbDeletedScanWithShip;

    // ScoreboardManager
    public static String scoreboardAvgPoints;
    public static String scoreboardAvgRank;
    public static String scoreboardPoints;
    public static String scoreboardRank;
    public static String scoreboardDatePoints;
    public static String scoreboardDateRank;

    // TrainBillV2
    public static String billNoOpen;
    public static String billOpen;

 // TrainManagerV2
    public static String trainManagerInvalidTrainId;
    public static String trainManagerInvalidTrainerId;
    
    
 // BattleTactic
    public static String tacticNormal;
    public static String tacticRaubzug;
    public static String tacticMachtDemo;
    public static String tacticTPT;
    public static String tacticSchnitt;
    public static String tacticNahkampf;
    public static String tacticSystem;
    public static String tacticAusweich;
    public static String tacticTT;
    public static String tacticZange;
    public static String tacticAbgesichert;
    public static String tacticSondierung;
    public static String tacticFern;
    public static String tacticMultivektor;
    public static String tacticKonzentriert;
    public static String tacticVertreiben;
    public static String tacticKralle;
    public static String tacticSichel;
    public static String tacticHitAndRun;
    public static String tacticHinterhalt;
    public static String tacticSturm;
    public static String tacticDauerbeschuss;
    public static String tacticAlien;

 // FleetScanShip
    public static String scanShipSpotFirstTime;
    public static String scanShipSpotted;
    public static String scanShipNameChanged;
    public static String scanShipOwnerChanged;
    public static String scanShipOwnerChangedIndicator;
    public static String scanShipOwnerChangedClan;

 // TrainEntity
    public static String trainEntityMisformatted;
    public static String trainEntityFormatWithFactor;
    public static String trainEntityFormatWithoutFactor;
    
 // TrainType
    public static String trainTypeIntelligence;
    public static String trainTypeBody;
    public static String trainTypeCommando;
    public static String trainTypeModule;
    public static String trainTypeCrew;
    public static String trainTypeTech;
    public static String trainTypePayment;
    public static String trainTypeExtendedIntelligence;
    public static String trainTypeExtendedBody;
    public static String trainTypeExtendedCommand;
    public static String trainTypeExtendedModule;
    public static String trainTypeExtendedCrew;
    public static String trainTypeExtendedTech;
    public static String trainTypeIntensiveIntelligence;
    public static String trainTypeIntensiveBody;
    public static String trainTypeIntensiveCommand;
    public static String trainTypeIntensiveModule;
    public static String trainTypeIntensiveCrew;
    public static String trainTypeIntensiveTech;

    static {
        Resources.init(FAMILY, MSG.class);
    }
}

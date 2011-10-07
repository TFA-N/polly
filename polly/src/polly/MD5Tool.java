package polly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.skuzzle.polly.sdk.Configuration;

import polly.data.User;

public class MD5Tool {

    public static void main(String[] args) throws IOException, ConfigurationFileException {
        if (args.length != 1) {
            System.out.println("Bitte nur einen Parameter angeben.");
            return;
        }
        
        String pw = args[0];
        User tmp = new User("", pw, 0);        
        System.out.println(tmp.getHashedPassword());
        System.out.println(
                "Soll das Passwort in der Konfigurationsdatei gespeichert werden (y/n)?");
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String line = r.readLine();
        if (line.equals("y")) {
            PollyConfiguration cfg = new PollyConfiguration("./cfg/polly.cfg");
            cfg.setProperty(Configuration.ADMIN_PASSWORD_HASH, tmp.getHashedPassword());
            cfg.store();
            System.out.println("Passwort wurde gespeichert.");
        } else {
            System.out.println("Passwort wurde nicht gespeichert.");
        }
    }
}
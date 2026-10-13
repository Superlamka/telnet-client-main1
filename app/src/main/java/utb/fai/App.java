package utb.fai;

public class App {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(
                "Použití: java main.java.utb.fai.App <IP adresa> <port>"
            );
            return;
        }

        String serverIp = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]);

            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println(
                "Chyba: port musí být celé číslo v rozsahu 1–65535."
            );
            return;
        }

        TelnetClient telnetClient = new TelnetClient(serverIp, port);
        telnetClient.run();
    }
}
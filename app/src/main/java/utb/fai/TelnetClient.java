package utb.fai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class TelnetClient {

    private final String serverIp;
    private final int port;

    public TelnetClient(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public void run() {
        try (Socket socket = new Socket(serverIp, port)) {
            System.out.println(
                "Připojeno k " + serverIp + ":" + port
            );
            System.out.println(
                "Pro ukončení spojení zadejte /QUIT."
            );

            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            AtomicBoolean running = new AtomicBoolean(true);

            Thread receiver = new Thread(() -> {
                byte[] buffer = new byte[4096];

                try {
                    while (running.get() && !socket.isClosed()) {
                        int available = input.available();

                        if (available > 0) {
                            int count = input.read(
                                buffer, 0,
                                Math.min(available, buffer.length)
                            );

                            if (count == -1) {
                                break;
                            }

                            // Vypisujeme přímo přijaté bajty.
                            // Nedochází k převodu znakové sady.
                            synchronized (System.out) {
                                System.out.write(buffer, 0, count);
                                System.out.flush();
                            }
                        } else {
                            // Omezení zátěže CPU při čekání na data.
                            Thread.sleep(20);
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println(
                            "Chyba při příjmu dat: " + e.getMessage()
                        );
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.set(false);
                }
            }, "telnet-receiver");

            Thread sender = new Thread(() -> {
                BufferedReader console = new BufferedReader(
                    new InputStreamReader(
                        System.in, StandardCharsets.UTF_8
                    )
                );

                try {
                    String line;

                    while (running.get()
                            && (line = console.readLine()) != null) {

                        if (line.equals("/QUIT")) {
                            running.set(false);
                            break;
                        }

                        byte[] data = (
                            line + System.lineSeparator()
                        ).getBytes(StandardCharsets.UTF_8);

                        synchronized (output) {
                            output.write(data);
                            output.flush();
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println(
                            "Chyba při odesílání dat: " + e.getMessage()
                        );
                    }
                } finally {
                    running.set(false);

                    try {
                        socket.shutdownOutput();
                    } catch (IOException ignored) {
                        // Soket již může být uzavřen.
                    }
                }
            }, "telnet-sender");

            receiver.start();
            sender.start();

            // Po ukončení odesílání ukončíme také příjem.
            sender.join();
            running.set(false);
            socket.close();
            receiver.interrupt();
            receiver.join();

            System.out.println("\nSpojení ukončeno.");

        } catch (UnknownHostException e) {
            System.err.println(
                "Neznámá IP adresa nebo název hostitele: " + serverIp
            );
        } catch (IOException e) {
            System.err.println(
                "Chyba spojení: " + e.getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Aplikace byla přerušena.");
        }
    }
}

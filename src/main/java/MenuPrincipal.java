import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;

public class MenuPrincipal {
    public static final String RESET = "\u001B[0m";
    public static final String VERDE = "\u001B[32m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String AZUL = "\u001B[34m";
    public static final String CIAN = "\u001B[36m";

    public static void main(String[] args) {
        String direccionEnlace = "mongodb://localhost:27017";

        try (MongoClient clienteMongo = MongoClients.create(direccionEnlace);
             Scanner teclado = new Scanner(System.in)) {
            
            MongoDatabase baseDeDatos = clienteMongo.getDatabase("TallerMecanico_DB");
            int opcionSeleccionada;

            do {
                try {
                    if (System.getProperty("os.name").contains("Windows")) {
                        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                    } else {
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                    }
                } catch (IOException | InterruptedException errorLimpieza) {
                    for (int i = 0; i < 50; i++) System.out.println();
                }

                System.out.println(AZUL + "==========================================" + RESET);
                System.out.println(CIAN + "       SISTEMA DE GESTION DE TALLER       " + RESET);
                System.out.println(AZUL + "==========================================" + RESET);
                System.out.println("1. VENDEDOR: Crear Cita (Entrada)");
                System.out.println("2. MECANICO: Atender Citas Pendientes");
                System.out.println("3. ADMIN: Registro de Clientes Frecuentes");
                System.out.println("4. CATALOGO: Consultar Servicios");
                System.out.println("5. SALIR");
                System.out.print("\nSeleccione una opcion: ");
                
                if (!teclado.hasNextInt()) {
                    teclado.next(); 
                    teclado.nextLine();
                    opcionSeleccionada = 0; 
                    continue; 
                }

                opcionSeleccionada = teclado.nextInt();
                teclado.nextLine(); 

                switch (opcionSeleccionada) {
                    case 1 -> {
                        System.out.println(CIAN + "\n--- REGISTRO DE NUEVA CITA ---" + RESET);
                        System.out.print("Nombre del Dueño: ");
                        String nombreCliente = teclado.nextLine();
                        System.out.print("Diagnostico de falla: ");
                        String diagnosticoFalla = teclado.nextLine();
                        System.out.print("Fecha de la cita (AAAA/MM/DD): ");
                        String fechaCita = teclado.nextLine();

                        baseDeDatos.getCollection("citas").insertOne(new Document("cliente", nombreCliente)
                                            .append("falla", diagnosticoFalla)
                                            .append("fecha_cita", fechaCita)
                                            .append("estado", "Pendiente"));
                        
                        System.out.println(VERDE + "\n>> Cita guardada correctamente." + RESET);
                        teclado.nextLine();
                    }
                    case 2 -> {
                        System.out.println(CIAN + "\n--- CITAS PENDIENTES EN SISTEMA ---" + RESET);
                        FindIterable<Document> citasPendientes = baseDeDatos.getCollection("citas").find(new Document("estado", "Pendiente"));
                        
                        boolean hayCitas = false;
                        for (Document cita : citasPendientes) {
                            System.out.println("- [" + AMARILLO + cita.getString("cliente") + RESET + "] Falla: " + cita.getString("falla") + " (Cita: " + cita.getString("fecha_cita") + ")");
                            hayCitas = true;
                        }

                        if (!hayCitas) {
                            System.out.println("No hay citas pendientes.");
                        } else {
                            System.out.print("\nEscriba el NOMBRE del cliente a atender: ");
                            String clienteAAtender = teclado.nextLine();
                            Document encontrada = baseDeDatos.getCollection("citas").find(new Document("cliente", clienteAAtender).append("estado", "Pendiente")).first();

                            if (encontrada != null) {
                                String fechaCitaOriginal = encontrada.getString("fecha_cita");
                                String fechaFin;
                                while (true) {
                                    System.out.println("Fecha registrada: " + fechaCitaOriginal);
                                    System.out.print("Fecha de entrega (AAAA/MM/DD): ");
                                    fechaFin = teclado.nextLine();
                                    if (fechaFin.compareTo(fechaCitaOriginal) >= 0) break;
                                    System.out.println("   [!] Verifique la fecha."); 
                                }
                                System.out.print("¿Que reparacion se le hizo finalmente?: ");
                                String reparacion = teclado.nextLine();

                                baseDeDatos.getCollection("reportes").insertOne(new Document("cliente", clienteAAtender).append("trabajo", reparacion).append("fecha", fechaFin));
                                baseDeDatos.getCollection("citas").updateOne(new Document("cliente", clienteAAtender).append("estado", "Pendiente"), new Document("$set", new Document("estado", "Terminado")));
                                System.out.println(VERDE + "\n>> ¡Proceso completado!" + RESET);
                            }
                        }
                        teclado.nextLine();
                    }
                    case 3 -> {
                        System.out.println(AMARILLO + "\n--- CLIENTES FRECUENTES ---" + RESET);
                        var procesoAgregacion = Arrays.asList(Aggregates.group("$cliente", Accumulators.sum("total", 1)), Aggregates.match(new Document("total", new Document("$gte", 2))));
                        var resultados = baseDeDatos.getCollection("reportes").aggregate(procesoAgregacion);
                        for (Document res : resultados) {
                            System.out.println(".3 " + VERDE + res.getString("_id") + RESET + " | Visitas: " + res.getInteger("total"));
                        }
                        teclado.nextLine();
                    }
                    case 4 -> {
                        System.out.println(AZUL + "\n--- CATALOGO ---" + RESET);
                        System.out.printf("%-25s %-10s\n", "SERVICIO", "PRECIO");
                        System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Cambio de Aceite", "$1,250");
                        System.out.printf("%-25s " + VERDE + "%-10s\n" + RESET, "Afinacion Mayor", "$2,800");
                        teclado.nextLine();
                    }
                }
            } while (opcionSeleccionada != 5);
        } catch (Exception error) {
        }
    }
}
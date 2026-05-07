import java.util.Scanner;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import java.util.Arrays;

public class MenuPrincipal {
    public static void main(String[] args) {
        String uri = "mongodb://localhost:27017";

        try (MongoClient mongoClient = MongoClients.create(uri);
             Scanner teclado = new Scanner(System.in)) {
            
            MongoDatabase db = mongoClient.getDatabase("TallerMecanico_DB");
            int opcion;

            do {
                try {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } catch (Exception e) {
                    for (int i = 0; i < 50; i++) System.out.println();
                }

                System.out.println("\n_________________________________________");
                System.out.println("       SISTEMA DE GESTION DE TALLER       ");
                System.out.println("__________________________________________");
                System.out.println("1. VENDEDOR: Crear Cita (Entrada)");
                System.out.println("2. MECANICO: Atender Citas Pendientes");
                System.out.println("3. ADMIN: Ver Clientes Frecuentes (Auto)");
                System.out.println("4. CATALOGO: Consultar Servicios");
                System.out.println("5. SALIR");
                System.out.print("Seleccione una opcion: ");
                
                if (!teclado.hasNextInt()) {
                    System.out.println("\n  [!] ERROR: Ingrese solo números.");
                    teclado.next(); teclado.nextLine();
                    opcion = 0; continue;
                }

                opcion = teclado.nextInt();
                teclado.nextLine(); 

                switch (opcion) {
                    case 1 -> {
                        System.out.println("\n--- REGISTRO DE NUEVA CITA ---");
                        System.out.print("Nombre del Dueño: ");
                        String cliente = teclado.nextLine();
                        System.out.print("Diagnostico de falla: ");
                        String falla = teclado.nextLine();
                        System.out.print("Fecha de la cita (AAAA/MM/DD): ");
                        String fechaCita = teclado.nextLine();

                        db.getCollection("citas").insertOne(new Document("cliente", cliente)
                                            .append("falla", falla)
                                            .append("fecha_cita", fechaCita)
                                            .append("estado", "Pendiente"));
                        
                        System.out.println("\n>> Cita guardada. Presione ENTER...");
                        teclado.nextLine();
                    }
                    case 2 -> {
                        System.out.println("\n--- CITAS PENDIENTES ---");
                        FindIterable<Document> pendientes = db.getCollection("citas").find(new Document("estado", "Pendiente"));
                        
                        boolean hayDatos = false;
                        for (Document d : pendientes) {
                            System.out.println("- [" + d.getString("cliente") + "] Cita: " + d.getString("fecha_cita"));
                            hayDatos = true;
                        }

                        if (!hayDatos) {
                            System.out.println("No hay pendientes.");
                        } else {
                            System.out.print("\nNombre del cliente a atender: ");
                            String clienteA = teclado.nextLine();

                            Document citaExistente = db.getCollection("citas").find(
                                new Document("cliente", clienteA).append("estado", "Pendiente")
                            ).first();

                            if (citaExistente == null) {
                                System.out.println("\n  [!] ERROR: Cliente no encontrado.");
                            } else {
                                String fechaCitaOriginal = citaExistente.getString("fecha_cita");
                                String fechaFin;

                                while (true) {
                                    System.out.print("Fecha entrega (Mínimo " + fechaCitaOriginal + "): ");
                                    fechaFin = teclado.nextLine();
                                    if (fechaFin.compareTo(fechaCitaOriginal) >= 0) break;
                                    System.out.println("  !!! Fecha inválida. Reintente.");
                                }

                                System.out.print("¿Reparacion realizada?: ");
                                String reparacion = teclado.nextLine();

                                db.getCollection("reportes").insertOne(new Document("cliente", clienteA)
                                                            .append("trabajo_realizado", reparacion)
                                                            .append("fecha_finalizacion", fechaFin));

                                db.getCollection("citas").updateOne(
                                    new Document("cliente", clienteA).append("estado", "Pendiente"), 
                                    new Document("$set", new Document("estado", "Terminado"))
                                );
                                System.out.println("\n>> ¡Listo!");
                            }
                        }
                        teclado.nextLine();
                    }
                    case 3 -> {
                        System.out.println("\n--- LISTA DE CLIENTES FRECUENTES ---");
                        System.out.println("(Clientes con 2 o más servicios realizados)\n");

                        var pipeline = Arrays.asList(
                            Aggregates.group("$cliente", Accumulators.sum("visitas", 1)),
                            Aggregates.match(new Document("visitas", new Document("$gte", 2)))
                        );

                        var resultados = db.getCollection("reportes").aggregate(pipeline);
                        
                        boolean hayFrecuentes = false;
                        for (Document res : resultados) {
                            System.out.println("⭐ Cliente: " + res.getString("_id") + " | Total Visitas: " + res.getInteger("visitas"));
                            hayFrecuentes = true;
                        }

                        if (!hayFrecuentes) {
                            System.out.println("Aún no hay clientes con la constancia suficiente.");
                        }

                        System.out.println("\nPresione ENTER para continuar...");
                        teclado.nextLine();
                    }
                    case 4 -> {
                        System.out.println("\n--- CATALOGO ---");
                        System.out.println("- Cambio de Aceite\n- Revision de Frenos\n- Ajuste de Motor\n- Suspension");
                        System.out.println("\nPresione ENTER para volver...");
                        teclado.nextLine();
                    }
                    case 5 -> System.out.println("Saliendo del sistema...");
                }
            } while (opcion != 5);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
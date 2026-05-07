import java.util.Scanner;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MenuPrincipal {
    public static void main(String[] args) {
        String uri = "mongodb://localhost:27017";

        try (MongoClient mongoClient = MongoClients.create(uri);
             Scanner teclado = new Scanner(System.in)) {
            
            MongoDatabase db = mongoClient.getDatabase("TallerMecanico_DB");
            int opcion;

            do {
                System.out.println("\n_________________________________________");
                System.out.println("       SISTEMA DE GESTION DE TALLER       ");
                System.out.println("__________________________________________");
                System.out.println("1. VENDEDOR: Crear Ficha Tecnica (Cita)");
                System.out.println("2. MECANICO: Registrar Reparacion");
                System.out.println("3. ADMIN: Registro de Clientes Frecuentes");
                System.out.println("4. CATALOGO: Consultar Servicios");
                System.out.println("5. SALIR");
                System.out.print("Seleccione una opcion: ");
                
                opcion = teclado.nextInt();
                teclado.nextLine(); 

                switch (opcion) {
                    case 1 -> {
                        System.out.print("Nombre del Dueño: ");
                        String cliente = teclado.nextLine();
                        System.out.print("Diagnostico de falla: ");
                        String falla = teclado.nextLine();
                        db.getCollection("citas").insertOne(new Document("cliente", cliente).append("falla", falla));
                        System.out.println(">> Ficha guardada.");
                    }
                    case 2 -> {
                        System.out.print("Servicio realizado: ");
                        String servicio = teclado.nextLine();
                        db.getCollection("reportes").insertOne(new Document("trabajo", servicio).append("fecha", new java.util.Date()));
                        System.out.println(">> Reporte generado.");
                    }
                    case 3 -> {
                        System.out.print("Nombre del Cliente: ");
                        String nombreC = teclado.nextLine();
                        db.getCollection("clientes").insertOne(new Document("nombre", nombreC));
                        System.out.println(">> Cliente registrado.");
                    }
                    case 4 -> {
                        System.out.println("\n--- SERVICIOS: Aceite, Frenos, Motor ---");
                    }
                    case 5 -> System.out.println("Cerrando...");
                    default -> System.out.println("Opcion no valida.");
                }
            } while (opcion != 5);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
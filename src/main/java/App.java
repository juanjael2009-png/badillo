
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class App {

    public static void main(String[] args) {
        // Conexión local predeterminada
        String uri = "mongodb://localhost:27017";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            // Conectamos a la base de datos 'admin' para probar
            MongoDatabase database = mongoClient.getDatabase("admin");

            // Enviamos un "ping" para confirmar que el servidor responde
            database.runCommand(new Document("ping", 1));

            System.out.println("\n======================================");
            System.out.println("   ¡CONEXIÓN EXITOSA A MONGODB!");
            System.out.println("   Estado: En línea");
            System.out.println("======================================\n");

        } catch (Exception e) {
            System.err.println("\n[ERROR]: No se pudo conectar.");
            System.err.println("Asegúrate de que el servidor MongoDB esté iniciado.");
            System.err.println("Detalle: " + e.getMessage());
        }
    }
}

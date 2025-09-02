package fr.hshc.athena;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AthenaSqlExecutorTest {

    @Test
    void testSqlParsing() throws IOException {
        // Créer un fichier SQL de test
        String sqlContent = 
            "-- Commentaire de ligne\n" +
            "SELECT COUNT(*) FROM table1;\n" +
            "\n" +
            "/* Commentaire de bloc\n" +
            "   sur plusieurs lignes */\n" +
            "SELECT \n" +
            "    column1,\n" +
            "    column2\n" +
            "FROM table2 \n" +
            "WHERE condition = 'value';\n" +
            "\n" +
            "-- Requête vide\n" +
            "\n" +
            "INSERT INTO table3 VALUES (1, 'test');\n";
        
        Path tempFile = Files.createTempFile("test", ".sql");
        Files.write(tempFile, sqlContent.getBytes());
        
        try {
            List<String> queries = parseSqlQueries(Files.readString(tempFile));
            String regex = "(\n|\r|\t| )+";
            assertEquals(3, queries.size(), "Devrait avoir 3 requêtes valides");
            assertTrue(queries.get(0).replaceAll(regex, " ").contains("SELECT COUNT(*) FROM table1"));
            assertTrue(queries.get(1).replaceAll(regex, " ").contains("SELECT column1, column2"));
            assertTrue(queries.get(2).replaceAll(regex, " ").contains("INSERT INTO table3"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    void testRemoveSqlComments() {
        String sqlWithComments = 
            "-- Commentaire de ligne\n" +
            "SELECT * FROM table1;\n" +
            "\n" +
            "/* Commentaire de bloc\n" +
            "   sur plusieurs lignes */\n" +
            "SELECT column1, column2 FROM table2;\n";
        
        String result = removeSqlComments(sqlWithComments);
        
        assertFalse(result.contains("-- Commentaire de ligne"));
        assertFalse(result.contains("/* Commentaire de bloc"));
        assertTrue(result.contains("SELECT * FROM table1"));
        assertTrue(result.contains("SELECT column1, column2 FROM table2"));
    }
    
    @Test
    void testAuthModeParsing() {
        assertEquals(AuthMode.USER_PASSWORD, AuthMode.fromString("user_password"));
        assertEquals(AuthMode.USER_PASSWORD, AuthMode.fromString("USER_PASSWORD"));
        assertEquals(AuthMode.AWS_PROFILE, AuthMode.fromString("aws_profile"));
        assertEquals(AuthMode.AWS_PROFILE, AuthMode.fromString("AWS_PROFILE"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            AuthMode.fromString("invalid_mode");
        });
    }
    
    // Méthodes de test statiques pour accéder aux méthodes privées
    public static List<String> parseSqlQueries(String content) {
        return AthenaSqlExecutor.parseSqlQueries(content);
    }
    
    public static String removeSqlComments(String content) {
        return AthenaSqlExecutor.removeSqlComments(content);
    }
    
    public enum AuthMode {
        USER_PASSWORD,
        AWS_PROFILE;
        
        public static AuthMode fromString(String mode) {
            if ("user_password".equalsIgnoreCase(mode)) {
                return USER_PASSWORD;
            } else if ("aws_profile".equalsIgnoreCase(mode)) {
                return AWS_PROFILE;
            } else {
                throw new IllegalArgumentException("Mode d'authentification non supporté: " + mode);
            }
        }
    }
}

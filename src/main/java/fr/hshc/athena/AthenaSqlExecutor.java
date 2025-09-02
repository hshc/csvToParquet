package fr.hshc.athena;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.TomlParseResult;

public class AthenaSqlExecutor {
    static void enableSSLDebugUsingSystemProperties() {
        System.setProperty("javax.net.debug", "ssl");
    }
    private static final Logger logger = LoggerFactory.getLogger(AthenaSqlExecutor.class);
    
    public static void main(String[] args) {
        enableSSLDebugUsingSystemProperties();
        try {
            validateArguments(args);
            
            String sqlFilePath = args[0];
            String configFilePath = args[1];
            
            // Validation des fichiers d'entrée
            validateInputFiles(sqlFilePath, configFilePath);
            
            // Chargement et validation de la configuration
            AthenaConfig config = loadConfiguration(configFilePath);
            
            // Application des configurations SSL et Logging
            applyConfigurations(config);
            
            // Exécution des requêtes
            executeQueries(sqlFilePath, config);
            
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation: {}", e.getMessage());
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            logger.error("Erreur fatale lors de l'exécution: {}", e.getMessage(), e);
            System.exit(2);
        }
    }
    
    private static void applyConfigurations(AthenaConfig config) {
        // Application de la configuration SSL
        config.getSslConfig().applySslSettings();
        
        // Application de la configuration Logging
        config.getLoggingConfig().applyLoggingSettings();
        
        logger.info("Configurations appliquées avec succès");
    }
    
    private static void validateArguments(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Nombre d'arguments incorrect");
        }
    }
    
    private static void validateInputFiles(String sqlFilePath, String configFilePath) {
        Path sqlPath = Paths.get(sqlFilePath);
        Path configPath = Paths.get(configFilePath);
        
        if (!Files.exists(sqlPath)) {
            throw new IllegalArgumentException("Fichier SQL introuvable: " + sqlFilePath);
        }
        if (!Files.exists(configPath)) {
            throw new IllegalArgumentException("Fichier de configuration introuvable: " + configFilePath);
        }
        if (!Files.isReadable(sqlPath)) {
            throw new IllegalArgumentException("Fichier SQL non lisible: " + sqlFilePath);
        }
        if (!Files.isReadable(configPath)) {
            throw new IllegalArgumentException("Fichier de configuration non lisible: " + configFilePath);
        }
    }
    
    private static AthenaConfig loadConfiguration(String configFilePath) throws Exception {
        logger.info("Chargement de la configuration depuis: {}", configFilePath);
        
        TomlParseResult config = org.tomlj.Toml.parse(Paths.get(configFilePath));
        
        if (config.hasErrors()) {
            throw new IllegalArgumentException("Erreurs dans le fichier de configuration: " + config.errors());
        }
        
        return new AthenaConfig(config);
    }
    
    private static void executeQueries(String sqlFilePath, AthenaConfig config) {
        logger.info("Connexion à Athena avec la configuration: {}", config.getConnectionInfo());
        
        try (Connection conn = createConnection(config);
             Statement stmt = conn.createStatement()) {
            
            List<String> queries = readQueriesFromFile(sqlFilePath);
            logger.info("{} requêtes trouvées dans le fichier", queries.size());
            
            for (int i = 0; i < queries.size(); i++) {
                String query = queries.get(i).trim();
                if (!query.isEmpty()) {
                    logger.info("Exécution de la requête {}/{}: {}", i + 1, queries.size(), 
                               query.length() > 100 ? query.substring(0, 100) + "..." : query);
                    executeQuery(stmt, query, i + 1);
                }
            }
            
            logger.info("Toutes les requêtes ont été exécutées avec succès");
            
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de l'exécution: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'exécution des requêtes", e);
        } catch (Exception e) {
            logger.error("Erreur inattendue: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue", e);
        }
    }
    
    private static Connection createConnection(AthenaConfig config) throws SQLException {
        Properties connectionProps = new Properties();
        StringBuilder url = new StringBuilder();
        
        url.append("jdbc:athena://Region=").append(config.getRegion())
           .append(";OutputLocation=").append(config.getOutputLocation())
           .append(";Workgroup=").append(config.getWorkgroup())
           .append(";Database=").append(config.getDatabase());
        //    .append(";skip_metadata=true");
        
        if (config.getAuthMode() == AuthMode.USER_PASSWORD) {
            connectionProps.put("user", config.getUser());
            connectionProps.put("password", config.getPassword());
        } else if (config.getAuthMode() == AuthMode.AWS_PROFILE) {
            url.append(";CredentialsProvider=ProfileCredentials")
               .append(";ProfileName=").append(config.getProfile());
        }
        
        // Configuration SSL basée sur la configuration
        // if (!config.getSslConfig().isEnabled()) {
        //     logger.warn("SSL désactivé - connexion non sécurisée");
        //     url.append(";UseSSL=false");
        // } else {
        //     url.append(";UseSSL=true");
        //     if (config.getSslConfig().isDisableCertChecking()) {
        //         url.append(";VerifyServerCertificate=false");
        //         logger.warn("Vérification des certificats SSL désactivée");
        //     }
        // }
        
        logger.debug("URL de connexion: {}", url.toString());        // Class.forName("com.simba.athena.jdbc.Driver");
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        System.setProperty("javax.net.debug", "true");
        System.out.println(System.getProperty("javax.net.ssl.trustStore"));
        logger.info("Tentative de connexion à Athena avec SSL: {}", !config.getSslConfig().disableCertChecking);
           
        return DriverManager.getConnection(url.toString(), connectionProps);
    }
    
    private static void executeQuery(Statement stmt, String query, int queryNumber) {
        try {
            boolean hasResults = stmt.execute(query);
            if (hasResults) {
                logger.info("Requête {} exécutée avec succès (avec résultats)", queryNumber);
            } else {
                int updateCount = stmt.getUpdateCount();
                logger.info("Requête {} exécutée avec succès ({} lignes affectées)", queryNumber, updateCount);
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de l'exécution de la requête {}: {}", queryNumber, e.getMessage());
            throw new RuntimeException("Échec de l'exécution de la requête " + queryNumber, e);
        }
    }
    
    private static List<String> readQueriesFromFile(String filePath) throws IOException {
        logger.debug("Lecture du fichier SQL: {}", filePath);
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            String content = sb.toString();
            
            // Amélioration du parsing SQL - gère les commentaires et chaînes
            return parseSqlQueries(content);
        }
    }
    
    public static List<String> parseSqlQueries(String content) {
        // Suppression des commentaires SQL
        content = removeSqlComments(content);
        
        // Découpage par point-virgule en évitant les chaînes
        return Arrays.stream(content.split(";"))
                .map(String::trim)
                .filter(query -> !query.isEmpty())
                .collect(Collectors.toList());
    }
    
    public static String removeSqlComments(String content) {
        // Suppression des commentaires -- (ligne)
        content = content.replaceAll("(?m)^--.*$", "");
        
        // Suppression des commentaires /* */ (bloc)
        content = content.replaceAll("/\\*(.|\n|\r|\n\r)*?\\*/", "");
        
        return content;
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar athena-sql-executor.jar <sql-file> <config-file.toml>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  sql-file        : Chemin vers le fichier SQL contenant les requêtes");
        System.out.println("  config-file.toml: Chemin vers le fichier de configuration TOML");
        System.out.println();
        System.out.println("Exemple de configuration TOML:");
        System.out.println("region = \"eu-west-3\"");
        System.out.println("output_location = \"s3://bucket/query-results\"");
        System.out.println("workgroup = \"primary\"");
        System.out.println("database = \"default\"");
        System.out.println();
        System.out.println("[auth]");
        System.out.println("mode = \"aws_profile\"  # ou \"user_password\"");
        System.out.println("profile = \"your-profile\"");
        System.out.println();
        System.out.println("[ssl]");
        System.out.println("# Désactiver la vérification des certificats (DÉCONSEILLÉ)");
        System.out.println("disable_cert_checking = false");
        System.out.println();
        System.out.println("[logging]");
        System.out.println("level = \"INFO\"  # DEBUG, INFO, WARN, ERROR");
        System.out.println("file_logging = true");
        System.out.println("log_file = \"logs/athena-executor.log\"");
    }
    
    // Classes internes pour une meilleure organisation
    public static class AthenaConfig {
        private final String region;
        private final String outputLocation;
        private final String workgroup;
        private final String database;
        private final AuthMode authMode;
        private final String user;
        private final String password;
        private final String profile;
        
        // Configuration SSL
        private final SslConfig sslConfig;
        
        // Configuration Logging
        private final LoggingConfig loggingConfig;
        
        public AthenaConfig(TomlParseResult config) {
            this.region = getRequiredString(config, "region");
            this.outputLocation = getRequiredString(config, "output_location");
            this.workgroup = getRequiredString(config, "workgroup");
            this.database = getRequiredString(config, "database");
            
            String authModeStr = getRequiredString(config, "auth.mode");
            this.authMode = AuthMode.fromString(authModeStr);
            
            if (this.authMode == AuthMode.USER_PASSWORD) {
                this.user = getRequiredString(config, "auth.user");
                this.password = getRequiredString(config, "auth.password");
                this.profile = null;
            } else if (this.authMode == AuthMode.AWS_PROFILE) {
                this.profile = getRequiredString(config, "auth.profile");
                this.user = null;
                this.password = null;
            } else {
                throw new IllegalArgumentException("Mode d'authentification non supporté: " + authModeStr);
            }
            
            // Chargement de la configuration SSL
            this.sslConfig = new SslConfig(config);
            
            // Chargement de la configuration Logging
            this.loggingConfig = new LoggingConfig(config);
        }
        
        private String getRequiredString(TomlParseResult config, String key) {
            String value = config.getString(key);
            if (value == null) {
                throw new IllegalArgumentException("Configuration manquante: " + key);
            }
            return value;
        }
        
        
        // Getters
        public String getRegion() { return region; }
        public String getOutputLocation() { return outputLocation; }
        public String getWorkgroup() { return workgroup; }
        public String getDatabase() { return database; }
        public AuthMode getAuthMode() { return authMode; }
        public String getUser() { return user; }
        public String getPassword() { return password; }
        public String getProfile() { return profile; }
        public SslConfig getSslConfig() { return sslConfig; }
        public LoggingConfig getLoggingConfig() { return loggingConfig; }
        
        public String getConnectionInfo() {
            return String.format("region=%s, workgroup=%s, database=%s, auth=%s, ssl=%s", 
                               region, workgroup, database, authMode, !sslConfig.disableCertChecking);
        }
    }
    
    // Configuration SSL
    public static class SslConfig {
        private final boolean disableCertChecking;
        
        public SslConfig(TomlParseResult config) {
            this.disableCertChecking = getOptionalBoolean(config, "ssl.disable_cert_checking", false);
        }
        
        private boolean getOptionalBoolean(TomlParseResult config, String key, boolean defaultValue) {
            Boolean value = config.getBoolean(key);
            return value != null ? value : defaultValue;
        }
        
        public boolean isDisableCertChecking() { return disableCertChecking; }
        
        public void applySslSettings() {
            if (disableCertChecking) {
                logger.warn("ATTENTION: Vérification des certificats SSL désactivée - RISQUE DE SÉCURITÉ!");
                System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
            } else {
                // Réactivation de la vérification SSL (par défaut)
                System.setProperty("com.amazonaws.sdk.disableCertChecking", "false");
            }
        }
    }
    
    // Configuration Logging
    public static class LoggingConfig {
        private final String level;
        private final boolean fileLogging;
        private final String logFile;
        
        public LoggingConfig(TomlParseResult config) {
            this.level = getOptionalString(config, "logging.level", "INFO");
            this.fileLogging = getOptionalBoolean(config, "logging.file_logging", true);
            this.logFile = getOptionalString(config, "logging.log_file", "logs/athena-executor.log");
        }
        
        private String getOptionalString(TomlParseResult config, String key, String defaultValue) {
            String value = config.getString(key);
            return value != null ? value : defaultValue;
        }
        
        private boolean getOptionalBoolean(TomlParseResult config, String key, boolean defaultValue) {
            Boolean value = config.getBoolean(key);
            return value != null ? value : defaultValue;
        }
        
        public String getLevel() { return level; }
        public boolean isFileLogging() { return fileLogging; }
        public String getLogFile() { return logFile; }
        
        public void applyLoggingSettings() {
            // Configuration des propriétés système pour Logback
            System.setProperty("LOG_LEVEL", level);
            System.setProperty("FILE_LOGGING", String.valueOf(fileLogging));
            System.setProperty("LOG_FILE", logFile);
            
            // Utilisation du fichier de configuration dynamique
            System.setProperty("logback.configurationFile", "src/main/resources/logback-dynamic.xml");
            
            // Création du dossier de logs si nécessaire
            if (fileLogging) {
                try {
                    Path logDir = Paths.get(logFile).getParent();
                    if (logDir != null && !Files.exists(logDir)) {
                        Files.createDirectories(logDir);
                        System.out.println("Dossier de logs créé: " + logDir);
                    }
                } catch (IOException e) {
                    System.err.println("Impossible de créer le dossier de logs: " + e.getMessage());
                }
            }
            
            System.out.println("Configuration logging appliquée: level=" + level + 
                             ", fileLogging=" + fileLogging + ", logFile=" + logFile);
        }
    }
    
    enum AuthMode {
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

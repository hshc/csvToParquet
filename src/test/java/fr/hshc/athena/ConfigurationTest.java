package fr.hshc.athena;

import org.junit.jupiter.api.Test;
import org.tomlj.TomlParseResult;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    @Test
    void testSslConfigDefault() {
        // Test avec configuration par défaut (SSL activé)
        String configContent = 
            "region = \"eu-west-3\"\n" +
            "output_location = \"s3://bucket/results\"\n" +
            "workgroup = \"primary\"\n" +
            "database = \"default\"\n" +
            "\n" +
            "[auth]\n" +
            "mode = \"aws_profile\"\n" +
            "profile = \"test-profile\"\n";
        
        TomlParseResult config = org.tomlj.Toml.parse(configContent);
        AthenaSqlExecutor.AthenaConfig athenaConfig = new AthenaSqlExecutor.AthenaConfig(config);
        
        assertFalse(athenaConfig.getSslConfig().isDisableCertChecking());
    }
    
    @Test
    void testSslConfigDisabled() {
        // Test avec SSL désactivé
        String configContent = 
            "region = \"eu-west-3\"\n" +
            "output_location = \"s3://bucket/results\"\n" +
            "workgroup = \"primary\"\n" +
            "database = \"default\"\n" +
            "\n" +
            "[auth]\n" +
            "mode = \"aws_profile\"\n" +
            "profile = \"test-profile\"\n" +
            "\n" +
            "[ssl]\n" +
            "disable_cert_checking = true\n";
        
        TomlParseResult config = org.tomlj.Toml.parse(configContent);
        AthenaSqlExecutor.AthenaConfig athenaConfig = new AthenaSqlExecutor.AthenaConfig(config);
        
        assertTrue(athenaConfig.getSslConfig().isDisableCertChecking());
    }
    
    @Test
    void testLoggingConfigDefault() {
        // Test avec configuration logging par défaut
        String configContent = 
            "region = \"eu-west-3\"\n" +
            "output_location = \"s3://bucket/results\"\n" +
            "workgroup = \"primary\"\n" +
            "database = \"default\"\n" +
            "\n" +
            "[auth]\n" +
            "mode = \"aws_profile\"\n" +
            "profile = \"test-profile\"\n";
        
        TomlParseResult config = org.tomlj.Toml.parse(configContent);
        AthenaSqlExecutor.AthenaConfig athenaConfig = new AthenaSqlExecutor.AthenaConfig(config);
        
        assertEquals("INFO", athenaConfig.getLoggingConfig().getLevel());
        assertTrue(athenaConfig.getLoggingConfig().isFileLogging());
        assertEquals("logs/athena-executor.log", athenaConfig.getLoggingConfig().getLogFile());
    }
    
    @Test
    void testLoggingConfigCustom() {
        // Test avec configuration logging personnalisée
        String configContent = 
            "region = \"eu-west-3\"\n" +
            "output_location = \"s3://bucket/results\"\n" +
            "workgroup = \"primary\"\n" +
            "database = \"default\"\n" +
            "\n" +
            "[auth]\n" +
            "mode = \"aws_profile\"\n" +
            "profile = \"test-profile\"\n" +
            "\n" +
            "[logging]\n" +
            "level = \"DEBUG\"\n" +
            "file_logging = false\n" +
            "log_file = \"custom-logs/app.log\"\n";
        
        TomlParseResult config = org.tomlj.Toml.parse(configContent);
        AthenaSqlExecutor.AthenaConfig athenaConfig = new AthenaSqlExecutor.AthenaConfig(config);
        
        assertEquals("DEBUG", athenaConfig.getLoggingConfig().getLevel());
        assertFalse(athenaConfig.getLoggingConfig().isFileLogging());
        assertEquals("custom-logs/app.log", athenaConfig.getLoggingConfig().getLogFile());
    }
    
    @Test
    void testFullConfiguration() {
        // Test avec configuration complète
        String configContent = 
            "region = \"eu-west-3\"\n" +
            "output_location = \"s3://bucket/results\"\n" +
            "workgroup = \"primary\"\n" +
            "database = \"default\"\n" +
            "\n" +
            "[auth]\n" +
            "mode = \"user_password\"\n" +
            "user = \"testuser\"\n" +
            "password = \"testpass\"\n" +
            "\n" +
            "[ssl]\n" +
            "disable_cert_checking = false\n" +
            "\n" +
            "[logging]\n" +
            "level = \"WARN\"\n" +
            "file_logging = true\n" +
            "log_file = \"logs/test.log\"\n";
        
        TomlParseResult config = org.tomlj.Toml.parse(configContent);
        AthenaSqlExecutor.AthenaConfig athenaConfig = new AthenaSqlExecutor.AthenaConfig(config);
        
        // Vérification de la configuration complète
        assertEquals("eu-west-3", athenaConfig.getRegion());
        assertEquals("s3://bucket/results", athenaConfig.getOutputLocation());
        assertEquals("primary", athenaConfig.getWorkgroup());
        assertEquals("default", athenaConfig.getDatabase());
        
        assertEquals(AthenaSqlExecutor.AuthMode.USER_PASSWORD, athenaConfig.getAuthMode());
        assertEquals("testuser", athenaConfig.getUser());
        assertEquals("testpass", athenaConfig.getPassword());
        
        assertFalse(athenaConfig.getSslConfig().isDisableCertChecking());
        
        assertEquals("WARN", athenaConfig.getLoggingConfig().getLevel());
        assertTrue(athenaConfig.getLoggingConfig().isFileLogging());
        assertEquals("logs/test.log", athenaConfig.getLoggingConfig().getLogFile());
    }
}

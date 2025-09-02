# Configuration SSL et Logging - Athena SQL Executor

Ce document détaille les nouvelles fonctionnalités de configuration SSL et logging ajoutées à l'Athena SQL Executor.

## 🔒 Configuration SSL

### Paramètres disponibles

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `ssl.disable_cert_checking` | boolean | `false` | Désactive la vérification des certificats SSL |

### Exemples de configuration

#### SSL sécurisé (recommandé)
```toml
[ssl]
disable_cert_checking = false
```

#### SSL avec vérification désactivée (développement uniquement)
```toml
[ssl]
disable_cert_checking = true
```

### Comportement

- **SSL activé par défaut** : La sécurité SSL est activée par défaut
- **Avertissements de sécurité** : Des avertissements sont affichés si la vérification SSL est désactivée
- **Configuration JDBC** : Les paramètres SSL sont automatiquement ajoutés à l'URL de connexion

## 📊 Configuration Logging

### Paramètres disponibles

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `logging.level` | string | `"INFO"` | Niveau de log (DEBUG, INFO, WARN, ERROR) |
| `logging.file_logging` | boolean | `true` | Active la sauvegarde des logs dans un fichier |
| `logging.log_file` | string | `"logs/athena-executor.log"` | Chemin du fichier de log |

### Exemples de configuration

#### Logging basique
```toml
[logging]
level = "INFO"
file_logging = true
log_file = "logs/athena-executor.log"
```

#### Logging détaillé pour le debug
```toml
[logging]
level = "DEBUG"
file_logging = true
log_file = "logs/debug.log"
```

#### Logging console uniquement
```toml
[logging]
level = "INFO"
file_logging = false
```

#### Logging personnalisé
```toml
[logging]
level = "WARN"
file_logging = true
log_file = "custom-logs/my-app.log"
```

### Fonctionnalités

- **Rotation automatique** : Les logs sont automatiquement rotés par jour
- **Rétention configurable** : Conservation de 30 jours par défaut
- **Création de dossiers** : Les dossiers de logs sont créés automatiquement
- **Configuration dynamique** : Les paramètres sont appliqués au démarrage

## 🔧 Configuration complète

Voici un exemple de configuration complète avec toutes les options :

```toml
# Configuration de base
region = "eu-west-3"
output_location = "s3://your-bucket/query-results"
workgroup = "primary"
database = "default"

# Authentification
[auth]
mode = "aws_profile"
profile = "your-aws-profile"

# Configuration SSL sécurisée
[ssl]
disable_cert_checking = false

# Configuration Logging détaillée
[logging]
level = "INFO"
file_logging = true
log_file = "logs/athena-executor.log"
```

## 🚨 Sécurité

### Bonnes pratiques SSL

1. **Ne jamais désactiver SSL en production**
2. **Utiliser des certificats valides**
3. **Surveiller les avertissements SSL**
4. **Tester la configuration SSL avant déploiement**

### Bonnes pratiques Logging

1. **Configurer des niveaux appropriés**
2. **Activer la rotation des logs**
3. **Surveiller l'espace disque**
4. **Ne pas logger d'informations sensibles**

## 🐛 Dépannage

### Problèmes SSL courants

1. **"SSL handshake failed"**
   - Vérifiez les certificats
   - Vérifiez la configuration réseau
   - Testez avec `disable_cert_checking = true` temporairement

2. **"Certificate not trusted"**
   - Ajoutez le certificat au truststore
   - Vérifiez la chaîne de certificats
   - Contactez l'administrateur système

### Problèmes Logging courants

1. **"Cannot create log directory"**
   - Vérifiez les permissions
   - Vérifiez l'espace disque
   - Utilisez un chemin absolu

2. **"Log file not writable"**
   - Vérifiez les permissions d'écriture
   - Vérifiez que le fichier n'est pas verrouillé
   - Redémarrez l'application

## 📈 Monitoring

### Métriques SSL à surveiller

- Nombre de connexions SSL réussies/échouées
- Temps de négociation SSL
- Erreurs de certificats

### Métriques Logging à surveiller

- Taille des fichiers de log
- Taux de rotation
- Erreurs d'écriture

## 🔄 Migration

### Depuis l'ancienne version

1. **Ajoutez les sections SSL et Logging** à votre `config.toml`
2. **Testez la configuration** en mode développement
3. **Déployez progressivement** en production
4. **Surveillez les logs** pour détecter les problèmes

### Exemple de migration

**Avant :**
```toml
region = "eu-west-3"
output_location = "s3://bucket/results"
workgroup = "primary"
database = "default"

[auth]
mode = "aws_profile"
profile = "default"
```

**Après :**
```toml
region = "eu-west-3"
output_location = "s3://bucket/results"
workgroup = "primary"
database = "default"

[auth]
mode = "aws_profile"
profile = "default"

[ssl]
disable_cert_checking = false

[logging]
level = "INFO"
file_logging = true
log_file = "logs/athena-executor.log"
```

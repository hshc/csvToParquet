# Athena SQL Executor

Un outil Java pour exécuter des requêtes SQL sur Amazon Athena de manière sécurisée et robuste.

## 🚀 Fonctionnalités

- ✅ Exécution de requêtes SQL depuis des fichiers
- ✅ Support de multiples modes d'authentification (AWS Profile, User/Password)
- ✅ Logging complet avec SLF4J/Logback
- ✅ Validation robuste des entrées
- ✅ Gestion d'erreurs améliorée
- ✅ Parsing SQL intelligent (gestion des commentaires)
- ✅ Configuration externalisée en TOML
- ✅ Gestion sécurisée des connexions SSL

## 📋 Prérequis

- Java 11 ou supérieur
- Maven 3.6+
- Accès à Amazon Athena
- Bucket S3 configuré pour les résultats Athena

## 🛠️ Installation

1. **Cloner le projet**
   ```bash
   git clone <repository-url>
   cd athenaSqlExecutor
   ```

2. **Télécharger le driver Athena JDBC**
   - Téléchargez `athena-jdbc-3.5.0-with-dependencies.jar` depuis [AWS](https://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html)
   - Placez-le dans le dossier `lib/`

3. **Compiler le projet**
   ```bash
   mvn clean package
   ```

## ⚙️ Configuration

### Fichier de configuration TOML

Créez un fichier `config.toml` :

```toml
# Région AWS
region = "eu-west-3"

# Emplacement S3 pour les résultats
output_location = "s3://your-bucket/query-results"

# Workgroup Athena
workgroup = "primary"

# Base de données par défaut
database = "default"

# Authentification
[auth]
mode = "aws_profile"  # ou "user_password"
profile = "your-aws-profile"

# Pour l'authentification par utilisateur/mot de passe :
# mode = "user_password"
# user = "your_username"
# password = "your_password"
```

### Configuration AWS

Pour l'authentification par profil AWS :
```bash
aws configure --profile your-aws-profile
```

## 🚀 Utilisation

### Exécution basique
```bash
java -jar target/sql-executor-1.0-snapshot.jar queries.sql config.toml
```

### Exemple de fichier SQL
```sql
-- Requête 1
SELECT COUNT(*) FROM your_table;

-- Requête 2 avec commentaire
SELECT 
    column1,
    column2
FROM your_table 
WHERE condition = 'value';
```

## 📊 Logging

Le système de logging utilise SLF4J avec Logback :

- **Console** : Affichage en temps réel
- **Fichier** : Logs sauvegardés dans `logs/athena-sql-executor.log`
- **Rotation** : Logs quotidiens avec rétention de 30 jours

### Niveaux de log
- `DEBUG` : Informations détaillées pour le débogage
- `INFO` : Informations générales (par défaut)
- `WARN` : Avertissements
- `ERROR` : Erreurs

## 🔒 Sécurité

### Améliorations apportées

1. **SSL sécurisé** : Suppression de la désactivation des certificats
2. **Validation des entrées** : Vérification des fichiers et paramètres
3. **Gestion des ressources** : Fermeture automatique des connexions
4. **Logging sécurisé** : Pas de logs de mots de passe

### Bonnes pratiques

- ✅ Utilisez des profils AWS plutôt que des credentials en dur
- ✅ Configurez des permissions IAM minimales
- ✅ Activez le chiffrement S3 pour les résultats
- ✅ Surveillez les logs d'accès

## 🐛 Dépannage

### Erreurs courantes

1. **"Fichier SQL introuvable"**
   - Vérifiez le chemin du fichier SQL
   - Assurez-vous que le fichier est lisible

2. **"Configuration manquante"**
   - Vérifiez la syntaxe du fichier TOML
   - Assurez-vous que tous les champs requis sont présents

3. **"Erreur d'authentification"**
   - Vérifiez votre configuration AWS
   - Testez avec `aws sts get-caller-identity --profile your-profile`

### Debug

Pour activer le debug :
```bash
java -Dlogback.configurationFile=logback-debug.xml -jar target/sql-executor-1.0-snapshot.jar queries.sql config.toml
```

## 📈 Améliorations futures

- [ ] Support des requêtes paramétrées
- [ ] Exécution parallèle de requêtes
- [ ] Interface web simple
- [ ] Support des métadonnées de requêtes
- [ ] Intégration avec AWS Secrets Manager

## 🤝 Contribution

1. Fork le projet
2. Créez une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 📞 Support

Pour toute question ou problème :
- Ouvrez une issue sur GitHub
- Consultez la documentation AWS Athena
- Vérifiez les logs d'erreur détaillés

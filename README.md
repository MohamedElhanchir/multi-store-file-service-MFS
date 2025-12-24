# File Service – Spring Boot

## Description

Ce projet est un **microservice Spring Boot de gestion des fichiers** permettant l'upload, le stockage, la récupération et la visualisation de fichiers.
Il supporte **trois stratégies de stockage interchangeables** configurables via les propriétés de l'application :

* **DB** : stockage des fichiers en base de données
* **FS** : stockage sur le système de fichiers
* **GED** : stockage via une GED basée sur **MinIO**

Le choix de la stratégie se fait dynamiquement grâce à `@ConditionalOnProperty`.

---

## Fonctionnalités principales

* Upload de fichiers avec validation
* Téléchargement des fichiers
* Prévisualisation des fichiers dans le navigateur
* Récupération des métadonnées
* Validation des extensions et de la taille
* Stockage interchangeable (DB / FS / GED)
* Gestion centralisée des métadonnées en base de données

---

## Architecture générale

Le projet suit une architecture en couches claire :

```
ma.elhanchir.fileservice
│
├── config        → Configuration Spring (RestTemplate)
├── dto           → Objets de transfert (Upload, Metadata, Data)
├── entity        → Entité JPA StoredFile
├── mapper        → Mapping MapStruct
├── repository    → Accès base de données (JPA)
├── service       → Logique métier + stratégies de stockage
├── validation    → Validation des fichiers uploadés
├── utils         → Méthodes utilitaires
└── web           → API REST
```

---

## Stratégies de stockage

### 1. Stockage en base de données (DB)

* Les fichiers sont stockés sous forme de `byte[]`
* Utilise JPA et `@Lob`
* Activé avec :

```properties
file.storage.type=DB
```

### 2. Stockage sur le système de fichiers (FS)

* Les fichiers sont stockés sur le disque
* Organisation par date (`yyyy/MM`)
* Le chemin est sauvegardé en base

```properties
file.storage.type=FS
file.fs.upload-dir=/chemin/vers/uploads
```

### 3. Stockage GED (MinIO)

* Utilise MinIO comme GED
* Création automatique du bucket au démarrage
* Les métadonnées restent en base

```properties
file.storage.type=GED
file.ged.url=http://localhost:9000
file.ged.username=minioadmin
file.ged.password=minioadmin
```

---

## Validation des fichiers

Les règles de validation sont configurables :

```properties
file.allowed.extensions=pdf,png,jpg,jpeg
file.max.size=5242880
```

Contrôles effectués :

* Fichier non vide
* Taille maximale respectée
* Extension autorisée
* Nom de fichier valide

---

## API REST

### Upload d'un fichier

```
POST /files/upload
```

**Paramètre**

* `file` : MultipartFile

### Récupération des métadonnées

```
GET /files/{id}/metadata
```

### Téléchargement du fichier

```
GET /files/{id}/download
```

### Prévisualisation dans le navigateur

```
GET /files/{id}/preview
```


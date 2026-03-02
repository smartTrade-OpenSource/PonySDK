# Analyse Statistique des IDs PonySDK

Ce test génère des visualisations statistiques pour prouver le non-déterminisme et l'unicité des IDs générés par `IdGenerator`.

## 🚀 Utilisation Rapide

**Test complet (100M échantillons)** :
```bash
./run_id_analysis.sh
```

**Test rapide (1M échantillons)** :
```bash
./quick_test.sh
```

## 🛠️ Utilisation Manuelle

1. **Test Java simple** :
```bash
javac TestIdGenerator.java && java TestIdGenerator
```

2. **Générer les graphiques** :
```bash
python3 visualize_id_stats.py
```

## Fichiers Générés

### Données CSV
- `bit_distribution.csv` - Distribution des bits pour histogramme
- `id_timeseries.csv` - Série temporelle des IDs générés  
- `uniqueness.csv` - Évolution du taux d'unicité

### Visualisations
- `id_statistics_analysis.png` - Graphique combiné avec 4 analyses

## Analyses Produites

### 1. Histogramme de Distribution des Bits
- **Objectif** : Prouver l'uniformité de la distribution
- **Attendu** : Chaque bit apparaît ~50% du temps
- **Visualisation** : Barres horizontales avec ligne de référence à 50%

### 2. Graphique ID vs Temps  
- **Objectif** : Montrer l'absence de structure temporelle
- **Attendu** : Nuage de points sans pattern visible
- **Visualisation** : Scatter plot (projection 8 bits hauts vs séquence)

### 3. Courbe d'Unicité
- **Objectif** : Prouver l'absence de collisions
- **Attendu** : Taux proche de 100% sur 100k échantillons
- **Visualisation** : Ligne montrant unique_ids/total_ids

### 4. Test d'Autocorrélation
- **Objectif** : Vérifier la randomness
- **Attendu** : Corrélation proche de 0 pour tous les lags
- **Visualisation** : Courbe d'autocorrélation

## Critères de Validation

Le test échoue si :
- Taux d'unicité < 99%
- Distribution d'un bit s'écarte de ±5% de 50%
- Collisions détectées dans l'échantillon

## 💻 Scripts Disponibles

- `run_id_analysis.sh` - Test complet 100M (avec confirmation)
- `quick_test.sh` - Test rapide 1M (validation)
- `TestIdGenerator.java` - Test Java pur (sans Gradle)
- `visualize_id_stats.py` - Génération des graphiques

## 📦 Dépendances Python

```bash
pip3 install pandas matplotlib seaborn numpy
```

## 📈 Résultats Attendus

- **Unicité** : >99.9% (excellent pour 100M)
- **Distribution bits** : 50% ± 0.1% par bit
- **Collisions** : <100,000 sur 100M
- **Randomness** : Distribution uniforme des projections

## 🔍 Interprétation des Résultats

### Graphique 1: Distribution des Bits
- Chaque barre doit être proche de 50%
- Écart-type < 0.1% = excellent

### Graphique 2: Série Temporelle
- Nuage de points sans structure
- Pas de pattern visible = bon

### Graphique 3: Courbe d'Unicité
- Doit rester proche de 100%
- Chute acceptable < 0.1% pour 100M

### Graphique 4: Distribution des Projections
- Histogramme uniforme
- Toutes les barres à hauteur similaire

## 📊 Paramètres

- **SAMPLE_SIZE** : 100,000,000 IDs (100M)
- **TIMESERIES_SAMPLE** : 100,000 points échantillonnés
- **Tolérance bits** : ±1% de 50% (pour 100M échantillons)
- **Seuil unicité** : 99%
- **Mémoire requise** : ~2-4 GB
- **Temps d'exécution** : 2-5 minutes

## ⚡ Performance

- **Optimisation mémoire** : Génération en streaming
- **Échantillonnage intelligent** : Réduction des données pour visualisation
- **Progress tracking** : Affichage du progrès toutes les 10M générations
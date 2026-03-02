# Concepts Clés - PonySDK

## 1. Application

### Définition
Une **Application** représente une session HTTP utilisateur. C'est un wrapper autour de `HttpSession` qui contient un ou plusieurs `UIContext`.

### Caractéristiques
- **1 Application = 1 HttpSession** : Liée au cycle de vie de la session HTTP
- **Multi-UIContext** : Peut contenir plusieurs UIContext (multi-onglets)
- **Attributs partagés** : Stockage de données partagées entre tous les onglets
- **Gestion centralisée** : Enregistrée dans le `SessionManager`

### Cycle de vie
```java
// Création lors de la première connexion WebSocket
Application app = new Application(sessionId, httpSession, configuration);
SessionManager.get().registerApplication(app);

// Enregistrement d'un UIContext
app.registerUIContext(uiContext);

// Destruction quand tous les UIContext sont fermés
app.destroy(); // Invalide la HttpSession
```

### Cas d'usage
- Partager des données entre plusieurs onglets d'un même utilisateur
- Gérer l'authentification au niveau session
- Push de données vers tous les onglets simultanément

---

## 2. UIContext

### Définition
Un **UIContext** représente un contexte d'exécution pour un onglet ou une fenêtre de navigateur. C'est le cœur de PonySDK où réside l'arbre des widgets et la logique d'interaction.

### Caractéristiques
- **1 UIContext = 1 onglet navigateur** : Isolation complète entre onglets
- **Thread-safe** : Utilise un `ReentrantLock` pour la synchronisation
- **Stateful** : Maintient l'état complet de l'UI côté serveur
- **ID unique** : Identifiant auto-incrémenté global

### Composants internes
```java
UIContext uiContext = ...;

// Arbre des widgets
PObjectCache cache = uiContext.getPObjectCache();
PObject widget = uiContext.getObject(objectId);

// Communication
ModelWriter writer = uiContext.getWriter();
uiContext.pushToClient(data);

// Événements
EventBus eventBus = uiContext.getRootEventBus();
PHistory history = uiContext.getHistory();
PCookies cookies = uiContext.getCookies();

// Attributs locaux
uiContext.setAttribute("key", value);
Object value = uiContext.getAttribute("key");
```

### Pattern ThreadLocal
```java
// Accès au UIContext courant
UIContext current = UIContext.get();

// Exécution dans le contexte
uiContext.execute(() -> {
    // Code exécuté avec le lock acquis
    // UIContext.get() retourne automatiquement uiContext
    PButton button = new PButton("Click me");
});
```

### Cycle de vie
1. **Création** : Lors de la connexion WebSocket
2. **Initialisation** : Appel de `EntryPoint.start(uiContext)`
3. **Actif** : Traitement des événements et mises à jour UI
4. **Destruction** : Fermeture WebSocket ou appel explicite à `destroy()`

---

## 3. PObject

### Définition
**PObject** est la classe de base pour tous les widgets PonySDK. C'est un proxy côté serveur qui représente un élément DOM côté client.

### Hiérarchie
```
PObject (base abstraite)
  ├─ PWidget (widgets visuels)
  │   ├─ PButton
  │   ├─ PLabel
  │   ├─ PTextBox
  │   ├─ PPanel
  │   │   ├─ PFlowPanel
  │   │   ├─ PVerticalPanel
  │   │   └─ PHorizontalPanel
  │   └─ ...
  ├─ PWindow (fenêtre navigateur)
  ├─ PHistory (gestion historique)
  └─ PCookies (gestion cookies)
```

### Mécanisme de synchronisation
```java
// Côté serveur : Création d'un widget
PButton button = new PButton("Click");
// → Génère une instruction ServerToClientModel.CREATE_WIDGET
// → Envoyée au client via WebSocket
// → Client crée l'élément DOM correspondant

// Modification
button.setText("New text");
// → Génère ServerToClientModel.UPDATE_PROPERTY
// → Client met à jour le DOM

// Événement utilisateur
button.addClickHandler(event -> {
    // Déclenché quand le client envoie ClientToServerModel.CLICK_EVENT
    System.out.println("Button clicked!");
});
```

### ID Management
Chaque PObject possède un ID unique généré par `UIContext.nextID()` :
- Permet l'identification côté client et serveur
- Utilisé pour router les événements
- Stocké dans le `PObjectCache` du UIContext

---

## 4. EntryPoint

### Définition
**EntryPoint** est l'interface fonctionnelle qui définit le point d'entrée de votre application PonySDK. C'est l'équivalent de la méthode `main()` pour une application web.

### Interface
```java
@FunctionalInterface
public interface EntryPoint {
    void start(UIContext uiContext);
}
```

### Implémentation typique
```java
public class MyEntryPoint implements EntryPoint {
    
    @Override
    public void start(UIContext uiContext) {
        // Construction de l'UI initiale
        PWindow window = PWindow.getMain();
        
        PVerticalPanel panel = new PVerticalPanel();
        panel.add(new PLabel("Welcome to PonySDK!"));
        
        PButton button = new PButton("Click me");
        button.addClickHandler(event -> {
            Notification.show("Button clicked!");
        });
        panel.add(button);
        
        window.add(panel);
    }
}
```

### Initialisation
```java
public class MyApplicationManager extends ApplicationManager {
    
    @Override
    protected EntryPoint initializeEntryPoint() {
        return new MyEntryPoint();
    }
    
    @Override
    public void start() {
        // Configuration supplémentaire si nécessaire
    }
}
```

---

## 5. Protocole de Communication

### ServerToClientModel
Instructions envoyées du serveur vers le client pour construire/modifier l'UI.

```java
// Exemples d'instructions
ServerToClientModel.CREATE_WIDGET    // Créer un widget
ServerToClientModel.UPDATE_PROPERTY  // Modifier une propriété
ServerToClientModel.ADD_HANDLER      // Ajouter un handler
ServerToClientModel.REMOVE_WIDGET    // Supprimer un widget
ServerToClientModel.DESTROY_CONTEXT  // Détruire le contexte
```

### ClientToServerModel
Événements envoyés du client vers le serveur suite aux interactions utilisateur.

```java
// Exemples d'événements
ClientToServerModel.CLICK_EVENT      // Clic souris
ClientToServerModel.CHANGE_EVENT     // Changement de valeur
ClientToServerModel.KEY_EVENT        // Événement clavier
ClientToServerModel.TYPE_HISTORY     // Changement d'historique
ClientToServerModel.OBJECT_ID        // ID de l'objet concerné
```

### Format de message
```json
{
  "OBJECT_ID": 42,
  "HANDLER_TYPE": "CLICK_EVENT",
  "CLICK_X": 150,
  "CLICK_Y": 200
}
```

---

## 6. ModelWriter

### Définition
**ModelWriter** est responsable de la sérialisation des instructions `ServerToClientModel` vers le format de protocole Pony.

### Utilisation
```java
ModelWriter writer = uiContext.getWriter();

// Début d'un objet
writer.beginObject(window);

// Écriture de propriétés
writer.write(ServerToClientModel.TYPE_CREATE, widgetType);
writer.write(ServerToClientModel.OBJECT_ID, objectId);
writer.write(ServerToClientModel.WIDGET_TEXT, "Hello");

// Fin d'objet
writer.endObject();

// Flush vers le client
uiContext.flush();
```

### Optimisation
Le ModelWriter bufferise les instructions pour minimiser les round-trips réseau.

---

## 7. EventBus

### Définition
**EventBus** est un système de publication-souscription pour la communication inter-composants sans couplage fort.

### Deux versions disponibles

#### EventBus classique
```java
EventBus eventBus = uiContext.getRootEventBus();

// Souscription
eventBus.addHandler(MyEvent.TYPE, event -> {
    System.out.println("Event received: " + event.getData());
});

// Publication
eventBus.fireEvent(new MyEvent("data"));
```

#### EventBus2 (nouvelle version)
```java
com.ponysdk.core.ui.eventbus2.EventBus eventBus = uiContext.getNewEventBus();

// Utilisation similaire avec API améliorée
```

### Cas d'usage
- Communication entre modules découplés
- Notification d'événements métier
- Coordination de composants UI

---

## 8. Transactions (STM)

### Définition
PonySDK utilise un système de **Software Transactional Memory** pour garantir la cohérence des modifications UI.

### Mécanisme
```java
// Transaction automatique dans execute()
uiContext.execute(() -> {
    Txn txn = Txn.get();
    txn.begin(context);
    try {
        // Modifications UI
        button.setText("New text");
        label.setVisible(false);
        
        txn.commit(); // Envoi des instructions au client
    } catch (Exception e) {
        txn.rollback(); // Annulation des modifications
        throw e;
    }
});
```

### Avantages
- **Atomicité** : Toutes les modifications sont appliquées ou aucune
- **Cohérence** : État UI toujours cohérent
- **Isolation** : Transactions isolées entre threads
- **Durabilité** : Instructions persistées jusqu'à l'envoi

---

## 9. PHistory

### Définition
**PHistory** gère l'historique de navigation et les tokens d'URL pour le deep-linking.

### Utilisation
```java
PHistory history = uiContext.getHistory();

// Ajouter un token
history.newItem("page/details/123", true); // true = fire event

// Écouter les changements
history.addValueChangeHandler(event -> {
    String token = event.getData();
    navigateToPage(token);
});

// Récupérer le token initial
String initialToken = uiContext.getHistoryToken();
```

### Cas d'usage
- Navigation entre pages/vues
- Deep-linking (URLs partageables)
- Boutons précédent/suivant du navigateur

---

## 10. DataListener & Push Serveur

### Définition
**DataListener** permet de pousser des données du serveur vers le client de manière asynchrone.

### Implémentation
```java
// Enregistrement d'un listener
uiContext.addDataListener(data -> {
    if (data instanceof StockPrice) {
        updateStockDisplay((StockPrice) data);
    }
});

// Push depuis un thread serveur
executorService.submit(() -> {
    StockPrice price = fetchLatestPrice();
    uiContext.pushToClient(price);
});

// Push vers tous les onglets d'un utilisateur
application.pushToClients(notification);
```

### Cas d'usage
- Notifications temps réel
- Mises à jour de données en streaming
- Chat, tableaux de bord temps réel

---

## 11. Pony Driver

### Définition
**Pony Driver** est un driver Selenium-compatible qui permet de tester les applications PonySDK sans navigateur, en communiquant directement via le protocole WebSocket.

### Caractéristiques
- Compatible Selenium WebDriver API
- Communication pure WebSocket (pas de HTML/JS)
- Tests fonctionnels rapides
- Même version que le serveur PonySDK

### Utilisation
```java
PonySDKWebDriver driver = new PonySDKWebDriver();
driver.get("ws://localhost:8081/sample/ws");

WebDriverWait wait = new WebDriverWait(driver, 10L);

// Sélection par classe CSS
wait.until(d -> d.findElement(By.className("login-button")));

// Sélection par type de widget
WebElement textbox = driver.findElement(By.tagName("TEXTBOX"));
textbox.sendKeys("admin");

// Clic sur un bouton
driver.findElement(By.cssSelector("BUTTON.submit")).click();
```

### Sélecteurs disponibles
- **id** : Attribut id du widget
- **name** : Attribut name du widget
- **className** : Classes CSS (toutes doivent matcher)
- **tagName** : Type de widget (BUTTON, TEXTBOX, etc.)
- **cssSelector** : Combinaison type + classes

---

## 12. ApplicationConfiguration

### Définition
**ApplicationConfiguration** contient les paramètres de configuration globaux de l'application.

### Utilisation
```java
ApplicationConfiguration config = new ApplicationConfiguration();
config.setApplicationName("MyApp");
config.setApplicationDescription("My PonySDK Application");

ApplicationManager manager = new MyApplicationManager();
manager.setConfiguration(config);
```

### Accès
```java
// Depuis UIContext
ApplicationConfiguration config = uiContext.getConfiguration();

// Depuis Application
ApplicationConfiguration config = application.getOptions();
```

---

## 13. SessionManager

### Définition
**SessionManager** est un singleton qui gère toutes les `Application` actives sur le serveur.

### Utilisation
```java
SessionManager manager = SessionManager.get();

// Enregistrement
manager.registerApplication(application);

// Récupération
Application app = manager.getApplication(sessionId);

// Désenregistrement
manager.unregisterApplication(application);
```

### Cas d'usage
- Monitoring des sessions actives
- Broadcast vers toutes les applications
- Gestion centralisée du cycle de vie

---

## 14. Latency Monitoring

### Définition
UIContext intègre un système de mesure de latence pour monitorer les performances.

### Métriques disponibles
```java
// Latence round-trip (aller-retour complet)
double roundtrip = uiContext.getRoundtripLatency();

// Latence réseau
double network = uiContext.getNetworkLatency();

// Latence terminal (traitement client)
double terminal = uiContext.getTerminalLatency();
```

### Utilisation
- Diagnostic de performance
- Détection de problèmes réseau
- Optimisation de l'expérience utilisateur

---

## 15. PAddons

### Définition
**PAddons** permet d'intégrer des frameworks JavaScript externes (React, Vue, D3.js, etc.) dans PonySDK.

### Concept
- Pont entre le code Java serveur et les bibliothèques JS client
- Permet d'utiliser des composants JS tout en gardant la logique en Java
- Communication bidirectionnelle via le protocole Pony

### Cas d'usage
- Intégration de graphiques complexes (D3.js, Chart.js)
- Utilisation de composants UI modernes
- Réutilisation de code JavaScript existant

---

## Résumé des Relations

```
SessionManager
    └─ Application (1 par HttpSession)
        └─ UIContext (1 par onglet)
            ├─ PObjectCache
            │   └─ PObject (widgets)
            ├─ ModelWriter (sérialisation)
            ├─ EventBus (événements)
            ├─ PHistory (navigation)
            ├─ PCookies (cookies)
            └─ DataListener (push serveur)

ApplicationManager
    ├─ ApplicationConfiguration
    └─ EntryPoint (point d'entrée)

WebSocketServlet
    └─ WebSocket
        └─ TxnContext (transactions)
            └─ Txn (begin/commit/rollback)
```

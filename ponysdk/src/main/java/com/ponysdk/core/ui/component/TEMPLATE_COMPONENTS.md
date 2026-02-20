# Template Components

## Vue d'ensemble

`PTemplateComponent` permet de créer des composants PonySDK en utilisant des templates HTML définis côté client. Le serveur envoie uniquement les props en JSON via WebSocket, optimisant ainsi la bande passante.

## Architecture

- **Serveur (Java)** : Gère l'état (props) et la logique métier
- **Client (JavaScript)** : Définit le template HTML et les styles une seule fois
- **WebSocket** : Transmet uniquement les changements de props en JSON (JSON Patch)

## Avantages

1. **Performance** : Pas de HTML envoyé via WebSocket, seulement du JSON
2. **Efficacité** : Utilise JSON Patch pour les mises à jour différentielles
3. **Simplicité** : Pas besoin de React/Vue/Svelte pour des composants simples
4. **Sécurité** : Échappement HTML automatique contre XSS
5. **Encapsulation** : Styles scopés via Shadow DOM

## Utilisation

### 1. Définir les Props (Java)

```java
public record CardProps(
    String title, 
    String content, 
    String color, 
    boolean highlighted
) {}
```

### 2. Créer le composant serveur (Java)

```java
public class CardComponent extends PTemplateComponent<CardProps> {

    public CardComponent() {
        super(new CardProps("Title", "Content", "#3498db", false));
        
        // Enregistrer les gestionnaires d'événements
        onEvent("cardClick", payload -> {
            System.out.println("Card clicked!");
            toggleHighlight();
        });
    }

    @Override
    protected Class<CardProps> getPropsClass() {
        return CardProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "card-component";  // Doit correspondre au nom JS
    }

    // API publique
    public void toggleHighlight() {
        CardProps current = getCurrentProps();
        setProps(new CardProps(
            current.title(), 
            current.content(), 
            current.color(), 
            !current.highlighted()
        ));
    }
}
```

### 3. Enregistrer le template (JavaScript)

```javascript
// card-component-register.js
registerTemplateComponent('card-component', {
    template: `
        <div class="card {{#highlighted}}highlighted{{/highlighted}}" 
             style="border-color: {{color}}" 
             data-event="cardClick">
            <h2>{{title}}</h2>
            <p>{{content}}</p>
            <button data-event="deleteClick">Delete</button>
        </div>
    `,
    styles: `
        .card {
            border: 2px solid;
            padding: 1rem;
            border-radius: 8px;
            cursor: pointer;
        }
        .card.highlighted {
            box-shadow: 0 0 20px rgba(52, 152, 219, 0.5);
        }
    `
});
```

### 4. Charger les scripts (HTML)

```html
<script src="script/template-component-renderer.js"></script>
<script src="script/card-component-register.js"></script>
```

## Syntaxe du template

### Interpolation de variables

```html
<h1>{{title}}</h1>
<p>{{content}}</p>
<div style="color: {{color}}">{{message}}</div>
```

### Blocs conditionnels

```html
<!-- Afficher si la variable est truthy -->
{{#highlighted}}
  <div class="highlight">Highlighted!</div>
{{/highlighted}}

<!-- Afficher si la variable est falsy -->
{{^highlighted}}
  <div>Not highlighted</div>
{{/highlighted}}
```

### Gestion d'événements

```html
<!-- Événement simple -->
<button data-event="click">Click me</button>

<!-- Événements multiples -->
<div data-event="cardClick,hover">Interactive card</div>

<!-- Événement personnalisé -->
<button data-event="deleteClick">Delete</button>
```

## Flux de données

```
1. Serveur crée CardComponent avec props initiales
   ↓
2. WebSocket envoie: { type: "COMPONENT_CREATE", props: {...} }
   ↓
3. Client rend le template avec les props
   ↓
4. Utilisateur clique sur le composant
   ↓
5. Client envoie: { eventType: "cardClick", payload: {...} }
   ↓
6. Serveur appelle toggleHighlight()
   ↓
7. setProps() génère un JSON Patch
   ↓
8. WebSocket envoie: { type: "COMPONENT_UPDATE", patch: [...] }
   ↓
9. Client applique le patch et re-rend
```

## Comparaison avec PReactComponent

| Critère | PTemplateComponent | PReactComponent |
|---------|-------------------|-----------------|
| Complexité | Simple | Complexe |
| Dépendances | Aucune | React 18 |
| Logique client | Minimale | Complète |
| Performance | Excellente | Très bonne |
| Cas d'usage | Composants simples | Composants riches |

**Utilisez PTemplateComponent quand :**
- Vous avez besoin de composants simples avec peu de logique
- Vous préférez HTML/CSS pur
- Vous voulez éviter les dépendances JavaScript
- La logique métier est côté serveur

**Utilisez PReactComponent quand :**
- Vous avez besoin de logique complexe côté client
- Vous voulez utiliser des bibliothèques React existantes
- Vous avez besoin d'interactions riches sans round-trip serveur
- Vous avez des animations complexes

## Exemple complet

Voir les fichiers :
- `CardComponent.java` - Composant serveur
- `card-component-register.js` - Template client
- `TemplateComponentPageActivity.java` - Page de démonstration

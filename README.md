# Le Mie Scadenze

App Android (Kotlin + Jetpack Compose) per tenere traccia di auto e moto: scadenze di
**revisione**, **assicurazione**, **bollo** (opzionale) e storico **manutenzioni**, con
notifiche automatiche quando una scadenza si avvicina (30, 15, 7, 1 giorno prima e il
giorno stesso).

## Funzionalità

- Scheda per ogni veicolo (auto o moto): nome, targa, marca, modello, anno, note.
- Date di scadenza revisione / assicurazione / bollo, con indicatore colorato
  (verde / arancio / rosso) in base all'urgenza.
- Storico manutenzioni per veicolo: tipo (scelto da un elenco predefinito o libero),
  data, km, costo, officina, note.
- Promemoria ricorrenti: imposta "ogni tot mesi" e la prossima scadenza della
  manutenzione viene calcolata e notificata automaticamente.
- Riepilogo per veicolo: numero di interventi, spesa totale e data dell'ultimo intervento.
- La prossima manutenzione in scadenza è visibile anche nella lista principale, sotto
  a revisione e assicurazione.
- Notifiche locali giornaliere (WorkManager) che avvisano quando una scadenza è vicina,
  anche se l'app non è aperta. Le notifiche riprendono automaticamente dopo il riavvio
  del telefono.
- Dati salvati localmente sul dispositivo (database Room/SQLite) — nessuna connessione
  a internet richiesta.

## Come aprire il progetto

1. Installa [Android Studio](https://developer.android.com/studio) (versione recente,
   Koala o successiva).
2. Apri Android Studio → **Open** → seleziona la cartella `RevisioniApp`.
3. Lascia che Android Studio scarichi le dipendenze e sincronizzi Gradle (la prima
   volta può richiedere qualche minuto).
4. Collega un telefono Android (con debug USB attivo) oppure avvia un emulatore.
5. Premi **Run ▶** per installare e avviare l'app.

Requisiti minimi: Android 8.0 (API 26) o superiore.

## Struttura del progetto

```
app/src/main/java/com/revisioni/app/
├── data/                     Entità Room (Vehicle, Maintenance), DAO, database, repository
│   └── notification/         Worker per il controllo scadenze, notifiche, ricevitore di boot
├── ui/
│   ├── screens/               Schermate Compose (lista, dettaglio, form)
│   ├── components/            Componenti riutilizzabili (selettore data, badge scadenza)
│   ├── theme/                 Tema Material 3
│   └── AppViewModel.kt         ViewModel condiviso
└── MainActivity.kt
```

## Note

- La prima apertura chiede il permesso di inviare notifiche (richiesto da Android 13+).
- Puoi rigenerare il file `gradle-wrapper.jar` mancante lasciando che Android Studio lo
  scarichi automaticamente al primo sync, oppure eseguendo `gradle wrapper` se hai
  Gradle installato a parte.

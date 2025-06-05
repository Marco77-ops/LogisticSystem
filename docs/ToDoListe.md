# 📋 To-do-Liste – Seminararbeit „DevOps und Cloud Computing“ (Kafka)

**Projekt:** LuckyPets Logistics System  
**Abgabe:** 19.07.2025  
**Präsentation:** 01.07.2025  
**Autor:** [Dein Name]  
**Stand:** 13.05.2025

---

## ✅ Erledigt

- Architekturkonzept Event-Driven mit Kafka
- ShipmentService, ScanService, DeliveryService implementiert
- Events (Created, Scanned, Delivered) + Vererbung von `AbstractEvent`
- Services kommunizieren über Kafka
- Dockerisierung & Multi-Modul-Maven-Struktur
- AnalyticService begonnen (Kafka Streams vorbereitet)

---

## 🧪 Tests

### 🧱 Unit-Tests
- [X] ShipmentService: Logik-Tests (z. B. Erstellung)
- [X] ScanService: Validierung & Event-Generierung
- [X] DeliveryService: Zielort-Erkennung, Delivered-Status
- [ ] Event-Klassen: Serialisierung, Validierung

### 🔄 Integrationstests
- [ ] Kafka-Producer/Consumer pro Service (z. B. mit EmbeddedKafka)
- [X] REST-Endpunkte (MockMvc/WebTestClient)
- [ ] Datenbankintegration (In-Memory oder Testcontainer)

### 🌐 End-to-End-Tests
- [ ] Shipment → Scan → Delivery: kompletter Event-Flow
- [ ] Test mit echten Docker-Services (manuell oder automatisiert)

---

## 📦 NotificationViewService

- [ ] Neuen Microservice `notificationviewservice` erstellen
- [ ] Kafka-Consumer für relevante Events
- [ ] Benachrichtigungen loggen oder über REST abrufbar machen
- [ ] Optional: In-Memory-Speicher für einfache Anzeige

---

## 📈 AnalyticService

- [X] Kafka Streams Topologie finalisieren (Aggregation je Stunde & Ort)
- [X] Streams-Test mit `TopologyTestDriver`
- [ ] REST-Endpunkt zur Anzeige aggregierter Daten
- [ ] Visualisierung oder Logausgabe vorbereiten

---

## 📘 Dokumentation (Seminararbeit)

### Kapitel
- [ ] **Kapitel 4**: Implementierung (Microservices, Events, Topics, Kommunikation)
- [ ] **Kapitel 5**: Tests (strategisch + Beispiele)
- [ ] **Kapitel 6**: Reflexion, Herausforderungen, Ausblick

### Diagramme
- [ ] Event-Flow-Diagramm (z. B. PlantUML)
- [ ] Architekturübersicht (Kafka, Services, Topics)
- [ ] Kafka Streams Topologie

---

## 🎤 Präsentation (01.07.2025)

- [ ] 15–20 Folien (Problem, Lösung, Architektur, Events, Lessons Learned)
- [ ] Optional: Live-Demo mit Docker + Postman
- [ ] Backup: GIFs/Screenshots der Event-Flow-Ausgabe
- [ ] 1–2 Probedurchläufe

---

## 🧾 Abgabe Seminararbeit (19.07.2025)

- [ ] Feinschliff Kapitel 1–6
- [ ] Literaturverzeichnis (APA oder Harvard)
- [ ] Screenshots/Logs/Diagramme als Anhang
- [ ] Formatierung & PDF-Erstellung
- [ ] Fristgerechte Abgabe

---

## 📅 Zeitplan (empfohlen)

| KW  | Zeitraum        | Fokus                                    |
|-----|------------------|------------------------------------------|
| 20  | 13.05.–19.05.    | Unit-/Integrationstests abschließen      |
| 21  | 20.05.–26.05.    | NotificationViewService + Analytics finalisieren |
| 22  | 27.05.–02.06.    | Kafka Streams + REST-API testen          |
| 23  | 03.06.–09.06.    | Kapitel 4–5 schreiben                    |
| 24  | 10.06.–16.06.    | Diagramme + Kapitel 6                    |
| 25  | 17.06.–23.06.    | Slides + Demo-Vorbereitung               |
| 26  | 24.06.–30.06.    | Generalprobe Präsentation                |
| 27  | 01.07.2025       | 🎤 Präsentation                          |
| 28–29 | bis 19.07.     | Feinschliff + Abgabe                     |

---

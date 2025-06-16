# Requirements – LuckyPets Logistics System (DevOps & Cloud Computing Seminar)

## 1. Architektur

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| A1    | Microservice-Architektur mit Shipment, Scan, Delivery, Analytics, Notification Services   | erledigt | docker-compose.yml      | Siehe README             |
| A2    | Event-Driven Communication über Apache Kafka                                             | erledigt | shared/events           | Kafka Producer/Consumer  |
| A3    | Standardisiertes Event-Modell (AbstractEvent, correlationId, etc.)                       | erledigt | shared/events           | BaseEvent, AbstractEvent |
| A4    | Erweiterbarkeit durch lose Kopplung & Event-Modell                                       | erledigt | README                  | Reflection & Outlook     |
| A5    | Alle Kern-Events und Flows korrekt abgebildet                                            | erledigt | demo_script.md          | End-to-End Flow          |

## 2. DevOps-Praktiken

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| D1    | CI/CD-Pipeline für Build, Test & Deployment                                              | offen    |                         |                          |
| D2    | Automatisierte Code-Qualitätschecks (Linting, Coverage)                                  | offen    |                         |                          |
| D3    | Jeder Service ist dockerisiert (eigener Dockerfile)                                      | erledigt | README, docker-compose  |                          |
| D4    | Zentrales Deployment via docker-compose                                                  | erledigt | README                  |                          |

## 3. Cloud & Infrastruktur

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| C1    | Cloud-Deployment vorbereitet oder beschrieben                                            | offen    |                         |                          |
| C2    | Infrastructure as Code (z. B. docker-compose, optional: Terraform/K8s)                   | erledigt | README, docker-compose  |                          |

## 4. Testing & Monitoring

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| T1    | Unit-Tests für Core Business Logic                                                       | erledigt | */unittest              | README dokumentiert      |
| T2    | Integrationstests für Event-Flows & REST Endpunkte                                       | erledigt | */integrationtest       | README dokumentiert      |
| T3    | End-to-End-Test für Gesamtflow                                                           | erledigt | demo_script.md          | README dokumentiert      |
| T4    | Monitoring und Logging vorhanden (z. B. Kafka UI, Healthchecks)                          | erledigt | README                  | Kafka UI                 |

## 5. Dokumentation & Nachvollziehbarkeit

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| DOC1  | Architekturdiagramm und Event-Flow im README dokumentiert                                | erledigt | README                  |                          |
| DOC2  | README mit Projektbeschreibung, Setup & API-Doku                                         | erledigt | README                  |                          |
| DOC3  | Event-Modelle und Topics dokumentiert                                                    | erledigt | README                  |                          |

## 6. Praxisbezug & Anwendung

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| P1    | Logistik-Use-Case realitätsnah umgesetzt                                                 | erledigt | README                  |                          |
| P2    | System ist lokal ausführbar und testbar                                                  | erledigt | README                  |                          |

## 7. Reflexion & Ausblick

| ID    | Requirement                                                                              | Status   | Nachweis / Link / Commit | Notizen                  |
|-------|------------------------------------------------------------------------------------------|----------|-------------------------|--------------------------|
| R1    | Reflexion zu Architektur/Technologien im Abschluss oder Ausblick enthalten               | erledigt | README                  | Reflection & Outlook     |
| R2    | Verbesserungs- und Erweiterungsvorschläge dokumentiert                                   | erledigt | README                  | Future Enhancements      |

---

## Hinweise

- **Status**: Trage „offen“ oder „erledigt“ ein, aktualisiere regelmäßig.
- **Nachweis**: Füge Links zu Code, README, Screenshots oder Commits hinzu.
- Passe die Liste nach Bedarf an, ergänze projektspezifische Besonderheiten.

---

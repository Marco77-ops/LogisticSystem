# LuckyPets Logistics System - Demonstration Guide

This guide provides resources for demonstrating the LuckyPets Logistics System, an event-driven microservice architecture built with Spring Boot and Apache Kafka.

## Demonstration Resources

This repository includes two demonstration scripts:

1. **[Detailed Demonstration Script](demo_script.md)** - A comprehensive step-by-step guide for testing and exploring the system, including detailed explanations and commands.

2. **[Presentation Script](presentation_script.md)** - A concise script designed for live presentations, with timing guidelines and narration points.

## Demonstration Workflow

Both scripts follow the same basic workflow:

1. Start the system using Docker Compose
2. Create a shipment via the Shipment Service
3. Scan the shipment at its origin location
4. Scan the shipment at its destination location (triggering delivery)
5. Check notifications in the Notification Service
6. Observe analytics processing in the Analytics Service
7. Demonstrate error handling and system resilience

## Preparation for Demonstration

Before giving a demonstration:

1. Ensure Docker and Docker Compose are installed and working
2. Clone the repository and navigate to the root directory
3. Review the scripts to familiarize yourself with the commands and flow
4. Consider running through the demonstration once to ensure everything works as expected
5. Prepare a terminal with a clear font and sufficient size for visibility
6. If presenting remotely, consider having a pre-recorded demo as backup

## Tips for Effective Demonstration

1. **Explain as you go** - Narrate what's happening at each step, especially when events are flowing between services
2. **Show the logs** - Use `docker-compose logs -f <service>` to show events being published and consumed
3. **Use the Kafka UI** - Show the Kafka UI (http://localhost:8080) to visualize the event flow
4. **Highlight key architectural benefits** - Emphasize loose coupling, scalability, and resilience
5. **Be prepared for questions** - Have a good understanding of the system architecture and event flow

## Troubleshooting

If you encounter issues during the demonstration:

1. **Services not starting** - Check Docker logs with `docker-compose logs`
2. **API calls failing** - Ensure all services are running and check the logs for errors
3. **Events not flowing** - Check Kafka is running and topics are created
4. **Reset the system** - Use `docker-compose down -v && docker-compose up -d` to reset the system

## Additional Resources

- [Architecture Documentation](docs/architecture.md) - Detailed information about the system architecture
- [API Documentation](http://localhost:8081/swagger-ui.html) - Swagger UI for service APIs (available when services are running)
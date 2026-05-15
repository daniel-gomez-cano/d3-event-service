# event-service

Repositorio central: [d3-central-repo](https://github.com/F4RGAN2007/d3-central-repo)

Microservicio event-service encargado de creación, publicación, listar eventos, tipos de tiquetes, lógica de cupos, precios y códigos promocionales.

- Implementa flujo de creacion y publicacion de eventos con estado DRAFT -> PUBLISHED.
- Soporte para multiples tipos de boleta en la creacion del evento.
- Endpoint de reserva de cupos para el order-service.
- Endpoint de liberacion de cupos para cancelaciones.

## PARA CREAR EVENTO EN DRAFT:
` curl -i -X POST "http://localhost:8081/api/v1/events" \
  -H "Authorization: Bearer EL_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concierto Rock 2026",
    "description": "Evento de prueba",
    "eventDate": "2026-06-30T20:00:00",
    "location": "Bogota",
    "imageUrl": "https://example.com/img.jpg",
    "maxCapacity": 100,
    "ticketTypes": [
      { "name": "General", "price": 30000, "quantity": 80 },
      { "name": "VIP", "price": 60000, "quantity": 20 }
    ]
  }'
`
## DEBERÍA DE RESPONDER:
	HTTP/1.1 201
	X-Content-Type-Options: nosniff
	X-XSS-Protection: 0
	Cache-Control: no-cache, no-store, max-age=0, must-revalidate
	Pragma: no-cache
	Expires: 0
	X-Frame-Options: DENY
	Content-Type: application/json
	Transfer-Encoding: chunked
	Date: Fri, 15 May 2026 23:16:52 GMT

	{"id":1,"name":"Concierto Rock 2026","description":"Evento de prueba","eventDate":"2026-06-30T20:00:00","location":"Bogota","imageUrl":"https://example.com/img.jpg","maxCapacity":100,"soldTickets":0,"remainingCapacity":100,"minPrice":30000,"soldOut":false,"status":"DRAFT","ticketTypes":[{"id":1,"name":"General","price":30000,"availableQuantity":80,"remainingStock":80},{"id":2,"name":"VIP","price":60000,"availableQuantity":20,"remainingStock":20}],"createdAt":"2026-05-15T23:16:52.224149","updatedAt":"2026-05-15T23:16:52.224253"}

## PARA PUBLICAR EVENTO: DRAFT -> PUBLISHED
`curl -i -X PATCH "http://localhost:8081/api/v1/events/1/publish" \
  -H "Authorization: Bearer EL_TOKEN" `

## DEBERÍA DE RESPONDER
	HTTP/1.1 200
	X-Content-Type-Options: nosniff
	X-XSS-Protection: 0
	Cache-Control: no-cache, no-store, max-age=0, must-revalidate
	Pragma: no-cache
	Expires: 0
	X-Frame-Options: DENY
	Content-Type: application/json
	Transfer-Encoding: chunked
	Date: Fri, 15 May 2026 23:18:43 GMT

	{"id":1,"name":"Concierto Rock 2026","description":"Evento de prueba","eventDate":"2026-06-30T20:00:00","location":"Bogota","imageUrl":"https://example.com/img.jpg","maxCapacity":100,"soldTickets":0,"remainingCapacity":100,"minPrice":30000.00,"soldOut":false,"status":"PUBLISHED","ticketTypes":[{"id":1,"name":"General","price":30000.00,"availableQuantity":80,"remainingStock":80},{"id":2,"name":"VIP","price":60000.00,"availableQuantity":20,"remainingStock":20}],"createdAt":"2026-05-15T23:16:52.224149","updatedAt":"2026-05-15T23:16:52.224253"}

## LISTAR EVENTOS PUBLICADOS (ENDPOINT PÚBLICO)

`curl "http://localhost:8081/api/v1/events?page=1&limit=12" `

## DEBERÍA DE RESPONDER

	{"content":[{"id":1,"name":"Concierto Rock 2026","eventDate":"2026-06-30T20:00:00","location":"Bogota","imageUrl":"https://example.com/img.jpg","minPrice":30000.00,"soldOut":false}],"page":1,"size":12,"totalElements":1,"totalPages":1,"last":true}


## Docker (local)

Requisitos: Docker y Docker Compose.

Este servicio no levanta Keycloak. El Keycloak vive en auth-service.
Puedes probar endpoints publicos sin auth-service, pero para rutas protegidas
necesitas que el Keycloak del auth-service este activo.

1) Asegura una red compartida (por ejemplo viva-eventos-net) entre auth-service y event-service.
2) Copia .env.example a .env y ajusta KEYCLOAK_ISSUER_URI segun tu setup.
3) Levanta el event-service:
	docker compose up --build
4) Event-service queda en http://localhost:8081

Ejemplos de KEYCLOAK_ISSUER_URI:
- http://viva_keycloak:8080/realms/viva-eventos (Keycloak en Docker, misma red)
- http://host.docker.internal:8080/realms/viva-eventos (Keycloak en host)

## Endpoints publicos

- GET /api/v1/events
	- Filtros: keyword, location, dateFrom, dateTo
	- Paginacion: page (default 1), limit (default 12, max 50)
	- Devuelve PagedResponse con content y metadatos.
	- Si dateFrom > dateTo retorna 422 con mensaje "dateFrom must be before dateTo".

- GET /api/v1/events/search
	- Alias temporal para compatibilidad hacia atras.

Ejemplos:
- /api/v1/events?keyword=rock&location=bogota&page=1&limit=12
- /api/v1/events?dateFrom=2026-06-01&dateTo=2026-06-30

Campos adicionales en el listado:
- minPrice: precio minimo entre tipos de boleta activos.
- soldOut: true cuando todos los tipos activos tienen remainingStock = 0.

## Benchmark de busqueda

La optimizacion de indices se documenta con una medicion antes/despues.
Para preparar el dataset y medir:

1) Cargar al menos 1.000 eventos usando el script scripts/seed-events.sql
2) Ejecutar EXPLAIN ANALYZE sobre la consulta de filtrado, por ejemplo:
	 EXPLAIN ANALYZE
	 SELECT * FROM events
	 WHERE status = 'PUBLISHED'
		 AND (LOWER(name) LIKE '%rock%' OR LOWER(description) LIKE '%rock%')
	 ORDER BY event_date ASC
	 LIMIT 12 OFFSET 0;

Registrar tiempos antes y despues de aplicar indices.

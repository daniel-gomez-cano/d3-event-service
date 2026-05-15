# event-service

Repositorio central: [d3-central-repo](https://github.com/F4RGAN2007/d3-central-repo)

Microservicio event-service encargado de creación, publicación, listar eventos, tipos de tiquetes, lógica de cupos, precios y códigos promocionales.

- Implementa flujo de creacion y publicacion de eventos con estado DRAFT -> PUBLISHED.
- Soporte para multiples tipos de boleta en la creacion del evento.
- Endpoint de reserva de cupos para el order-service.
- Endpoint de liberacion de cupos para cancelaciones.

-- Postgres-only seed script for benchmark datasets
-- Creates 1,000 published events and two ticket types per event.

INSERT INTO events (
    name,
    description,
    event_date,
    location,
    image_url,
    max_capacity,
    sold_tickets,
    status,
    organizer_keycloak_id,
    created_at,
    updated_at
)
SELECT
    'Evento ' || gs,
    'Descripcion ' || gs,
    NOW() + (gs || ' days')::interval,
    CASE
        WHEN gs % 3 = 0 THEN 'Bogota'
        WHEN gs % 3 = 1 THEN 'Medellin'
        ELSE 'Cali'
    END,
    NULL,
    100,
    0,
    'PUBLISHED',
    'seed-organizer',
    NOW(),
    NOW()
FROM generate_series(1, 1000) AS gs;

INSERT INTO ticket_types (
    event_id,
    name,
    price,
    available_quantity,
    sold_quantity,
    active
)
SELECT e.id, 'General', 50.00, 100, 0, true
FROM events e
WHERE e.organizer_keycloak_id = 'seed-organizer';

INSERT INTO ticket_types (
    event_id,
    name,
    price,
    available_quantity,
    sold_quantity,
    active
)
SELECT e.id, 'VIP', 80.00, 50, 0, true
FROM events e
WHERE e.organizer_keycloak_id = 'seed-organizer';

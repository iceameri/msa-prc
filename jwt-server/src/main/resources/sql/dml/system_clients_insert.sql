INSERT INTO system_clients (client_id, display_name)
VALUES
    ('jwt-server',    'JWT Server'),
    ('opaque-server', 'Opaque Server')
ON CONFLICT (client_id) DO NOTHING;

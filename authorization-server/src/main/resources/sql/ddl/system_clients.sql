CREATE TABLE IF NOT EXISTS system_clients (
    client_id    VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_system_clients PRIMARY KEY (client_id)
);

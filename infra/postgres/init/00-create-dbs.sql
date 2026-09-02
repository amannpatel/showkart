-- Provision one logical database per bounded context.
-- Table DDL is owned by each service's Flyway migrations, not this script.

CREATE DATABASE auth_db;
CREATE DATABASE show_db;
CREATE DATABASE booking_db;
CREATE DATABASE payment_db;
CREATE DATABASE notif_db;
CREATE DATABASE gateway_db;

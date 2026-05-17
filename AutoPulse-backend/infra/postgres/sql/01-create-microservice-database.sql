DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fleet_user') THEN
            CREATE ROLE fleet_user LOGIN PASSWORD 'fleet_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'parcel_user') THEN
            CREATE ROLE parcel_user LOGIN PASSWORD 'parcel_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'delivery_user') THEN
            CREATE ROLE delivery_user LOGIN PASSWORD 'delivery_execution_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'geography_user') THEN
            CREATE ROLE geography_user LOGIN PASSWORD 'geography_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'notification_user') THEN
            CREATE ROLE notification_user LOGIN PASSWORD 'notification_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'routing_user') THEN
            CREATE ROLE routing_user LOGIN PASSWORD 'routing_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'user_user') THEN
            CREATE ROLE user_user LOGIN PASSWORD 'user_password';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak_user') THEN
            CREATE ROLE keycloak_user LOGIN PASSWORD 'keycloak_password';
        END IF;
    END
$$;

SELECT 'CREATE DATABASE fleet_db OWNER fleet_user'
    WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'fleet_db') \gexec

SELECT 'CREATE DATABASE parcel_db OWNER parcel_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'parcel_db') \gexec

SELECT 'CREATE DATABASE delivery_db OWNER delivery_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'delivery_db') \gexec

SELECT 'CREATE DATABASE geography_db OWNER geography_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'geography_db') \gexec

SELECT 'CREATE DATABASE notification_db OWNER notification_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'notification_db') \gexec

SELECT 'CREATE DATABASE routing_db OWNER routing_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'routing_db') \gexec

SELECT 'CREATE DATABASE user_db OWNER user_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'user_db') \gexec

SELECT 'CREATE DATABASE keycloak_db OWNER keycloak_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'keycloak_db') \gexec

REVOKE CONNECT ON DATABASE fleet_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE parcel_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE delivery_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE geography_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE notification_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE routing_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE user_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE keycloak_db FROM PUBLIC;

GRANT CONNECT ON DATABASE fleet_db TO fleet_user;
GRANT CONNECT ON DATABASE parcel_db TO parcel_user;
GRANT CONNECT ON DATABASE delivery_db TO delivery_user;
GRANT CONNECT ON DATABASE geography_db TO geography_user;
GRANT CONNECT ON DATABASE notification_db TO notification_user;
GRANT CONNECT ON DATABASE routing_db TO routing_user;
GRANT CONNECT ON DATABASE user_db TO user_user;
GRANT CONNECT ON DATABASE keycloak_db TO keycloak_user;

\connect fleet_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO fleet_user;
GRANT USAGE, CREATE ON SCHEMA public TO fleet_user;

\connect parcel_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO parcel_user;
GRANT USAGE, CREATE ON SCHEMA public TO parcel_user;

\connect delivery_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO delivery_user;
GRANT USAGE, CREATE ON SCHEMA public TO delivery_user;

\connect geography_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO geography_user;
GRANT USAGE, CREATE ON SCHEMA public TO geography_user;

\connect notification_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO notification_user;
GRANT USAGE, CREATE ON SCHEMA public TO notification_user;

\connect routing_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO routing_user;
GRANT USAGE, CREATE ON SCHEMA public TO routing_user;

\connect user_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO user_user;
GRANT USAGE, CREATE ON SCHEMA public TO user_user;

\connect keycloak_db
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO keycloak_user;
GRANT USAGE, CREATE ON SCHEMA public TO keycloak_user;

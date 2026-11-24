ALTER TABLE clients
    ADD CONSTRAINT ck_clients_email_format CHECK (position('@' in email) > 1);

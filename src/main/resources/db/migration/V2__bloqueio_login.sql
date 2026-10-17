ALTER TABLE usuarios
    ADD COLUMN tentativas_login_falhas INTEGER NOT NULL DEFAULT 0;

ALTER TABLE usuarios
    ADD COLUMN login_bloqueado BOOLEAN NOT NULL DEFAULT FALSE;

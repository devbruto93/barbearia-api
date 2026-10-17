ALTER TABLE usuarios
    ADD COLUMN login_bloqueado_ate TIMESTAMP WITH TIME ZONE;

UPDATE usuarios SET login_bloqueado_ate = CURRENT_TIMESTAMP
WHERE login_bloqueado = TRUE;

ALTER TABLE usuarios
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

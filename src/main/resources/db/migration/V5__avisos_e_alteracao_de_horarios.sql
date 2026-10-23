ALTER TABLE agendamentos
    ADD COLUMN duracao_minutos INTEGER NOT NULL DEFAULT 30;

ALTER TABLE agendamentos
    ADD COLUMN horario_original TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE agendamentos
    ADD COLUMN alteracao_horario_pendente BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE agendamentos
    ADD COLUMN remarcacao_solicitada BOOLEAN NOT NULL DEFAULT FALSE;

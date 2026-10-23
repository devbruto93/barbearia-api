CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM agendamentos a
        JOIN agendamentos b
          ON a.barbeiro_id = b.barbeiro_id
         AND a.id < b.id
         AND a.status = 'AGENDADO'
         AND b.status = 'AGENDADO'
         AND tsrange(
                 a.data_hora,
                 a.data_hora + a.duracao_minutos * INTERVAL '1 minute',
                 '[)'
             ) && tsrange(
                 b.data_hora,
                 b.data_hora + b.duracao_minutos * INTERVAL '1 minute',
                 '[)'
             )
    ) THEN
        RAISE EXCEPTION 'Existem agendamentos ativos sobrepostos; corrija-os antes de aplicar a restricao de agenda.';
    END IF;
END $$;

ALTER TABLE agendamentos
    ADD CONSTRAINT ck_agendamentos_duracao_positiva
        CHECK (duracao_minutos > 0),
    ADD CONSTRAINT ex_agendamentos_sem_sobreposicao
        EXCLUDE USING gist (
            barbeiro_id WITH =,
            tsrange(
                data_hora,
                data_hora + duracao_minutos * INTERVAL '1 minute',
                '[)'
            ) WITH &&
        )
        WHERE (status = 'AGENDADO')
        DEFERRABLE INITIALLY DEFERRED;

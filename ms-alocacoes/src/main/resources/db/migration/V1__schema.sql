-- Esquema do microsserviço de Alocações (Azure SQL / SQL Server, T-SQL).
-- O ativo vive no microsserviço de Ativos (MongoDB): aqui ele é referenciado pelo número
-- de patrimônio, sem chave estrangeira entre bancos (Database per Service).

CREATE TABLE colaborador (
    id        BIGINT IDENTITY(1, 1) NOT NULL,
    nome      NVARCHAR(120) NOT NULL,
    matricula NVARCHAR(20)  NOT NULL,
    setor     NVARCHAR(60)  NOT NULL,
    situacao  VARCHAR(10)   NOT NULL,
    CONSTRAINT pk_colaborador PRIMARY KEY (id),
    CONSTRAINT uk_colaborador_matricula UNIQUE (matricula),
    CONSTRAINT ck_colaborador_situacao CHECK (situacao IN ('ATIVO', 'INATIVO'))
);

CREATE TABLE alocacao (
    id                BIGINT IDENTITY(1, 1) NOT NULL,
    numero_patrimonio NVARCHAR(30)  NOT NULL,
    equipamento       NVARCHAR(200) NOT NULL,
    colaborador_id    BIGINT        NOT NULL,
    data_retirada     DATE          NOT NULL,
    data_devolucao    DATE          NULL,
    CONSTRAINT pk_alocacao PRIMARY KEY (id),
    CONSTRAINT fk_alocacao_colaborador FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    CONSTRAINT ck_alocacao_datas CHECK (data_devolucao IS NULL OR data_devolucao >= data_retirada)
);

-- Regra RF23 / CQ02: no máximo UMA alocação vigente (sem devolução) por ativo.
-- Índice único filtrado: o próprio banco recusa a segunda alocação, mesmo em requisições simultâneas.
CREATE UNIQUE INDEX uk_alocacao_vigente ON alocacao (numero_patrimonio) WHERE data_devolucao IS NULL;

CREATE INDEX idx_alocacao_colaborador ON alocacao (colaborador_id);

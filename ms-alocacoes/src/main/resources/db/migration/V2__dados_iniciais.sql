-- Dados de exemplo, coerentes com os ativos de exemplo do microsserviço de Ativos.
-- Os ids gerados seguem a ordem de inserção (1 a 8).

INSERT INTO colaborador (nome, matricula, setor, situacao) VALUES
    (N'Marina Alves',     N'0041287', N'Financeiro',       'ATIVO'),
    (N'Carlos Nunes',     N'0038114', N'Suporte',          'ATIVO'),
    (N'Priscila Rocha',   N'0052900', N'Comercial',        'ATIVO'),
    (N'Eduardo Lima',     N'0029045', N'TI',               'ATIVO'),
    (N'Renata Farias',    N'0060221', N'RH',               'ATIVO'),
    (N'Tiago Moraes',     N'0011903', N'Logística',        'INATIVO'),
    (N'Juliana Peres',    N'0058812', N'Recursos Humanos', 'ATIVO'),
    (N'Fernanda Ribeiro', N'0044210', N'Marketing',        'ATIVO');

-- Alocações vigentes (ativos "Em uso") e encerradas (histórico).
INSERT INTO alocacao (numero_patrimonio, equipamento, colaborador_id, data_retirada, data_devolucao) VALUES
    (N'PAT-0001', N'Dell Latitude 5440',   1, '2023-03-14', NULL),
    (N'PAT-0007', N'Samsung S27A400',      1, '2023-03-14', NULL),
    (N'PAT-0014', N'Dell WD19S',           1, '2023-03-14', NULL),
    (N'PAT-0004', N'Apple MacBook Air M2', 8, '2023-11-20', NULL),
    (N'PAT-0009', N'Logitech M170',        8, '2023-11-20', NULL),
    (N'PAT-0005', N'Logitech H390',        4, '2023-05-06', NULL),
    (N'PAT-0011', N'Dell KB216',           4, '2023-05-06', NULL),
    (N'PAT-0002', N'Lenovo ThinkPad E14',  4, '2022-08-02', '2026-06-14'),
    (N'PAT-0012', N'HP ProBook 440',       7, '2022-06-30', '2026-04-10');

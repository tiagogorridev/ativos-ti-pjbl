const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const { calcularDepreciacao, mesesDecorridos } = require("../src/dominio/depreciacao");
const { lerEvento, registroDeAuditoria } = require("../src/dominio/auditoria");

const HOJE = new Date("2026-11-05T15:00:00Z");

describe("depreciação linear", () => {
  it("conta só meses completos", () => {
    assert.equal(mesesDecorridos("2024-01-10", HOJE), 33);
    assert.equal(mesesDecorridos("2026-11-06", HOJE), 0);
  });

  it("deprecia proporcionalmente aos meses de uso", () => {
    const resultado = calcularDepreciacao(
      [{ numeroPatrimonio: "PAT-0003", status: "Em estoque", dataAquisicao: "2024-01-10", valorAquisicao: 899 }],
      HOJE,
    );
    const item = resultado.itens[0];
    // 33 de 60 meses = 55%
    assert.equal(item.percentualDepreciado, 55);
    assert.equal(item.depreciacaoAcumulada, 494.45);
    assert.equal(item.valorAtual, 404.55);
  });

  it("ativo com mais de 5 anos ou baixado vale zero", () => {
    const resultado = calcularDepreciacao(
      [
        { numeroPatrimonio: "PAT-0006", status: "Em manutenção", dataAquisicao: "2019-02-25", valorAquisicao: 1450 },
        { numeroPatrimonio: "PAT-0008", status: "Baixado", dataAquisicao: "2025-01-01", valorAquisicao: 4200 },
      ],
      HOJE,
    );
    assert.deepEqual(resultado.itens.map((i) => i.valorAtual), [0, 0]);
    assert.equal(resultado.depreciacaoTotal, 5650);
    assert.equal(resultado.valorAtualTotal, 0);
  });

  it("recusa entrada sem lista ou com data inválida", () => {
    assert.throws(() => calcularDepreciacao(undefined, HOJE), /ativos/);
    assert.throws(
      () => calcularDepreciacao([{ numeroPatrimonio: "X", dataAquisicao: "ontem", valorAquisicao: 10 }], HOJE),
      /Data de aquisição inválida/,
    );
  });
});

describe("auditoria dos eventos", () => {
  const evento = {
    tipo: "AtivoAlocado",
    alocacaoId: 10,
    numeroPatrimonio: "PAT-0003",
    equipamento: "LG 24MK430H",
    colaborador: "Carlos Nunes",
    data: "2026-11-05",
    ocorridoEm: "2026-11-05T15:00:00.000Z",
  };

  it("lê o evento como objeto ou como texto JSON", () => {
    assert.equal(lerEvento(evento).numeroPatrimonio, "PAT-0003");
    assert.equal(lerEvento(JSON.stringify(evento)).colaborador, "Carlos Nunes");
  });

  it("monta a mensagem de auditoria", () => {
    const registro = registroDeAuditoria(evento, "msg-1");
    assert.equal(registro.mensagem, "Auditoria: ativo PAT-0003 (LG 24MK430H) alocado para Carlos Nunes em 2026-11-05.");
    assert.equal(registro.idDaMensagem, "msg-1");
  });

  it("recusa evento incompleto", () => {
    assert.throws(() => lerEvento({ tipo: "AtivoAlocado" }), /numeroPatrimonio/);
  });
});

import { describe, expect, it } from "vitest";
import { AlocarAtivo } from "../../src/application/alocacoes/AlocarAtivo.js";
import { DevolverAtivo } from "../../src/application/alocacoes/DevolverAtivo.js";
import { ObterDadosAgregados } from "../../src/application/dados-agregados/ObterDadosAgregados.js";
import { ObterDashboard } from "../../src/application/dashboard/ObterDashboard.js";
import { EncaminharRequisicao } from "../../src/application/proxy/EncaminharRequisicao.js";
import { ErroDeServico } from "../../src/domain/erros.js";
import { movimentacoesRecentes } from "../../src/domain/movimentacoes.js";
import {
  AGORA,
  PublicadorFalso,
  RegistroSilencioso,
  ServicoFalso,
  alocacaoCriada,
  ativoEmEstoque,
  indicadores,
} from "../apoio/ServicoFalso.js";

const depreciacao = { calculadoEm: "2026-11-05", valorAquisicaoTotal: 899, valorAtualTotal: 700, depreciacaoTotal: 199, itens: [] };

function servicosCompletos() {
  const ativos = new ServicoFalso("Ativos")
    .responder("GET", "/api/ativos", 200, [ativoEmEstoque])
    .responder("GET", "/api/indicadores", 200, indicadores);
  const alocacoes = new ServicoFalso("Alocações")
    .responder("GET", "/api/alocacoes?vigentes=true", 200, [alocacaoCriada])
    .responder("GET", "/api/colaboradores", 200, [{ id: 2, nome: "Carlos Nunes" }]);
  const calculo = new ServicoFalso("Cálculo").responder("POST", "/depreciacao", 200, depreciacao);
  return { ativos, alocacoes, calculo };
}

describe("ObterDadosAgregados (GET /aggregated-data)", () => {
  it("junta os dois microsserviços e a Function em uma resposta", async () => {
    const { ativos, alocacoes, calculo } = servicosCompletos();

    const dados = await new ObterDadosAgregados(ativos, alocacoes, calculo, AGORA).executar();

    expect(dados.geradoEm).toBe("2026-11-05T15:00:00.000Z");
    expect(dados.ativos?.itens).toHaveLength(1);
    expect(dados.ativos?.indicadores.totalAtivos).toBe(14);
    expect(dados.alocacoes?.vigentes[0]?.colaborador).toBe("Carlos Nunes");
    expect(dados.depreciacao?.depreciacaoTotal).toBe(199);
    expect(dados.fontes.microservicoAtivos.status).toBe("ok");
    expect(dados.fontes.microservicoAlocacoes.status).toBe("ok");
    expect(dados.fontes.azureFunction.status).toBe("ok");
    expect(calculo.chamadas[0]?.corpo).toEqual({
      ativos: [{ numeroPatrimonio: "PAT-0003", tipo: "Monitor", status: "Em estoque", dataAquisicao: "2024-01-10", valorAquisicao: 899 }],
    });
  });

  it("continua respondendo quando a Function está fora do ar", async () => {
    const { ativos, alocacoes } = servicosCompletos();
    const calculo = new ServicoFalso("Cálculo").foraDoAr("POST", "/depreciacao");

    const dados = await new ObterDadosAgregados(ativos, alocacoes, calculo, AGORA).executar();

    expect(dados.depreciacao).toBeNull();
    expect(dados.fontes.azureFunction.status).toBe("indisponivel");
    expect(dados.ativos).not.toBeNull();
  });

  it("indica qual microsserviço faltou sem perder o outro", async () => {
    const { ativos, calculo } = servicosCompletos();
    const alocacoes = new ServicoFalso("Alocações").foraDoAr("GET", "/api/alocacoes?vigentes=true");

    const dados = await new ObterDadosAgregados(ativos, alocacoes, calculo, AGORA).executar();

    expect(dados.alocacoes).toBeNull();
    expect(dados.fontes.microservicoAlocacoes.status).toBe("indisponivel");
    expect(dados.fontes.microservicoAtivos.status).toBe("ok");
  });
});

describe("AlocarAtivo", () => {
  function cenario() {
    const ativos = new ServicoFalso("Ativos")
      .responder("GET", "/api/ativos/PAT-0003", 200, ativoEmEstoque)
      .responder("PUT", "/api/ativos/PAT-0003/uso", 200, { ...ativoEmEstoque, status: "Em uso" });
    const alocacoes = new ServicoFalso("Alocações")
      .responder("POST", "/api/alocacoes", 201, alocacaoCriada)
      .responder("DELETE", "/api/alocacoes/10", 200, { mensagem: "ok" });
    const eventos = new PublicadorFalso();
    const registro = new RegistroSilencioso();
    return { ativos, alocacoes, eventos, registro, alocar: new AlocarAtivo(ativos, alocacoes, eventos, registro, AGORA) };
  }

  it("grava a alocação, marca o ativo em uso e publica AtivoAlocado", async () => {
    const { ativos, alocacoes, eventos, alocar } = cenario();

    const alocacao = await alocar.executar({ numeroPatrimonio: "PAT-0003", colaboradorId: 2 });

    expect(alocacao.id).toBe(10);
    expect(alocacoes.chamadas[0]?.corpo).toMatchObject({ numeroPatrimonio: "PAT-0003", equipamento: "LG 24MK430H", colaboradorId: 2 });
    expect(ativos.chamadas.at(-1)).toMatchObject({ metodo: "PUT", corpo: { colaborador: "Carlos Nunes", setor: "Suporte" } });
    expect(eventos.publicados).toEqual([
      {
        tipo: "AtivoAlocado",
        alocacaoId: 10,
        numeroPatrimonio: "PAT-0003",
        equipamento: "LG 24MK430H",
        colaborador: "Carlos Nunes",
        data: "2026-11-05",
        ocorridoEm: "2026-11-05T15:00:00.000Z",
      },
    ]);
  });

  it("recusa ativo que não está em estoque sem gravar nada", async () => {
    const { ativos, alocacoes, alocar } = cenario();
    ativos.responder("GET", "/api/ativos/PAT-0003", 200, { ...ativoEmEstoque, status: "Em manutenção" });

    await expect(alocar.executar({ numeroPatrimonio: "PAT-0003", colaboradorId: 2 })).rejects.toMatchObject({ status: 409 });
    expect(alocacoes.chamadas).toHaveLength(0);
  });

  it("repassa o 409 do microsserviço quando o ativo já tem alocação vigente", async () => {
    const { alocacoes, eventos, alocar } = cenario();
    alocacoes.responder("POST", "/api/alocacoes", 409, { error: "O ativo PAT-0003 já está alocado para Marina Alves." });

    await expect(alocar.executar({ numeroPatrimonio: "PAT-0003", colaboradorId: 2 })).rejects.toMatchObject({
      status: 409,
      message: "O ativo PAT-0003 já está alocado para Marina Alves.",
    });
    expect(eventos.publicados).toHaveLength(0);
  });

  it("desfaz a alocação se o serviço de Ativos não confirmar o uso", async () => {
    const { ativos, alocacoes, eventos, alocar } = cenario();
    ativos.foraDoAr("PUT", "/api/ativos/PAT-0003/uso");

    await expect(alocar.executar({ numeroPatrimonio: "PAT-0003", colaboradorId: 2 })).rejects.toBeInstanceOf(ErroDeServico);
    expect(alocacoes.chamou("DELETE", "/api/alocacoes/10")).toBe(true);
    expect(eventos.publicados).toHaveLength(0);
  });

  it("não desfaz a alocação se só a publicação do evento falhar", async () => {
    const { alocacoes, eventos, registro, alocar } = cenario();
    eventos.falhar = true;

    const alocacao = await alocar.executar({ numeroPatrimonio: "PAT-0003", colaboradorId: 2 });

    expect(alocacao.id).toBe(10);
    expect(alocacoes.chamou("DELETE", "/api/alocacoes/10")).toBe(false);
    expect(registro.avisos).toHaveLength(1);
  });

  it("exige o número de patrimônio", async () => {
    const { alocar } = cenario();
    await expect(alocar.executar({ colaboradorId: 2 })).rejects.toMatchObject({ status: 400 });
  });
});

describe("DevolverAtivo", () => {
  it("encerra a alocação, devolve o ativo ao estoque e publica AtivoDevolvido", async () => {
    const devolvida = { ...alocacaoCriada, dataDevolucao: "2026-11-06", vigente: false };
    const ativos = new ServicoFalso("Ativos").responder("DELETE", "/api/ativos/PAT-0003/uso", 200, ativoEmEstoque);
    const alocacoes = new ServicoFalso("Alocações").responder("POST", "/api/alocacoes/10/devolucao", 200, devolvida);
    const eventos = new PublicadorFalso();

    const resultado = await new DevolverAtivo(ativos, alocacoes, eventos, new RegistroSilencioso(), AGORA).executar("10");

    expect(resultado.vigente).toBe(false);
    expect(ativos.chamou("DELETE", "/api/ativos/PAT-0003/uso")).toBe(true);
    expect(eventos.publicados[0]).toMatchObject({ tipo: "AtivoDevolvido", data: "2026-11-06" });
  });
});

describe("EncaminharRequisicao (proxy do CRUD)", () => {
  it("manda cada recurso para o microsserviço dono", async () => {
    const ativos = new ServicoFalso("Ativos").responder("GET", "/api/manutencoes?numeroPatrimonio=PAT-0002", 200, []);
    const alocacoes = new ServicoFalso("Alocações").responder("PUT", "/api/colaboradores/1", 200, { id: 1 });
    const proxy = new EncaminharRequisicao(ativos, alocacoes);

    await proxy.executar("GET", "/api/manutencoes?numeroPatrimonio=PAT-0002");
    await proxy.executar("PUT", "/api/colaboradores/1", { setor: "TI" });

    expect(ativos.chamadas).toHaveLength(1);
    expect(alocacoes.chamadas[0]).toEqual({ metodo: "PUT", caminho: "/api/colaboradores/1", corpo: { setor: "TI" } });
  });

  it("não envia corpo em GET e DELETE e recusa rota desconhecida", async () => {
    const ativos = new ServicoFalso("Ativos").responder("DELETE", "/api/ativos/PAT-0001", 200, {});
    const proxy = new EncaminharRequisicao(ativos, new ServicoFalso("Alocações"));

    await proxy.executar("DELETE", "/api/ativos/PAT-0001", { qualquer: 1 });

    expect(ativos.chamadas[0]?.corpo).toBeUndefined();
    await expect(proxy.executar("GET", "/api/usuarios")).rejects.toMatchObject({ status: 404 });
  });

  it("não expõe a rota interna /uso, que só a alocação e a devolução usam", async () => {
    const ativos = new ServicoFalso("Ativos");
    const proxy = new EncaminharRequisicao(ativos, new ServicoFalso("Alocações"));

    await expect(proxy.executar("PUT", "/api/ativos/PAT-0003/uso", { colaborador: "X" })).rejects.toMatchObject({ status: 404 });
    await expect(proxy.executar("DELETE", "/api/ativos/PAT-0001/uso")).rejects.toMatchObject({ status: 404 });
    expect(ativos.chamadas).toHaveLength(0);
  });
});

describe("Dashboard e movimentações", () => {
  it("junta indicadores e as movimentações mais recentes", async () => {
    const ativos = new ServicoFalso("Ativos").responder("GET", "/api/indicadores", 200, indicadores);
    const alocacoes = new ServicoFalso("Alocações").responder("GET", "/api/alocacoes", 200, [
      { ...alocacaoCriada, id: 1, dataRetirada: "2026-01-10", dataDevolucao: "2026-03-01", vigente: false },
      alocacaoCriada,
    ]);

    const dashboard = await new ObterDashboard(ativos, alocacoes).executar();

    expect(dashboard.totalAtivos).toBe(14);
    expect(dashboard.ultimasMovimentacoes.map((m) => [m.data, m.operacao])).toEqual([
      ["2026-11-05", "Alocação"],
      ["2026-03-01", "Devolução"],
      ["2026-01-10", "Alocação"],
    ]);
  });

  it("limita a quantidade de movimentações", () => {
    const muitas = Array.from({ length: 10 }, (_, i) => ({ ...alocacaoCriada, id: i + 1, dataRetirada: `2026-01-${String(i + 1).padStart(2, "0")}` }));
    expect(movimentacoesRecentes(muitas, 5)).toHaveLength(5);
  });
});

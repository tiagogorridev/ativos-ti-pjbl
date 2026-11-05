// Ponto de entrada (composition root): cria os adaptadores e entrega aos casos de uso.
import { criarApp } from "./api/app.js";
import { AlocarAtivo } from "./application/alocacoes/AlocarAtivo.js";
import { DevolverAtivo } from "./application/alocacoes/DevolverAtivo.js";
import { ObterDadosAgregados } from "./application/dados-agregados/ObterDadosAgregados.js";
import { ObterDashboard } from "./application/dashboard/ObterDashboard.js";
import { EncaminharRequisicao } from "./application/proxy/EncaminharRequisicao.js";
import type { PublicadorDeEventos } from "./domain/portas.js";
import { lerConfiguracao } from "./infrastructure/configuracao.js";
import { PublicadorEmLog, PublicadorServiceBus } from "./infrastructure/eventos/PublicadorServiceBus.js";
import { ServicoHttpFetch } from "./infrastructure/http/ServicoHttpFetch.js";
import { RegistroConsole } from "./infrastructure/RegistroConsole.js";

const config = lerConfiguracao();
const registro = new RegistroConsole();

const ativos = new ServicoHttpFetch("Ativos", config.ativosUrl, config.timeoutMs);
const alocacoes = new ServicoHttpFetch("Alocações", config.alocacoesUrl, config.timeoutMs);
const calculo = new ServicoHttpFetch(
  "Cálculo (Azure Function)",
  config.functionUrl,
  config.timeoutMs,
  config.functionKey ? { "x-functions-key": config.functionKey } : {},
);

const eventos: PublicadorDeEventos = config.serviceBusConexao
  ? new PublicadorServiceBus(config.serviceBusConexao, config.serviceBusFila)
  : new PublicadorEmLog(registro);

const app = criarApp({
  obterDadosAgregados: new ObterDadosAgregados(ativos, alocacoes, calculo),
  obterDashboard: new ObterDashboard(ativos, alocacoes),
  alocarAtivo: new AlocarAtivo(ativos, alocacoes, eventos, registro),
  devolverAtivo: new DevolverAtivo(ativos, alocacoes, eventos, registro),
  encaminharRequisicao: new EncaminharRequisicao(ativos, alocacoes),
});

app.listen(config.porta, () => {
  registro.info(`BFF ouvindo na porta ${config.porta}. Swagger em /swagger`);
});

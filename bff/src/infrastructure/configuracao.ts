/** Configuração lida das variáveis de ambiente (endereços e segredos nunca ficam no código). */
export interface Configuracao {
  porta: number;
  ativosUrl: string;
  alocacoesUrl: string;
  functionUrl: string;
  functionKey?: string;
  serviceBusConexao?: string;
  serviceBusFila: string;
  timeoutMs: number;
}

export function lerConfiguracao(env: NodeJS.ProcessEnv = process.env): Configuracao {
  return {
    porta: Number(env.PORT ?? 8084),
    ativosUrl: env.ATIVOS_URL ?? "http://localhost:8082",
    alocacoesUrl: env.ALOCACOES_URL ?? "http://localhost:8083",
    functionUrl: env.FUNCTION_URL ?? "http://localhost:7071/api",
    functionKey: env.FUNCTION_KEY || undefined,
    serviceBusConexao: env.SERVICEBUS_CONNECTION_STRING || undefined,
    serviceBusFila: env.SERVICEBUS_FILA ?? "fila-alocacoes",
    // Os serviços desligam quando ociosos para não gerar custo; a primeira chamada pode demorar.
    timeoutMs: Number(env.TIMEOUT_MS ?? 90000),
  };
}

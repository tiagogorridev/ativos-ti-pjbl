// Contratos de dados trocados com os microsserviços e com o frontend.
// O BFF não guarda dados: ele junta e repassa o que os serviços devolvem.

export interface Ativo {
  id: number;
  numeroPatrimonio: string;
  tipo: string;
  marca: string;
  modelo: string;
  numeroSerie: string;
  status: string;
  colaborador: string | null;
  setor: string | null;
  dataAquisicao: string;
  valorAquisicao: number;
  equipamento?: string;
}

export interface Alocacao {
  id: number;
  numeroPatrimonio: string;
  equipamento: string;
  colaboradorId: number;
  colaborador: string;
  setor: string;
  dataRetirada: string;
  dataDevolucao: string | null;
  vigente: boolean;
}

export interface Colaborador {
  id: number;
  nome: string;
  matricula: string;
  setor: string;
  situacao: string;
  ativosAlocados: number;
}

export interface Indicadores {
  totalAtivos: number;
  emUso: number;
  emEstoque: number;
  emManutencao: number;
  baixados: number;
  custoManutencaoAcumulado: number;
  ativosPorSetor: { setor: string; quantidade: number }[];
  custoManutencaoPorMes: { mes: string; valor: number }[];
}

/** Resposta da Azure Function de cálculo (depreciação linear dos ativos). */
export interface Depreciacao {
  calculadoEm: string;
  valorAquisicaoTotal: number;
  valorAtualTotal: number;
  depreciacaoTotal: number;
  itens: {
    numeroPatrimonio: string;
    valorAquisicao: number;
    valorAtual: number;
    depreciacaoAcumulada: number;
    percentualDepreciado: number;
    vidaUtilAnos: number;
  }[];
}

/** Movimentação no formato que o dashboard do frontend já usa. */
export interface Movimentacao {
  id: number;
  data: string;
  numeroPatrimonio: string;
  ativo: string;
  operacao: string;
  colaborador: string | null;
  registradoPor: string;
}

/** Evento publicado na fila do Azure Service Bus. */
export interface EventoDeAlocacao {
  tipo: "AtivoAlocado" | "AtivoDevolvido";
  alocacaoId: number;
  numeroPatrimonio: string;
  equipamento: string;
  colaborador: string;
  data: string;
  ocorridoEm: string;
}

export type StatusAtivo = "Em estoque" | "Em uso" | "Em manutenção" | "Baixado";
export type SituacaoPessoa = "Ativo" | "Inativo";
export type TipoManutencao = "Corretiva" | "Preventiva";

export interface Ativo {
  id: number;
  tipo: string;
  marca: string;
  modelo: string;
  numeroPatrimonio: string;
  numeroSerie: string;
  status: StatusAtivo;
  colaborador: string | null;
  setor: string | null;
  dataAquisicao: string;
  valorAquisicao: number;
}

/** Payload para POST /api/ativos — o "id" é atribuído pelo backend. */
export type NovoAtivo = Omit<Ativo, "id">;

/** Payload para PUT /api/ativos/{numeroPatrimonio} — patrimônio e id não são editáveis por aqui. */
export type AtivoEditavel = Partial<Omit<Ativo, "id" | "numeroPatrimonio">>;

export interface Colaborador {
  id: number;
  nome: string;
  matricula: string;
  setor: string;
  situacao: SituacaoPessoa;
  ativosAlocados: number;
}

export interface Alocacao {
  id: number;
  numeroPatrimonio: string;
  equipamento: string;
  colaborador: string;
  setor: string;
  dataRetirada: string;
}

export interface Manutencao {
  id: number;
  data: string;
  numeroPatrimonio: string;
  equipamento: string;
  tipo: TipoManutencao;
  descricao: string;
  custo: number;
}


export interface Movimentacao {
  id: number;
  data: string;
  numeroPatrimonio: string;
  ativo: string;
  operacao: string;
  colaborador: string | null;
  registradoPor: string;
}

export interface AtivosPorSetor {
  setor: string;
  quantidade: number;
}

export interface CustoPorMes {
  mes: string;
  valor: number;
}

export interface DashboardData {
  totalAtivos: number;
  emUso: number;
  emEstoque: number;
  emManutencao: number;
  baixados: number;
  custoManutencaoAcumulado: number;
  ativosPorSetor: AtivosPorSetor[];
  custoManutencaoPorMes: CustoPorMes[];
  ultimasMovimentacoes: Movimentacao[];
}

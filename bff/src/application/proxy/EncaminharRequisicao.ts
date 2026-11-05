import { ErroDeServico } from "../../domain/erros.js";
import type { RespostaHttp, ServicoHttp } from "../../domain/portas.js";

const ROTA_INTERNA = /^\/api\/ativos\/[^/]+\/uso\/?$/;

/**
 * Caso de uso do proxy de CRUD: o frontend só conhece o BFF, e o BFF repassa cada rota ao
 * microsserviço dono do recurso, devolvendo o mesmo status e o mesmo corpo.
 */
export class EncaminharRequisicao {
  private readonly rotas: { prefixo: string; servico: ServicoHttp }[];

  constructor(ativos: ServicoHttp, alocacoes: ServicoHttp) {
    this.rotas = [
      { prefixo: "/api/ativos", servico: ativos },
      { prefixo: "/api/manutencoes", servico: ativos },
      { prefixo: "/api/indicadores", servico: ativos },
      { prefixo: "/api/colaboradores", servico: alocacoes },
      { prefixo: "/api/alocacoes", servico: alocacoes },
    ];
  }

  async executar(metodo: string, caminhoComQuery: string, corpo?: unknown): Promise<RespostaHttp> {
    const caminho = caminhoComQuery.split("?")[0] ?? "";
    const rota = this.rotas.find((r) => caminho === r.prefixo || caminho.startsWith(`${r.prefixo}/`));
    // "/uso" é interno: só o próprio BFF marca o ativo em uso, dentro da alocação e da devolução.
    if (!rota || ROTA_INTERNA.test(caminho)) {
      throw new ErroDeServico(404, `Rota ${caminho} não encontrada no BFF.`);
    }
    const temCorpo = !["GET", "HEAD", "DELETE"].includes(metodo.toUpperCase());
    return rota.servico.requisitar(metodo, caminhoComQuery, temCorpo ? corpo : undefined);
  }
}

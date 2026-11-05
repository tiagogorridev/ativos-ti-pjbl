import { exigirSucesso } from "../../domain/erros.js";
import type { PublicadorDeEventos, Registro, ServicoHttp } from "../../domain/portas.js";
import type { Alocacao } from "../../domain/tipos.js";

/**
 * Caso de uso: registrar a devolução, orquestrando os dois microsserviços.
 * 1. Encerra a alocação no serviço de Alocações.
 * 2. Devolve o ativo ao estoque no serviço de Ativos.
 * 3. Publica o evento AtivoDevolvido na fila do Service Bus.
 */
export class DevolverAtivo {
  constructor(
    private readonly ativos: ServicoHttp,
    private readonly alocacoes: ServicoHttp,
    private readonly eventos: PublicadorDeEventos,
    private readonly registro: Registro,
    private readonly agora: () => Date = () => new Date(),
  ) {}

  async executar(alocacaoId: string, dataDevolucao?: string): Promise<Alocacao> {
    const alocacao = exigirSucesso<Alocacao>(
      await this.alocacoes.requisitar("POST", `/api/alocacoes/${encodeURIComponent(alocacaoId)}/devolucao`, {
        dataDevolucao,
      }),
      this.alocacoes.nome,
    );

    const resposta = await this.ativos
      .requisitar("DELETE", `/api/ativos/${encodeURIComponent(alocacao.numeroPatrimonio)}/uso`)
      .catch((erro: unknown) => ({ status: 503, corpo: erro }));
    if (resposta.status >= 300) {
      this.registro.aviso(
        `Devolução registrada, mas o ativo ${alocacao.numeroPatrimonio} não voltou para o estoque no serviço de Ativos`,
        resposta.corpo,
      );
    }

    try {
      await this.eventos.publicar({
        tipo: "AtivoDevolvido",
        alocacaoId: alocacao.id,
        numeroPatrimonio: alocacao.numeroPatrimonio,
        equipamento: alocacao.equipamento,
        colaborador: alocacao.colaborador,
        data: alocacao.dataDevolucao ?? alocacao.dataRetirada,
        ocorridoEm: this.agora().toISOString(),
      });
    } catch (erro) {
      this.registro.aviso("Não foi possível publicar o evento AtivoDevolvido", erro);
    }
    return alocacao;
  }
}

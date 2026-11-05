import { ErroDeServico, exigirSucesso } from "../../domain/erros.js";
import type { PublicadorDeEventos, Registro, ServicoHttp } from "../../domain/portas.js";
import type { Alocacao, Ativo } from "../../domain/tipos.js";

export interface AlocarAtivoEntrada {
  numeroPatrimonio?: string;
  colaboradorId?: number;
  dataRetirada?: string;
}

/**
 * Caso de uso: alocar um ativo, orquestrando os dois microsserviços.
 * 1. Confere no serviço de Ativos se o ativo existe e está "Em estoque".
 * 2. Grava a alocação no serviço de Alocações (que recusa alocação duplicada).
 * 3. Marca o ativo como "Em uso" no serviço de Ativos; se falhar, desfaz a alocação.
 * 4. Publica o evento AtivoAlocado na fila do Service Bus.
 */
export class AlocarAtivo {
  constructor(
    private readonly ativos: ServicoHttp,
    private readonly alocacoes: ServicoHttp,
    private readonly eventos: PublicadorDeEventos,
    private readonly registro: Registro,
    private readonly agora: () => Date = () => new Date(),
  ) {}

  async executar(entrada: AlocarAtivoEntrada): Promise<Alocacao> {
    const patrimonio = entrada.numeroPatrimonio?.trim();
    if (!patrimonio) {
      throw new ErroDeServico(400, "Informe o campo 'numeroPatrimonio'.", { error: "Informe o campo 'numeroPatrimonio'.", campo: "numeroPatrimonio" });
    }

    const ativo = exigirSucesso<Ativo>(
      await this.ativos.requisitar("GET", `/api/ativos/${encodeURIComponent(patrimonio)}`),
      this.ativos.nome,
    );
    if (ativo.status !== "Em estoque") {
      throw new ErroDeServico(409, `O ativo ${patrimonio} está '${ativo.status}'. Só ativos 'Em estoque' podem ser alocados.`);
    }

    const alocacao = exigirSucesso<Alocacao>(
      await this.alocacoes.requisitar("POST", "/api/alocacoes", {
        numeroPatrimonio: patrimonio,
        equipamento: ativo.equipamento ?? `${ativo.marca} ${ativo.modelo}`.trim(),
        colaboradorId: entrada.colaboradorId,
        dataRetirada: entrada.dataRetirada,
      }),
      this.alocacoes.nome,
    );

    try {
      exigirSucesso(
        await this.ativos.requisitar("PUT", `/api/ativos/${encodeURIComponent(patrimonio)}/uso`, {
          colaborador: alocacao.colaborador,
          setor: alocacao.setor,
        }),
        this.ativos.nome,
      );
    } catch (erro) {
      // Compensação: sem o ativo marcado como "Em uso", a alocação não pode ficar gravada.
      await this.alocacoes.requisitar("DELETE", `/api/alocacoes/${alocacao.id}`).catch(() => undefined);
      throw erro;
    }

    await this.publicar(alocacao);
    return alocacao;
  }

  private async publicar(alocacao: Alocacao): Promise<void> {
    try {
      await this.eventos.publicar({
        tipo: "AtivoAlocado",
        alocacaoId: alocacao.id,
        numeroPatrimonio: alocacao.numeroPatrimonio,
        equipamento: alocacao.equipamento,
        colaborador: alocacao.colaborador,
        data: alocacao.dataRetirada,
        ocorridoEm: this.agora().toISOString(),
      });
    } catch (erro) {
      // O evento é para auditoria: a alocação já está gravada e não é desfeita por isso.
      this.registro.aviso("Não foi possível publicar o evento AtivoAlocado", erro);
    }
  }
}

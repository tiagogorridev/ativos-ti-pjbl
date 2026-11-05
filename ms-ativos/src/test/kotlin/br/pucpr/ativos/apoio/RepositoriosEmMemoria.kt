package br.pucpr.ativos.apoio

import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.FiltroDeAtivos
import br.pucpr.ativos.domain.ativo.StatusAtivo
import br.pucpr.ativos.domain.manutencao.Manutencao
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.manutencao.TipoManutencao
import java.math.BigDecimal
import java.time.LocalDate

/** Implementações em memória das portas: testam os casos de uso sem MongoDB (DIP). */
class AtivoRepositoryEmMemoria(vararg iniciais: Ativo) : AtivoRepository {

    val dados = linkedMapOf<String, Ativo>().apply { iniciais.forEach { put(it.numeroPatrimonio, it) } }
    private var ultimoId = iniciais.maxOfOrNull { it.id } ?: 0L

    override fun listar(filtro: FiltroDeAtivos): List<Ativo> =
        dados.values
            .filter { filtro.status == null || it.status == filtro.status }
            .filter { ativo ->
                val termo = filtro.busca ?: return@filter true
                listOf(ativo.numeroPatrimonio, ativo.numeroSerie, ativo.tipo, ativo.marca, ativo.modelo, ativo.colaborador, ativo.setor)
                    .any { it?.contains(termo, ignoreCase = true) == true }
            }
            .sortedBy { it.id }

    override fun buscarPorPatrimonio(numeroPatrimonio: String): Ativo? = dados[numeroPatrimonio]

    override fun existePatrimonio(numeroPatrimonio: String): Boolean = dados.containsKey(numeroPatrimonio)

    override fun proximoId(): Long = ++ultimoId

    override fun inserir(ativo: Ativo) {
        dados[ativo.numeroPatrimonio] = ativo
    }

    override fun atualizar(ativo: Ativo) {
        dados[ativo.numeroPatrimonio] = ativo
    }

    override fun excluir(numeroPatrimonio: String) {
        dados.remove(numeroPatrimonio)
    }
}

class ManutencaoRepositoryEmMemoria(vararg iniciais: Manutencao) : ManutencaoRepository {

    val dados = linkedMapOf<Long, Manutencao>().apply { iniciais.forEach { put(it.id, it) } }
    private var ultimoId = iniciais.maxOfOrNull { it.id } ?: 0L

    override fun listar(numeroPatrimonio: String?): List<Manutencao> =
        dados.values
            .filter { numeroPatrimonio == null || it.numeroPatrimonio == numeroPatrimonio }
            .sortedWith(compareByDescending<Manutencao> { it.data }.thenByDescending { it.id })

    override fun buscarPorId(id: Long): Manutencao? = dados[id]

    override fun existeParaAtivo(numeroPatrimonio: String): Boolean =
        dados.values.any { it.numeroPatrimonio == numeroPatrimonio }

    override fun proximoId(): Long = ++ultimoId

    override fun inserir(manutencao: Manutencao) {
        dados[manutencao.id] = manutencao
    }

    override fun atualizar(manutencao: Manutencao) {
        dados[manutencao.id] = manutencao
    }

    override fun excluir(id: Long) {
        dados.remove(id)
    }
}

fun umAtivo(
    numeroPatrimonio: String = "PAT-0001",
    status: StatusAtivo = StatusAtivo.EM_ESTOQUE,
    id: Long = 1,
    colaborador: String? = if (status == StatusAtivo.EM_USO) "Marina Alves" else null,
    setor: String? = if (status == StatusAtivo.EM_USO) "Financeiro" else null,
) = Ativo(
    id = id,
    numeroPatrimonio = numeroPatrimonio,
    tipo = "Notebook",
    marca = "Dell",
    modelo = "Latitude 5440",
    numeroSerie = "8KJ2H31",
    status = status,
    colaborador = colaborador,
    setor = setor,
    dataAquisicao = LocalDate.of(2023, 3, 14),
    valorAquisicao = BigDecimal("6890.00"),
)

fun umaManutencao(
    id: Long = 1,
    numeroPatrimonio: String = "PAT-0001",
    data: LocalDate = LocalDate.of(2026, 8, 22),
    custo: String = "130.00",
) = Manutencao(
    id = id,
    numeroPatrimonio = numeroPatrimonio,
    equipamento = "Dell Latitude 5440",
    data = data,
    tipo = TipoManutencao.CORRETIVA,
    descricao = "Troca de bateria",
    custo = BigDecimal(custo),
)

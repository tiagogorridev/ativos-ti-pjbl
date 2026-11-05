package br.pucpr.alocacoes.apoio

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import br.pucpr.alocacoes.domain.colaborador.Situacao
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Implementações em memória das portas: testam os casos de uso sem banco (DIP). */
class ColaboradorRepositoryEmMemoria(vararg iniciais: Colaborador) : ColaboradorRepository {

    val dados = linkedMapOf<Long, Colaborador>().apply { iniciais.forEach { put(it.id, it) } }
    private var ultimoId = iniciais.maxOfOrNull { it.id } ?: 0L

    override fun listar(): List<Colaborador> = dados.values.sortedBy { it.nome }

    override fun buscarPorId(id: Long): Colaborador? = dados[id]

    override fun idsComMatricula(matricula: String): List<Long> =
        dados.values.filter { it.matricula == matricula }.map { it.id }

    override fun inserir(colaborador: Colaborador): Long {
        val id = ++ultimoId
        dados[id] = colaborador.copy(id = id)
        return id
    }

    override fun atualizar(colaborador: Colaborador) {
        dados[colaborador.id] = colaborador
    }
}

/** Imita o índice único do banco: recusa a segunda alocação vigente do mesmo patrimônio. */
class AlocacaoRepositoryEmMemoria(vararg iniciais: Alocacao) : AlocacaoRepository {

    val dados = linkedMapOf<Long, Alocacao>().apply { iniciais.forEach { put(it.id, it) } }
    private var ultimoId = iniciais.maxOfOrNull { it.id } ?: 0L

    override fun listar(vigentes: Boolean?, colaboradorId: Long?): List<Alocacao> =
        dados.values
            .filter { vigentes == null || it.vigente == vigentes }
            .filter { colaboradorId == null || it.colaboradorId == colaboradorId }
            .sortedWith(compareByDescending<Alocacao> { it.dataRetirada }.thenByDescending { it.id })

    override fun buscarPorId(id: Long): Alocacao? = dados[id]

    override fun buscarVigentePorPatrimonio(numeroPatrimonio: String): Alocacao? =
        dados.values.firstOrNull { it.numeroPatrimonio == numeroPatrimonio && it.vigente }

    override fun contarVigentesDoColaborador(colaboradorId: Long): Int =
        dados.values.count { it.colaboradorId == colaboradorId && it.vigente }

    override fun contarVigentesPorColaborador(): Map<Long, Int> =
        dados.values.filter { it.vigente }.groupingBy { it.colaboradorId }.eachCount()

    override fun inserir(alocacao: Alocacao): Long {
        if (buscarVigentePorPatrimonio(alocacao.numeroPatrimonio) != null) {
            throw ConflitoException("O ativo ${alocacao.numeroPatrimonio} já possui uma alocação vigente.")
        }
        val id = ++ultimoId
        dados[id] = alocacao.copy(id = id)
        return id
    }

    override fun atualizar(alocacao: Alocacao) {
        dados[alocacao.id] = alocacao
    }

    override fun excluir(id: Long) {
        dados.remove(id)
    }
}

val HOJE: LocalDate = LocalDate.of(2026, 11, 5)

val RELOGIO_FIXO: Clock = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))

fun umColaborador(
    id: Long = 1,
    nome: String = "Marina Alves",
    matricula: String = "0041287",
    setor: String = "Financeiro",
    situacao: Situacao = Situacao.ATIVO,
) = Colaborador(id = id, nome = nome, matricula = matricula, setor = setor, situacao = situacao)

fun umaAlocacao(
    id: Long = 1,
    numeroPatrimonio: String = "PAT-0001",
    colaborador: Colaborador = umColaborador(),
    dataRetirada: LocalDate = LocalDate.of(2026, 3, 14),
    dataDevolucao: LocalDate? = null,
) = Alocacao(
    id = id,
    numeroPatrimonio = numeroPatrimonio,
    equipamento = "Dell Latitude 5440",
    colaboradorId = colaborador.id,
    colaboradorNome = colaborador.nome,
    setor = colaborador.setor,
    dataRetirada = dataRetirada,
    dataDevolucao = dataDevolucao,
)

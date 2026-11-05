package br.pucpr.ativos.infrastructure.mongo

import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.FiltroDeAtivos
import br.pucpr.ativos.domain.ativo.StatusAtivo
import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson
import org.springframework.stereotype.Component

/** Adaptador da porta [AtivoRepository] para a coleção "ativos" do MongoDB. */
@Component
class MongoAtivoRepository(
    database: MongoDatabase,
    private val sequencia: SequenciaMongo,
) : AtivoRepository {

    private val colecao: MongoCollection<Document> = database.getCollection(COLECAO)

    override fun listar(filtro: FiltroDeAtivos): List<Ativo> {
        val condicoes = mutableListOf<Bson>()
        filtro.status?.let { condicoes.add(Filters.eq("status", it.rotulo)) }
        filtro.busca?.let { termo ->
            val padrao = escaparRegex(termo)
            condicoes.add(Filters.or(CAMPOS_DE_BUSCA.map { campo -> Filters.regex(campo, padrao, "i") }))
        }
        val consulta = if (condicoes.isEmpty()) Filters.empty() else Filters.and(condicoes)
        return colecao.find(consulta).sort(Sorts.ascending("id")).toList().map { paraAtivo(it) }
    }

    override fun buscarPorPatrimonio(numeroPatrimonio: String): Ativo? {
        val documento: Document? = colecao.find(Filters.eq("numeroPatrimonio", numeroPatrimonio)).first()
        return documento?.let { paraAtivo(it) }
    }

    override fun existePatrimonio(numeroPatrimonio: String): Boolean =
        colecao.countDocuments(Filters.eq("numeroPatrimonio", numeroPatrimonio)) > 0

    override fun proximoId(): Long = sequencia.proximo(COLECAO)

    override fun inserir(ativo: Ativo) {
        try {
            colecao.insertOne(paraDocumento(ativo))
        } catch (e: MongoWriteException) {
            if (e.error.category == ErrorCategory.DUPLICATE_KEY) {
                throw ConflitoException("Já existe um ativo com o patrimônio ${ativo.numeroPatrimonio}.")
            }
            throw e
        }
    }

    override fun atualizar(ativo: Ativo) {
        colecao.replaceOne(Filters.eq("numeroPatrimonio", ativo.numeroPatrimonio), paraDocumento(ativo))
    }

    override fun excluir(numeroPatrimonio: String) {
        colecao.deleteOne(Filters.eq("numeroPatrimonio", numeroPatrimonio))
    }

    /** Mesmo formato de documento que as Azure Functions já gravavam (compatível com os dados do Atlas). */
    private fun paraDocumento(ativo: Ativo): Document =
        Document()
            .append("id", ativo.id)
            .append("tipo", ativo.tipo)
            .append("marca", ativo.marca)
            .append("modelo", ativo.modelo)
            .append("numeroPatrimonio", ativo.numeroPatrimonio)
            .append("numeroSerie", ativo.numeroSerie)
            .append("status", ativo.status.rotulo)
            .append("colaborador", ativo.colaborador)
            .append("setor", ativo.setor)
            .append("dataAquisicao", ativo.dataAquisicao.toString())
            .append("valorAquisicao", ativo.valorAquisicao.toDouble())

    private fun paraAtivo(documento: Document): Ativo =
        Ativo(
            id = documento.inteiroLongo("id"),
            numeroPatrimonio = documento.texto("numeroPatrimonio"),
            tipo = documento.texto("tipo"),
            marca = documento.texto("marca"),
            modelo = documento.texto("modelo"),
            numeroSerie = documento.texto("numeroSerie"),
            status = StatusAtivo.doRotulo(documento.getString("status")) ?: StatusAtivo.EM_ESTOQUE,
            colaborador = documento.textoOuNulo("colaborador"),
            setor = documento.textoOuNulo("setor"),
            dataAquisicao = documento.data("dataAquisicao"),
            valorAquisicao = documento.decimal("valorAquisicao"),
        )

    /** O texto digitado na busca é tratado como literal, nunca como expressão regular. */
    private fun escaparRegex(texto: String): String =
        texto.replace(CARACTERES_ESPECIAIS) { "\\" + it.value }

    private companion object {
        const val COLECAO = "ativos"
        val CAMPOS_DE_BUSCA = listOf("numeroPatrimonio", "numeroSerie", "tipo", "marca", "modelo", "colaborador", "setor")
        val CARACTERES_ESPECIAIS = Regex("""[\\^$.|?*+()\[\]{}]""")
    }
}

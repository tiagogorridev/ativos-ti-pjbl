package br.pucpr.ativos.infrastructure.mongo

import br.pucpr.ativos.domain.manutencao.Manutencao
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.manutencao.TipoManutencao
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.stereotype.Component

/** Adaptador da porta [ManutencaoRepository] para a coleção "manutencoes" do MongoDB. */
@Component
class MongoManutencaoRepository(
    database: MongoDatabase,
    private val sequencia: SequenciaMongo,
) : ManutencaoRepository {

    private val colecao: MongoCollection<Document> = database.getCollection(COLECAO)

    override fun listar(numeroPatrimonio: String?): List<Manutencao> {
        val consulta = if (numeroPatrimonio == null) Filters.empty() else Filters.eq("numeroPatrimonio", numeroPatrimonio)
        return colecao.find(consulta)
            .sort(Sorts.orderBy(Sorts.descending("data"), Sorts.descending("id")))
            .toList()
            .map { paraManutencao(it) }
    }

    override fun buscarPorId(id: Long): Manutencao? {
        val documento: Document? = colecao.find(Filters.eq("id", id)).first()
        return documento?.let { paraManutencao(it) }
    }

    override fun existeParaAtivo(numeroPatrimonio: String): Boolean =
        colecao.countDocuments(Filters.eq("numeroPatrimonio", numeroPatrimonio)) > 0

    override fun proximoId(): Long = sequencia.proximo(COLECAO)

    override fun inserir(manutencao: Manutencao) {
        colecao.insertOne(paraDocumento(manutencao))
    }

    override fun atualizar(manutencao: Manutencao) {
        colecao.replaceOne(Filters.eq("id", manutencao.id), paraDocumento(manutencao))
    }

    override fun excluir(id: Long) {
        colecao.deleteOne(Filters.eq("id", id))
    }

    private fun paraDocumento(manutencao: Manutencao): Document =
        Document()
            .append("id", manutencao.id)
            .append("data", manutencao.data.toString())
            .append("numeroPatrimonio", manutencao.numeroPatrimonio)
            .append("equipamento", manutencao.equipamento)
            .append("tipo", manutencao.tipo.rotulo)
            .append("descricao", manutencao.descricao)
            .append("custo", manutencao.custo.toDouble())

    private fun paraManutencao(documento: Document): Manutencao =
        Manutencao(
            id = documento.inteiroLongo("id"),
            numeroPatrimonio = documento.texto("numeroPatrimonio"),
            equipamento = documento.texto("equipamento"),
            data = documento.data("data"),
            tipo = TipoManutencao.doRotulo(documento.getString("tipo")) ?: TipoManutencao.CORRETIVA,
            descricao = documento.texto("descricao"),
            custo = documento.decimal("custo"),
        )

    private companion object {
        const val COLECAO = "manutencoes"
    }
}

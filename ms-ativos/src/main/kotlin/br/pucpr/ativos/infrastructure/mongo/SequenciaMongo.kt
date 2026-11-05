package br.pucpr.ativos.infrastructure.mongo

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import org.bson.Document
import org.springframework.stereotype.Component

/**
 * Gera identificadores numéricos sequenciais por coleção (o frontend usa "id" numérico).
 * Usa a coleção "contadores" com incremento atômico. Na primeira vez, parte do maior id já gravado,
 * para continuar a numeração dos dados que já estavam no Atlas.
 */
@Component
class SequenciaMongo(private val database: MongoDatabase) {

    private val contadores: MongoCollection<Document> = database.getCollection(COLECAO_CONTADORES)

    fun proximo(nomeDaColecao: String): Long {
        garantirContador(nomeDaColecao)
        val atualizado: Document? = contadores.findOneAndUpdate(
            Filters.eq("_id", nomeDaColecao),
            Updates.inc("valor", 1L),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        val valor = atualizado?.get("valor") as? Number
            ?: throw IllegalStateException("Contador da coleção '$nomeDaColecao' indisponível.")
        return valor.toLong()
    }

    private fun garantirContador(nomeDaColecao: String) {
        val existente: Document? = contadores.find(Filters.eq("_id", nomeDaColecao)).first()
        if (existente != null) return

        val maior: Document? = database.getCollection(nomeDaColecao)
            .find()
            .sort(Sorts.descending("id"))
            .limit(1)
            .first()
        val inicial = (maior?.get("id") as? Number)?.toLong() ?: 0L
        try {
            contadores.insertOne(Document("_id", nomeDaColecao).append("valor", inicial))
        } catch (e: MongoWriteException) {
            // Outra instância criou o contador no mesmo instante: segue com o que já existe.
            if (e.error.category != ErrorCategory.DUPLICATE_KEY) throw e
        }
    }

    private companion object {
        const val COLECAO_CONTADORES = "contadores"
    }
}

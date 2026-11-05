package br.pucpr.ativos.infrastructure.mongo

import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/**
 * Leitura tolerante dos documentos: os dados do Atlas foram gravados por versões anteriores
 * (Azure Functions e script de seed), então números podem vir como Int, Double ou Decimal128
 * e datas como texto ou Date.
 */
internal fun Document.texto(campo: String): String = getString(campo).orEmpty()

internal fun Document.textoOuNulo(campo: String): String? = getString(campo)?.takeIf { it.isNotBlank() }

internal fun Document.inteiroLongo(campo: String): Long = (get(campo) as? Number)?.toLong() ?: 0L

internal fun Document.decimal(campo: String): BigDecimal =
    when (val valor = get(campo)) {
        is Decimal128 -> valor.bigDecimalValue()
        is Number -> BigDecimal(valor.toString())
        is String -> valor.toBigDecimalOrNull() ?: BigDecimal.ZERO
        else -> BigDecimal.ZERO
    }

internal fun Document.data(campo: String): LocalDate =
    when (val valor = get(campo)) {
        is Date -> valor.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
        is String -> runCatching { LocalDate.parse(valor.take(10)) }.getOrDefault(DATA_DESCONHECIDA)
        else -> DATA_DESCONHECIDA
    }

private val DATA_DESCONHECIDA: LocalDate = LocalDate.of(1970, 1, 1)

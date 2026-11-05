package br.pucpr.ativos.api.indicadores

import br.pucpr.ativos.application.indicadores.ConsultarIndicadores
import br.pucpr.ativos.application.indicadores.Indicadores
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/indicadores")
@Tag(name = "Indicadores", description = "Números consolidados do parque de ativos para o dashboard")
class IndicadorController(private val consultarIndicadores: ConsultarIndicadores) {

    @GetMapping
    @Operation(summary = "Totais por status e setor e custo de manutenção por mês")
    fun consultar(): Indicadores = consultarIndicadores.executar()
}

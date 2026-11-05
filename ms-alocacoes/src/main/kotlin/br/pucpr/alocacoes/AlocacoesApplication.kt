package br.pucpr.alocacoes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AlocacoesApplication

fun main(args: Array<String>) {
    runApplication<AlocacoesApplication>(*args)
}

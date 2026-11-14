package br.unisanta.desafio02.model

import com.google.firebase.Timestamp

data class Pedido(
    var id: String = "",
    val clienteId: String = "",
    val clienteNome: String = "",
    val clienteEmail: String = "",
    val itens: String = "",
    val valorTotal: Double = 0.0,
    val status: String = "Pendente",
    val dataCriacao: Timestamp = Timestamp.now(),
    val dataAtualizacao: Timestamp = Timestamp.now()
)

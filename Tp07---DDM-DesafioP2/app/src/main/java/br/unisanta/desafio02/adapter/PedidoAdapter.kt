package br.unisanta.desafio02.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.unisanta.desafio02.R
import br.unisanta.desafio02.model.Pedido
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PedidoAdapter(
    private val listaPedidos: List<Pedido>,
    private val isRestaurante: Boolean,
    private val onAvancarStatusClick: ((Pedido) -> Unit)? = null
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCliente: TextView = view.findViewById(R.id.txtClientePedido)
        val txtItens: TextView = view.findViewById(R.id.txtItensPedido)
        val txtValor: TextView = view.findViewById(R.id.txtValorPedido)
        val txtStatus: TextView = view.findViewById(R.id.txtStatusPedido)
        val txtDataCriacao: TextView = view.findViewById(R.id.txtDataCriacaoPedido)
        val txtDataAtualizacao: TextView = view.findViewById(R.id.txtDataAtualizacaoPedido)
        val btnAvancar: Button = view.findViewById(R.id.btnAvancarStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = listaPedidos[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        val identificacaoCliente = if (pedido.clienteNome.isNotBlank()) {
            "${pedido.clienteNome} (${pedido.clienteEmail})"
        } else {
            pedido.clienteEmail
        }

        holder.txtCliente.text = "Cliente: $identificacaoCliente"
        holder.txtItens.text = "Itens: ${pedido.itens}"
        holder.txtValor.text = "Total: ${moeda.format(pedido.valorTotal)}"
        holder.txtStatus.text = "Status: ${pedido.status}"
        holder.txtDataCriacao.text = "Pedido em: ${sdf.format(pedido.dataCriacao.toDate())}"
        holder.txtDataAtualizacao.text = "Última atualização: ${sdf.format(pedido.dataAtualizacao.toDate())}"

        val corStatus = when (pedido.status) {
            "Pendente" -> Color.parseColor("#D32F2F")
            "Em preparo" -> Color.parseColor("#F57C00")
            "Pronto para entrega" -> Color.parseColor("#1976D2")
            "Entregue" -> Color.parseColor("#388E3C")
            else -> Color.DKGRAY
        }
        holder.txtStatus.setTextColor(corStatus)

        if (isRestaurante && pedido.status != "Entregue") {
            val proximo = when (pedido.status) {
                "Pendente" -> "Em preparo"
                "Em preparo" -> "Pronto para entrega"
                "Pronto para entrega" -> "Entregue"
                else -> "Entregue"
            }
            holder.btnAvancar.visibility = View.VISIBLE
            holder.btnAvancar.text = "Mudar para: $proximo"
            holder.btnAvancar.setOnClickListener {
                onAvancarStatusClick?.invoke(pedido)
            }
        } else {
            holder.btnAvancar.visibility = View.GONE
        }
    }

    override fun getItemCount() = listaPedidos.size
}

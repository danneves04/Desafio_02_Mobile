package br.unisanta.desafio02

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.unisanta.desafio02.adapter.PedidoAdapter
import br.unisanta.desafio02.model.Pedido
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RestauranteActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val listaPedidos = mutableListOf<Pedido>()
    private lateinit var adapter: PedidoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        if (user == null) {
            voltarParaLogin()
            return
        }

        setContentView(R.layout.activity_restaurante)

        val nome = if (user.displayName.isNullOrBlank()) "Restaurante" else user.displayName
        findViewById<TextView>(R.id.txtNomeRestaurante).text = "Painel: $nome"
        findViewById<TextView>(R.id.txtEmailRestaurante).text = user.email

        findViewById<Button>(R.id.btnSairRestaurante).setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener { voltarParaLogin() }
        }

        val rvPedidos = findViewById<RecyclerView>(R.id.rvPedidosRestaurante)
        adapter = PedidoAdapter(listaPedidos, isRestaurante = true) { pedido ->
            atualizarStatusPedido(pedido)
        }
        rvPedidos.layoutManager = LinearLayoutManager(this)
        rvPedidos.adapter = adapter

        ouvirPedidosEmTempoReal()
    }

    private fun ouvirPedidosEmTempoReal() {
        db.collection("pedidos")
            .orderBy("dataCriacao", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                listaPedidos.clear()
                for (doc in snapshots.documents) {
                    val pedido = doc.toObject(Pedido::class.java)?.apply { id = doc.id }
                    if (pedido != null) {
                        listaPedidos.add(pedido)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun atualizarStatusPedido(pedido: Pedido) {
        val proximoStatus = when (pedido.status) {
            "Pendente" -> "Em preparo"
            "Em preparo" -> "Pronto para entrega"
            "Pronto para entrega" -> "Entregue"
            else -> "Entregue"
        }

        db.collection("pedidos").document(pedido.id)
            .update(
                mapOf(
                    "status" to proximoStatus,
                    "dataAtualizacao" to Timestamp.now()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Status atualizado para: $proximoStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun voltarParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

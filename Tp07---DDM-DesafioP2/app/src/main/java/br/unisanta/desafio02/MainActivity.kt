package br.unisanta.desafio02

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
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
import java.util.Calendar

class MainActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_main)

        val nome = if (user.displayName.isNullOrBlank()) "Cliente" else user.displayName
        findViewById<TextView>(R.id.txtBoasVindas).text = "Olá, $nome!"
        findViewById<TextView>(R.id.txtEmail).text = user.email

        findViewById<Button>(R.id.btnSair).setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener { voltarParaLogin() }
        }

        val cbHamburguer = findViewById<CheckBox>(R.id.cbHamburguer)
        val cbPizza = findViewById<CheckBox>(R.id.cbPizza)
        val cbCombo = findViewById<CheckBox>(R.id.cbCombo)
        val cbRefri = findViewById<CheckBox>(R.id.cbRefrigerante)
        val btnFazerPedido = findViewById<Button>(R.id.btnFazerPedido)
        val rvPedidos = findViewById<RecyclerView>(R.id.rvPedidosCliente)

        adapter = PedidoAdapter(listaPedidos, isRestaurante = false)
        rvPedidos.layoutManager = LinearLayoutManager(this)
        rvPedidos.adapter = adapter

        btnFazerPedido.setOnClickListener {
            val itensSelecionados = mutableListOf<String>()
            var total = 0.0

            if (cbHamburguer.isChecked) {
                itensSelecionados.add("X-Burger Artesanal")
                total += 25.0
            }
            if (cbPizza.isChecked) {
                itensSelecionados.add("Pizza Calabresa")
                total += 45.0
            }
            if (cbCombo.isChecked) {
                itensSelecionados.add("Combo Batata + Refri")
                total += 18.0
            }
            if (cbRefri.isChecked) {
                itensSelecionados.add("Refrigerante Lata")
                total += 6.0
            }

            if (itensSelecionados.isEmpty()) {
                Toast.makeText(this, "Selecione ao menos 1 item do menu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val agora = Timestamp.now()
            val novoPedido = hashMapOf(
                "clienteId" to user.uid,
                "clienteNome" to (user.displayName ?: "Cliente"),
                "clienteEmail" to (user.email ?: ""),
                "itens" to itensSelecionados.joinToString(", "),
                "valorTotal" to total,
                "status" to "Pendente",
                "dataCriacao" to agora,
                "dataAtualizacao" to agora
            )

            btnFazerPedido.isEnabled = false
            db.collection("pedidos").add(novoPedido)
                .addOnSuccessListener {
                    btnFazerPedido.isEnabled = true
                    Toast.makeText(this, "Pedido realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    cbHamburguer.isChecked = false
                    cbPizza.isChecked = false
                    cbCombo.isChecked = false
                    cbRefri.isChecked = false
                }
                .addOnFailureListener { e ->
                    btnFazerPedido.isEnabled = true
                    Toast.makeText(this, "Erro ao enviar pedido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        carregarHistorico6Meses(user.uid)
    }

    private fun carregarHistorico6Meses(uid: String) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        val seisMesesAtras = cal.time

        db.collection("pedidos")
            .whereEqualTo("clienteId", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val filtrados = snapshots.documents.mapNotNull { doc ->
                    doc.toObject(Pedido::class.java)?.apply { id = doc.id }
                }.filter { pedido ->
                    !pedido.dataCriacao.toDate().before(seisMesesAtras)
                }.sortedByDescending { it.dataCriacao }

                listaPedidos.clear()
                listaPedidos.addAll(filtrados)
                adapter.notifyDataSetChanged()
            }
    }

    private fun voltarParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

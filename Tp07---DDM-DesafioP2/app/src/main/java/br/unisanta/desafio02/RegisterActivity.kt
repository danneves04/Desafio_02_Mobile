package br.unisanta.desafio02

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val tilNome = findViewById<TextInputLayout>(R.id.tilNome)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilSenha = findViewById<TextInputLayout>(R.id.tilSenha)
        val tilConfirma = findViewById<TextInputLayout>(R.id.tilConfirma)
        val edtNome = findViewById<TextInputEditText>(R.id.edtNome)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtSenha = findViewById<TextInputEditText>(R.id.edtSenha)
        val edtConfirma = findViewById<TextInputEditText>(R.id.edtConfirma)
        val rbCliente = findViewById<RadioButton>(R.id.rbCliente)
        val btnCriar = findViewById<Button>(R.id.btnCriarConta)

        findViewById<Button>(R.id.btnVoltarLogin).setOnClickListener { finish() }

        btnCriar.setOnClickListener {
            val nome = edtNome.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString()
            val confirma = edtConfirma.text.toString()
            val tipo = if (rbCliente.isChecked) "Cliente" else "Restaurante"

            tilNome.error = null
            tilEmail.error = null
            tilSenha.error = null
            tilConfirma.error = null

            when {
                nome.isEmpty() -> tilNome.error = "Informe seu nome"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> tilEmail.error = "Informe um e-mail válido"
                senha.length < 6 -> tilSenha.error = "A senha deve ter ao menos 6 caracteres"
                senha != confirma -> tilConfirma.error = "As senhas não coincidem"
                else -> {
                    btnCriar.isEnabled = false
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnSuccessListener { resultado ->
                            val user = resultado.user ?: return@addOnSuccessListener
                            val perfil = UserProfileChangeRequest.Builder()
                                .setDisplayName(nome)
                                .build()

                            user.updateProfile(perfil).addOnCompleteListener {
                                val userMap = hashMapOf(
                                    "uid" to user.uid,
                                    "nome" to nome,
                                    "email" to email,
                                    "tipo" to tipo
                                )
                                db.collection("usuarios").document(user.uid).set(userMap)
                                    .addOnSuccessListener {
                                        irParaTelaInicial(tipo)
                                    }
                                    .addOnFailureListener { e ->
                                        btnCriar.isEnabled = true
                                        Toast.makeText(this, "Erro ao salvar perfil: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            btnCriar.isEnabled = true
                            Toast.makeText(this, "Erro ao cadastrar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }
    }

    private fun irParaTelaInicial(tipo: String) {
        Toast.makeText(this, "Conta criada como $tipo!", Toast.LENGTH_SHORT).show()
        val destino = if (tipo == "Restaurante") {
            RestauranteActivity::class.java
        } else {
            MainActivity::class.java
        }
        val intent = Intent(this, destino)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

package br.unisanta.desafio02

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilSenha: TextInputLayout
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtSenha: TextInputEditText
    private lateinit var btnEntrar: Button

    private val firebaseUiLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        onFirebaseUiResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null) {
            redirecionarPorPerfil(usuarioAtual.uid)
            return
        }

        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.tilEmail)
        tilSenha = findViewById(R.id.tilSenha)
        edtEmail = findViewById(R.id.edtEmail)
        edtSenha = findViewById(R.id.edtSenha)
        btnEntrar = findViewById(R.id.btnEntrar)

        btnEntrar.setOnClickListener { fazerLogin() }

        findViewById<Button>(R.id.btnCadastrar).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<Button>(R.id.btnEsqueciSenha).setOnClickListener { recuperarSenha() }

        findViewById<Button>(R.id.btnFirebaseUi).setOnClickListener { iniciarFirebaseUi() }
    }

    private fun fazerLogin() {
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString()

        tilEmail.error = null
        tilSenha.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Informe um e-mail válido"
            return
        }
        if (senha.isEmpty()) {
            tilSenha.error = "Informe a senha"
            return
        }

        btnEntrar.isEnabled = false
        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    redirecionarPorPerfil(uid)
                }
            }
            .addOnFailureListener { e ->
                btnEntrar.isEnabled = true
                Toast.makeText(this, "Falha no login: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun recuperarSenha() {
        val email = edtEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Informe seu e-mail para recuperar a senha"
            return
        }
        tilEmail.error = null
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "E-mail de recuperação enviado", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun iniciarFirebaseUi() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder()
                .setAllowNewAccounts(true)
                .setRequireName(true)
                .build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        firebaseUiLauncher.launch(signInIntent)
    }

    private fun onFirebaseUiResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            val user = auth.currentUser ?: return
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        val dados = hashMapOf(
                            "uid" to user.uid,
                            "nome" to (user.displayName ?: "Cliente"),
                            "email" to (user.email ?: ""),
                            "tipo" to "Cliente"
                        )
                        db.collection("usuarios").document(user.uid).set(dados)
                            .addOnCompleteListener { redirecionarPorPerfil(user.uid) }
                    } else {
                        redirecionarPorPerfil(user.uid)
                    }
                }
                .addOnFailureListener { redirecionarPorPerfil(user.uid) }
        } else {
            val erro = result.idpResponse?.error?.localizedMessage
            val msg = if (result.idpResponse == null) "Login cancelado" else "Erro ao autenticar: $erro"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun redirecionarPorPerfil(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val tipo = doc.getString("tipo") ?: "Cliente"
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
            .addOnFailureListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }
}
